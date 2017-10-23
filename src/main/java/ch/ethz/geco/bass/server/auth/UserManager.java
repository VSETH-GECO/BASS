package ch.ethz.geco.bass.server.auth;

import ch.ethz.geco.bass.util.ErrorHandler;
import ch.ethz.geco.bass.util.SQLite;
import org.java_websocket.WebSocket;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the handling of users. This includes authorization, account management, and session handling.
 */
public class UserManager {
    /**
     * The logger of this class
     */
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);

    /**
     * Holds a mapping of the currently open sessions to the user who opened the session.
     */
    private static final Map<String, User> validSessions = new HashMap<>();

    // Check database integrity
    static {
        try {
            if (!SQLite.tableExists("Users")) {
                logger.debug("User table does not exist, creating...");
                Connection con = SQLite.getConnection();
                PreparedStatement statement = con.prepareStatement("CREATE TABLE Users (ID INTEGER PRIMARY KEY AUTOINCREMENT, Name TEXT NOT NULL, Password TEXT NOT NULL)");
                statement.execute();
                logger.debug("User table created!");
            } else {
                logger.debug("User table already exists.");
            }
        } catch (SQLException e) {
            ErrorHandler.handleLocal(e);
        }
    }

    /**
     * Tries to login a user with the given credentials and return a valid session token to the given web socket on success.
     * If it fails, it will send appropriate error messages to the given web socket.
     *
     * @param ws   the web socket which tried to login
     * @param user the user name
     * @param pw   the password
     */
    public static void login(WebSocket ws, String user, String pw) {
        try {
            Connection con = SQLite.getConnection();

            // Get hashed password
            PreparedStatement statement = con.prepareStatement("SELECT * FROM Users WHERE Username = ?;");
            statement.setString(1, user);
            ResultSet result = statement.executeQuery();
            String hash = result.getString("Password");

            if (BCrypt.checkpw(pw, hash)) {
                // Logout old session if existing
                logout(user);

                String token = UUID.randomUUID().toString();
                validSessions.put(token, new User(("" + result.getInt("ID")), user));

                // TODO: Send session token to interface
            } else {
                // TODO: Send wrong password notification
            }
        } catch (SQLException e) {
            ErrorHandler.handleRemote(e, ws);
        }
    }

    /**
     * Tries to register a new user. Responds to the given web socket if the operation was successful.
     * It could fail because of duplicate user names or internal errors.
     *
     * @param ws   the web socket which wants to register a new user
     * @param user the user name
     * @param pw   the password
     */
    public static void register(WebSocket ws, String user, String pw) {
        try {
            Connection con = SQLite.getConnection();
            PreparedStatement queryStatement = con.prepareStatement("SELECT * FROM Users WHERE Username = ?;");
            queryStatement.setString(1, user);
            ResultSet result = queryStatement.executeQuery();

            // Check if there is already a user with that name
            if (!result.next()) {
                PreparedStatement insertStatement = con.prepareStatement("INSERT INTO Users VALUES (?, ?)");
                insertStatement.setString(1, user);
                insertStatement.setString(2, pw);
                insertStatement.executeUpdate();

                // TODO: Send registration successful notification
            } else {
                // TODO: Send name already taken notification
            }
        } catch (SQLException e) {
            ErrorHandler.handleRemote(e, ws);
        }
    }

    /**
     * Tries to delete the given user. Only works in combination with a valid session token for that user.
     * Responds to the given web socket if the operation was successful.
     *
     * @param ws     the web socket which wants to delete a user
     * @param userID the user ID of the user to delete
     * @param token  a valid session token for the given user
     */
    public static void delete(WebSocket ws, String userID, String token) {
        if (isValidSession(token, userID)) {
            try {
                Connection con = SQLite.getConnection();
                PreparedStatement deleteStatement = con.prepareStatement("DELETE * FROM Users WHERE ID = ?;");
                deleteStatement.setString(1, userID);
                deleteStatement.executeUpdate();

                // TODO: Send account successfully deleted notification
            } catch (SQLException e) {
                ErrorHandler.handleRemote(e, ws);
            }
        } else {
            // TODO: Send unauthorized notification
        }
    }

    /**
     * Performs a logout on the given user by removing it's session from the list of valid sessions.
     *
     * @param user the user to logout
     * @return true if the logout was successful, false if the given user was not logged in
     */
    private static boolean logout(String user) {
        boolean wasLoggedIn = false;
        for (Map.Entry<String, User> curEntry : validSessions.entrySet()) {
            if (curEntry.getValue().getName().equals(user)) {
                validSessions.remove(curEntry.getKey());
                wasLoggedIn = true;
            }
        }

        return wasLoggedIn;
    }

    /**
     * Returns whether or not the given session token is valid in combination with the given user.
     *
     * @param token  the session token to check
     * @param userID the ID of the user
     * @return if the token is valid
     */
    public static boolean isValidSession(String token, String userID) {
        User user = validSessions.get(token);
        return user != null && user.getUserID().equals(userID);
    }
}
