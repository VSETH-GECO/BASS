package ch.ethz.geco.bass.server.auth;

import ch.ethz.geco.bass.server.AuthWebSocket;
import ch.ethz.geco.bass.server.RequestSender;
import ch.ethz.geco.bass.util.ErrorHandler;
import ch.ethz.geco.bass.util.SQLite;
import com.google.gson.JsonObject;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
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
            Connection con = SQLite.getConnection();

            if (!SQLite.tableExists("Users")) {
                logger.debug("User table does not exist, creating...");
                PreparedStatement statement = con.prepareStatement("CREATE TABLE Users (ID INTEGER PRIMARY KEY AUTOINCREMENT, Name TEXT NOT NULL, Password TEXT NOT NULL);");
                statement.execute();
                logger.debug("User table created!");

                // TODO: Remove static user in prod
                UserManager.register(null, "admin", "password");
            } else {
                logger.debug("User table already exists.");
            }

            if (!SQLite.tableExists("Sessions")) {
                logger.debug("Sessions table does not exist, creating...");
                PreparedStatement statement = con.prepareStatement("CREATE TABLE Sessions (UserID INTEGER NOT NULL, Token TEXT NOT NULL, Valid DATETIME NOT NULL, FOREIGN KEY(UserID) REFERENCES Users(ID));");
                statement.execute();
                logger.debug("Sessions table created!");
            } else {
                logger.debug("Sessions table already exists.");
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
            Connection con = SQLite.getConnection();

            // Get hashed password
            PreparedStatement queryStatement = con.prepareStatement("SELECT * FROM Users WHERE Name = ?;");
            queryStatement.setString(1, userName);
            ResultSet queryResult = queryStatement.executeQuery();

            // Check if we found any rows
            if (queryResult.next()) {
                String hash = queryResult.getString("Password");
                if (BCrypt.checkpw(password, hash)) {
                    String token = UUID.randomUUID().toString();
                    Integer userID = queryResult.getInt("ID");

                    // Add a new session token to the database which is valid for one day
                    PreparedStatement insertStatement = con.prepareStatement("INSERT INTO Sessions VALUES (?,?, datetime('now', '+1 day'));");
                    insertStatement.setInt(1, userID);
                    insertStatement.setString(2, token);
                    insertStatement.executeUpdate();

                    User user = new User(userID, userName);
                    webSocket.setAuthorizedUser(user);

                    // TODO: Send session token to interface
                } else {
                    // Wrong password
                    JsonObject data = new JsonObject();
                    data.addProperty("message", "Wrong password.");
                    RequestSender.sendError(webSocket, data);
                }
            } else {
                // Account not found
                JsonObject data = new JsonObject();
                data.addProperty("message", "Account not found.");
                RequestSender.sendError(webSocket, data);
            }
        } catch (SQLException e) {
            ErrorHandler.handleLocal(e);
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
            Connection con = SQLite.getConnection();
            PreparedStatement sessionQuery = con.prepareStatement("SELECT UserID FROM Sessions WHERE Token = ?;");
            sessionQuery.setString(1, token);
            ResultSet sessionResult = sessionQuery.executeQuery();

            if (sessionResult.next()) {
                int userID = sessionResult.getInt("UserID");
                PreparedStatement userQuery = con.prepareStatement("SELECT Name FROM Users WHERE ID = ?;");
                userQuery.setInt(1, userID);
                ResultSet userResult = userQuery.executeQuery();

                if (userResult.next()) {
                    String userName = userResult.getString("Name");
                    User user = new User(userID, userName);
                    webSocket.setAuthorizedUser(user);

                    // TODO: Resend session token to interface?
                } else {
                    ErrorHandler.handleLocal(new IllegalStateException("Found session associated to non-existing user."));
                }
            } else {
                JsonObject data = new JsonObject();
                data.addProperty("message", "Invalid session.");
                RequestSender.sendError(webSocket, data);
            }
        } catch (SQLException e) {
            ErrorHandler.handleLocal(e);
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
            Connection con = SQLite.getConnection();
            PreparedStatement queryStatement = con.prepareStatement("SELECT * FROM Users WHERE Name = ?;");
            queryStatement.setString(1, userName);
            ResultSet result = queryStatement.executeQuery();

            // Check if there is already a user with that name
            if (!result.next()) {
                PreparedStatement insertStatement = con.prepareStatement("INSERT INTO Users (Name, Password) VALUES (?, ?);");
                insertStatement.setString(1, userName);
                insertStatement.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
                insertStatement.executeUpdate();

                if (webSocket != null) {
                    // TODO: Send registration successful notification
                }
            } else {
                // Name already taken
                if (webSocket != null) {
                    JsonObject data = new JsonObject();
                    data.addProperty("message", "Name already taken.");
                    RequestSender.sendError(webSocket, data);
                }
            }
        } catch (SQLException e) {
            ErrorHandler.handleLocal(e);
        }
    }

    /**
     * Tries to delete the given user. Responds to the given web socket if the operation was successful.
     *
     * @param webSocket the web socket which wants to delete a user
     * @param userID    the user ID of the user to delete
     */
    public static void delete(AuthWebSocket webSocket, String userID) {
        try {
            Connection con = SQLite.getConnection();
            PreparedStatement deleteStatement = con.prepareStatement("DELETE FROM Users WHERE ID = ?;");
            deleteStatement.setString(1, userID);
            deleteStatement.executeUpdate();

            // TODO: Send account successfully deleted notification
        } catch (SQLException e) {
            ErrorHandler.handleLocal(e);
        }
    }

    /**
     * Deletes a specific session token from the database.
     *
     * @param token the token to delete
     */
    private static void deleteSessionToken(String token) {
        try {
            Connection con = SQLite.getConnection();
            PreparedStatement deleteStatement = con.prepareStatement("DELETE FROM Sessions WHERE Token = ?;");
            deleteStatement.setString(1, token);
            deleteStatement.executeUpdate();
        } catch (SQLException e) {
            ErrorHandler.handleLocal(e);
        }
    }

    /**
     * Deletes old session tokens in the database.
     */
    private static void deleteExpiredSessions() {
        try {
            Connection con = SQLite.getConnection();
            PreparedStatement deleteStatement = con.prepareStatement("DELETE FROM Sessions WHERE Valid < datetime('now');");
            int removedSessions = deleteStatement.executeUpdate();

            if (removedSessions > 0) {
                logger.info("Removed " + removedSessions + " expired Sessions.");
            }
        } catch (SQLException e) {
            ErrorHandler.handleLocal(e);
        }
    }
}
