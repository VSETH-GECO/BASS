package ch.ethz.geco.bass.server.auth;

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
            e.printStackTrace();
        }
    }

    /**
     * Tries to login a user with the give credentials and return a valid session token on success. If it fails, this will return null.
     *
     * @param ws   The web socket which tried to login
     * @param user the user name
     * @param pw   the password
     * @return a valid session token success or null otherwise
     */
    public static void login(WebSocket ws, String user, String pw) {
        if (!isLoggedIn(user)) {
            try {
                Connection con = SQLite.getConnection();

                // Get hashed password
                PreparedStatement statement = con.prepareStatement("SELECT * FROM Users WHERE Username = ?");
                statement.setString(1, user);
                ResultSet result = statement.executeQuery();
                String hash = result.getString("Password");

                if (BCrypt.checkpw(pw, hash)) {
                    String token = UUID.randomUUID().toString();
                    validSessions.put(token, new User(("" + result.getInt("ID")), user));

                    // TODO: Send session token to interface
                } else {
                    // TODO: Send wrong password notification
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            // TODO: Send already logged in notification
        }
    }

    /**
     * Tries to register a new user. Returns true if the registration was successful, false otherwise.
     * It could fail because of duplicate user names or internal errors.
     *
     * @param ws   The web socket which tried to register a new user
     * @param user The user name
     * @param pw   The password
     * @return true on success, false otherwise
     */
    public static boolean register(WebSocket ws, String user, String pw) {
        try {
            Connection con = SQLite.getConnection();
            PreparedStatement statement = con.prepareStatement("SELECT * FROM Users WHERE Username = ?");
            statement.setString(1, user);
            ResultSet result = statement.executeQuery();

            // Check if we found a row
            if (!result.next()) {

            } else {
                // TODO: Send name already taken notification
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Checks if the given user is currently logged in.
     *
     * @param user the user to check
     * @return if the given user is currently logged in
     */
    private static boolean isLoggedIn(String user) {
        boolean isLoggedIn = false;
        for (User activeUser : validSessions.values()) {
            if (activeUser.getName().equals(user)) {
                isLoggedIn = true;
            }
        }

        return isLoggedIn;
    }

    /**
     * Returns whether or not the given session token is valid in combination with the given user.
     *
     * @param token  the session token to check
     * @param userID the ID of the user
     * @return if the token is valid
     */
    public static boolean isValidSession(String token, String userID) {
        return validSessions.get(token).getUserID().equals(userID);
    }
}
