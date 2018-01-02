package ch.ethz.geco.bass.server.auth;

import ch.ethz.geco.bass.server.AuthWebSocket;
import ch.ethz.geco.bass.server.Server.Action;
import ch.ethz.geco.bass.server.Server.Resource;
import ch.ethz.geco.bass.server.VoteHandler;
import ch.ethz.geco.bass.server.util.FavoriteTrack;
import ch.ethz.geco.bass.server.util.RequestSender;
import ch.ethz.geco.bass.server.util.WsPackage;
import ch.ethz.geco.bass.util.ErrorHandler;
import ch.ethz.geco.bass.util.SQLite;
import com.google.gson.JsonObject;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Manages the handling of users. This includes authorization, account management, and session handling.
 */
public class UserManager {
    /**
     * The logger of this class
     */
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);

    // Check database integrity
    static {
        try {
            if (!SQLite.tableExists("Users")) {
                logger.debug("User table does not exist, creating...");
                PreparedStatement statement = SQLite.getPreparedStatement(SQLite.Statements.CREATE_TABLE_USERS);
                statement.execute();
                logger.debug("User table created!");

                // TODO: Remove static user in prod
                UserManager.register(null, "admin", "password");
                UserManager.setAdmin(null, 1, true);
            } else {
                logger.debug("User table already exists.");
            }

            if (!SQLite.tableExists("Sessions")) {
                logger.debug("Sessions table does not exist, creating...");
                PreparedStatement statement = SQLite.getPreparedStatement(SQLite.Statements.CREATE_TABLE_SESSIONS);
                statement.execute();
                logger.debug("Sessions table created!");
            } else {
                logger.debug("Sessions table already exists.");
            }

            if (!SQLite.tableExists("Favorites")) {
                logger.debug("Favorites table does not exist, creating...");
                PreparedStatement statement = SQLite.getPreparedStatement(SQLite.Statements.CREATE_TABLE_FAVORITES);
                statement.execute();
                logger.debug("Favorites table created!");
            } else {
                logger.debug("Favorites table already exists.");
            }
        } catch (SQLException e) {
            ErrorHandler.handleLocal(e);
        }

        // Periodically remove old session tokens every minute
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                deleteExpiredSessions();
            }
        }, 60000, 60000);

        logger.info("Started periodic session token cleanup!");
    }

    /**
     * Tries to login a user with the given credentials and return a valid session token to the given web socket on success.
     * If it fails, it will send appropriate error messages to the given web socket. This function will generate a new session token,
     * which is valid for one day, for each call. Like this, you can connect multiple times with the same account.
     *
     * @param webSocket the web socket which wants to login
     * @param userName  the user name
     * @param password  the password
     */
    public static void login(AuthWebSocket webSocket, String userName, String password) {
        try {
            // Get hashed password
            PreparedStatement queryStatement = SQLite.getPreparedStatement(SQLite.Statements.GET_USER_BY_NAME);
            queryStatement.setString(1, userName);
            ResultSet queryResult = queryStatement.executeQuery();

            // Check if we found any rows
            if (queryResult.next()) {
                String hash = queryResult.getString("Password");
                if (BCrypt.checkpw(password, hash)) {
                    String token = UUID.randomUUID().toString();
                    Integer userID = queryResult.getInt("ID");
                    boolean isAdmin = queryResult.getBoolean("Admin");

                    // Add a new session token to the database which is valid for one day
                    //  PreparedStatement insertStatement = con.prepareStatement("INSERT INTO Sessions VALUES (?,?, datetime('now', '+1 day'));");
                    PreparedStatement insertStatement = SQLite.getPreparedStatement(SQLite.Statements.INSERT_NEW_SESSION);
                    insertStatement.setInt(1, userID);
                    insertStatement.setString(2, token);
                    insertStatement.executeUpdate();

                    User user = new User(userID, userName, isAdmin);
                    webSocket.setAuthorizedUser(user);

                    // Remove vote expiry
                    VoteHandler.removeExpiry(userID);

                    RequestSender.sendUserInfo(webSocket, token, userName, userID, isAdmin);
                } else {
                    // Wrong password
                    JsonObject data = new JsonObject();
                    data.addProperty("message", "Wrong password.");
                    RequestSender.sendError(webSocket, Resource.USER, data);
                }
            } else {
                // Account not found
                JsonObject data = new JsonObject();
                data.addProperty("message", "Account not found.");
                RequestSender.sendError(webSocket, Resource.USER, data);
            }
        } catch (SQLException e) {
            RequestSender.handleInternalError(webSocket, e);
        }
    }

    /**
     * Tries to login a user with the given session token. This can be used to resume old sessions.
     *
     * @param webSocket the web socket which wants to login
     * @param token     the session token
     */
    public static void login(AuthWebSocket webSocket, String token) {
        try {
            PreparedStatement sessionQuery = SQLite.getPreparedStatement(SQLite.Statements.GET_USER_ID_BY_SESSION_TOKEN);
            sessionQuery.setString(1, token);
            ResultSet sessionResult = sessionQuery.executeQuery();

            if (sessionResult.next()) {
                int userID = sessionResult.getInt("UserID");
                PreparedStatement userQuery = SQLite.getPreparedStatement(SQLite.Statements.GET_USER_BY_ID);
                userQuery.setInt(1, userID);
                ResultSet userResult = userQuery.executeQuery();

                if (userResult.next()) {
                    String userName = userResult.getString("Name");
                    boolean isAdmin = userResult.getInt("Admin") == 1; // This is dumb, I know
                    User user = new User(userID, userName, isAdmin);
                    webSocket.setAuthorizedUser(user);

                    refreshToken(token);

                    // Remove vote expiry
                    VoteHandler.removeExpiry(userID);

                    RequestSender.sendUserInfo(webSocket, token, userName, userID, isAdmin);
                } else {
                    ErrorHandler.handleLocal(new IllegalStateException("Found session associated to non-existing user."));
                }
            } else {
                JsonObject data = new JsonObject();
                data.addProperty("message", "Invalid session.");
                RequestSender.sendError(webSocket, Resource.USER, data);
            }
        } catch (SQLException e) {
            RequestSender.handleInternalError(webSocket, e);
        }
    }

    /**
     * Tries to log out the given web socket.
     *
     * @param webSocket the web socket which wants to log out
     * @param token that should be deleted
     */
    public static void logout(AuthWebSocket webSocket, String token) {
        try {
            deleteSessionToken(token);
            webSocket.logout();

            WsPackage.create(Resource.USER, Action.LOGOUT).send(webSocket);
        } catch (SQLException e) {
            RequestSender.handleInternalError(webSocket, e);
        }
    }

    /**
     * Tries to register a new user. Responds to the given web socket if the operation was successful.
     * It could fail because of duplicate user names or internal errors.
     *
     * @param webSocket the web socket which wants to register a new user
     * @param userName  the user name
     * @param password  the password
     */
    public static void register(AuthWebSocket webSocket, String userName, String password) {
        try {
            PreparedStatement queryStatement = SQLite.getPreparedStatement(SQLite.Statements.GET_USER_BY_NAME);
            // PreparedStatement queryStatement = con.prepareStatement("SELECT * FROM Users WHERE Name = ?;");
            queryStatement.setString(1, userName);
            ResultSet result = queryStatement.executeQuery();

            // Check if there is already a user with that name
            if (!result.next()) {
                PreparedStatement insertStatement = SQLite.getPreparedStatement(SQLite.Statements.INSERT_NEW_USER);
                insertStatement.setString(1, userName);
                insertStatement.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
                insertStatement.executeUpdate();

                if (webSocket != null) {
                    WsPackage.create(Resource.USER, Action.SUCCESS)
                            .addData("action", Action.ADD.toString())
                            .addData("message", "User created").send(webSocket);
                }
            } else {
                // Name already taken
                if (webSocket != null) {
                    JsonObject data = new JsonObject();
                    data.addProperty("message", "Name already taken.");
                    RequestSender.sendError(webSocket, Resource.USER, data);
                }
            }
        } catch (SQLException e) {
            RequestSender.handleInternalError(webSocket, e);
        }
    }

    /**
     * Tries to update a users admin property. Responds to the given web socket if the operation was successful.
     * It could fail because the userName provided does not match any existing user.
     * It currently requires a new login for the changes to take action.
     *
     * @param webSocket the web socket that wants to update a user
     * @param userID of the user to be update
     * @param isAdmin value that should be set
     * @return 1 if the change was successful, 0 if that user wasn't found and -1 on database errors
     */
    public static int setAdmin(AuthWebSocket webSocket, int userID, boolean isAdmin) {
        // TODO add direct update of the connection with the updated user if one exists.
        try {
            PreparedStatement updateStatement = SQLite.getPreparedStatement(SQLite.Statements.UPDATE_ADMIN_BY_ID);
            updateStatement.setInt(1, isAdmin ? 1 : 0); // Because SQLite does not support booleans apparently
            updateStatement.setInt(2, userID);
            return updateStatement.executeUpdate() > 0 ? 1 : 0;

        } catch (SQLException e) {
            RequestSender.handleInternalError(webSocket, e);
        }

        return -1;
    }

    /**
     * Fetches a list of currently registered users
     *
     * @return a list of users with their relevant information
     */
    public static List<User> getUsers() {
        try {
            List<User> users = new ArrayList<>();

            PreparedStatement queryStatement = SQLite.getPreparedStatement(SQLite.Statements.GET_ALL_USERS);
            ResultSet queryResult = queryStatement.executeQuery();

            while (queryResult.next()) {
                User user = new User(queryResult.getInt("ID"),
                        queryResult.getString("Name"),
                        queryResult.getBoolean("Admin"));

                users.add(user);
            }

            return users;
        } catch (SQLException e) {
            ErrorHandler.handleLocal(e);
        }

        return null;
    }

    /**
     * Tries to delete the given user. Responds to the given web socket if the operation was successful.
     *
     * @param webSocket the web socket which wants to delete a user
     * @param userID    the user ID of the user to delete
     */
    public static void delete(AuthWebSocket webSocket, int userID) {
        try {
            PreparedStatement userDelete = SQLite.getPreparedStatement(SQLite.Statements.DELETE_USER_BY_ID);
            userDelete.setInt(1, userID);
            userDelete.executeUpdate();

            PreparedStatement sessionDelete = SQLite.getPreparedStatement(SQLite.Statements.DELETE_SESSION_BY_ID);
            sessionDelete.setInt(1, userID);
            sessionDelete.executeUpdate();

            PreparedStatement favoritesDelete = SQLite.getPreparedStatement(SQLite.Statements.DELETE_FAVORITES_BY_USER_ID);
            favoritesDelete.setInt(1, userID);
            favoritesDelete.executeUpdate();

            WsPackage.create(Resource.USER, Action.SUCCESS)
                    .addData("action", Action.DELETE.toString())
                    .addData("message", "User deleted").send(webSocket);
        } catch (SQLException e) {
            RequestSender.handleInternalError(webSocket, e);
        }
    }

    /**
     * Sets the username for a user
     *
     * @param webSocket to respond to
     * @param userID    of the user
     * @param name      to be set
     * @return 1 on success, 0 on user not found and -1 on database error
     */
    public static int setUsername(AuthWebSocket webSocket, int userID, String name) {
        // TODO add direct update of the connection with the updated user if one exists.
        try {
            PreparedStatement updateStatement = SQLite.getPreparedStatement(SQLite.Statements.UPDATE_USER_NAME_BY_ID);
            updateStatement.setString(1, name);
            updateStatement.setInt(2, userID);
            return updateStatement.executeUpdate() > 0 ? 1 : 0;
        } catch (SQLException e) {
            RequestSender.handleInternalError(webSocket, e);
        }

        return -1;
    }

    /**
     * Set the password for a user
     *
     * @param webSocket to respond to
     * @param userID of the user
     * @param password to be set
     * @return 1 on success, 0 on user not found and -1 on database error
     */
    public static int setPassword(AuthWebSocket webSocket, int userID, String password) {
        // TODO add direct update of the connection with the updated user if one exists.
        try {
            PreparedStatement updateStatement = SQLite.getPreparedStatement(SQLite.Statements.UPDATE_PASSWORD_BY_ID);
            updateStatement.setString(1, BCrypt.hashpw(password, BCrypt.gensalt()));
            updateStatement.setInt(2, userID);
            return updateStatement.executeUpdate() > 0 ? 1 : 0;
        } catch (SQLException e) {
            RequestSender.handleInternalError(webSocket, e);
        }

        return -1;
    }

    /**
     * Deletes a specific session token from the database.
     *
     * @param token the token to delete
     */
    private static void deleteSessionToken(String token) throws SQLException {
        PreparedStatement deleteStatement = SQLite.getPreparedStatement(SQLite.Statements.DELETE_SESSION_BY_TOKEN);
        deleteStatement.setString(1, token);
        deleteStatement.executeUpdate();
    }

    /**
     * Refreshes the validity of the given token.
     *
     * @param token the token to refresh
     * @throws SQLException on sql exception
     */
    private static void refreshToken(String token) throws SQLException {
        PreparedStatement refreshStatement = SQLite.getPreparedStatement(SQLite.Statements.UPDATE_SESSION_VALIDITY_BY_TOKEN);
        refreshStatement.setString(1, token);
        refreshStatement.executeUpdate();
    }

    /**
     * Deletes old session tokens in the database.
     */
    private static void deleteExpiredSessions() {
        try {
            PreparedStatement deleteStatement = SQLite.getPreparedStatement(SQLite.Statements.DELETE_SESSION_BY_VALIDITY);
            int removedSessions = deleteStatement.executeUpdate();

            if (removedSessions > 0) {
                logger.info("Removed " + removedSessions + " expired Sessions.");
            }
        } catch (SQLException e) {
            ErrorHandler.handleLocal(e);
        }
    }

    /**
     * Retrieves a list of the favorite tracks for a user
     *
     * @param userID of the user
     * @return the list of favorites
     */
    public static List<FavoriteTrack> getFavorites(int userID) {
        ArrayList <FavoriteTrack> favorites = new ArrayList<>();

        try {
            PreparedStatement queryStatement = SQLite.getPreparedStatement(SQLite.Statements.GET_FAVORITES_BY_USER_ID);
            queryStatement.setInt(1, userID);
            ResultSet results = queryStatement.executeQuery();

            while (results.next()) {
                favorites.add(new FavoriteTrack(results.getString("Uri"), results.getString("Title")));
            }
        } catch (SQLException e) {
            ErrorHandler.handleLocal(e);
        }

        return favorites;
    }

    /**
     * Adds a track to a users favorites
     *
     * @param userID of the user
     * @param uri of the track to be added
     * @param title of the track to be added
     */
    public static void addFavorite(int userID, String uri, String title) {
        try {
            if (hasFavorite(userID, uri)) {
                return;
            }

            PreparedStatement insertStatement = SQLite.getPreparedStatement(SQLite.Statements.INSERT_NEW_FAVORITE);
            insertStatement.setInt(1, userID);
            insertStatement.setString(2, uri);
            insertStatement.setString(3, title);

            insertStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes a track for a users favorites
     *
     * @param userID of the user
     * @param uri of the track to be removed
     * @return true if a track was deleted
     */
    public static boolean removeFavorite(int userID, String uri) {
        try {
            PreparedStatement deleteStatement = SQLite.getPreparedStatement(SQLite.Statements.DELETE_FAVORITE_BY_USER_ID_AND_URI);
            deleteStatement.setInt(1, userID);
            deleteStatement.setString(2, uri);

            int removedFavorites = deleteStatement.executeUpdate();

            return removedFavorites > 0;
        } catch (SQLException e) {
            ErrorHandler.handleLocal(e);
        }

        return false;
    }

    /**
     * Checks whether a user has a track in his favorites
     *
     * @param userID of the user
     * @param uri of the track
     * @return true if the track is in the users favorites
     */
    private static boolean hasFavorite(int userID, String uri) {
        try {
            PreparedStatement queryStatement = SQLite.getPreparedStatement(SQLite.Statements.GET_FAVORITE_BY_USER_ID_AND_URI);
            queryStatement.setInt(1, userID);
            queryStatement.setString(2, uri);

            ResultSet results = queryStatement.executeQuery();

            return results.next();
        } catch (SQLException e) {
            ErrorHandler.handleLocal(e);
        }

        return false;
    }
}
