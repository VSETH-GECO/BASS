package ch.ethz.geco.bass.server.auth;

import ch.ethz.geco.bass.util.SQLite;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserManager {
    /**
     * Holds a mapping of the currently open sessions to the user who opened the session.
     */
    private static Map<String, User> currentSessions = new HashMap<>();

    /**
     * Tries to login a user with the give credentials and return a valid session token on success. If it fails, this will return null.
     * @param user the user name
     * @param pw the password
     * @return a valid session token success or null otherwise
     */
    public static void login(String user, String pw) {
        boolean alreadyLoggedIn = false;
        for (User activeUser : currentSessions.values()) {
            if (activeUser.getName().equals(user)) {
                alreadyLoggedIn = true;
            }
        }

        if (!alreadyLoggedIn) {
            try {
                Connection con = SQLite.getConnection();

                // Get hashed password
                PreparedStatement statement = con.prepareStatement("SELECT * FROM Users WHERE Username = ?");
                statement.setString(1, user);
                ResultSet result = statement.executeQuery();
                String hash = result.getString("Password");

                if (BCrypt.checkpw(pw, hash)) {
                    String token = UUID.randomUUID().toString();
                    // Success!
                    // TODO: Add user to session map and send token to interface
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
     * Tries to register a new user
     * @param user
     * @param pw
     * @return
     */
    public static String register(String user, String pw) {
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
    }

    /**
     * Returns whether or not the given session token is valid.
     * @param token the session token to check
     * @return if the token is valid
     */
    public static boolean isValidSession(String token) {
        return currentSessions.containsKey(token);
    }
}
