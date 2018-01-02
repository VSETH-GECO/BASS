package ch.ethz.geco.bass.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * A small helper to make database communication easier.
 */
public class SQLite {
    public enum Statements {
        GET_ALL_USERS("SELECT * FROM Users;"),
        GET_USER_BY_ID("SELECT * FROM Users WHERE Id = ?;"),
        GET_USER_BY_NAME("SELECT * FROM Users WHERE Name = ?;"),
        GET_PLAYER_BY_ID("SELECT * FROM Players WHERE ID = ?;"),
        GET_FAVORITES_BY_USER_ID("SELECT * FROM Favorites WHERE UserID = ?"),
        GET_USER_ID_BY_SESSION_TOKEN("SELECT UserID FROM Sessions WHERE Token = ?;"),
        GET_FAVORITE_BY_USER_ID_AND_URI("SELECT UserID FROM Favorites WHERE UserID = ? AND Uri = ?"),

        UPDATE_ADMIN_BY_ID("UPDATE Users SET Admin = ? WHERE ID = ?"),
        UPDATE_PASSWORD_BY_ID("UPDATE Users SET Password = ? WHERE ID = ?"),
        UPDATE_USER_NAME_BY_ID("UPDATE Users SET Name = ? WHERE ID = ?"),
        UPDATE_SESSION_VALIDITY_BY_TOKEN("UPDATE Sessions SET Valid = datetime('now', '+1 day') WHERE Token = ?;"),

        INSERT_NEW_USER("INSERT INTO Users (Name, Password, Admin) VALUES (?, ?, 0);"),
        INSERT_NEW_SESSION("INSERT INTO Sessions VALUES (?,?, datetime('now', '+1 day'));"),
        INSERT_NEW_FAVORITE("INSERT INTO Favorites VALUES (?, ?, ?)"),
        INSERT_OR_REPLACE_PLAYER("INSERT OR REPLACE INTO Players VALUES (?,?,?,?,?);"),

        DELETE_USER_BY_ID("DELETE FROM Users WHERE ID = ?;"),
        DELETE_SESSION_BY_ID("DELETE FROM Sessions WHERE UserID = ?;"),
        DELETE_SESSION_BY_TOKEN("DELETE FROM Sessions WHERE Token = ?;"),
        DELETE_SESSION_BY_VALIDITY("DELETE FROM Sessions WHERE Valid < datetime('now');"),
        DELETE_FAVORITES_BY_USER_ID("DELETE FROM Favorites WHERE UserID = ?"),
        DELETE_FAVORITE_BY_USER_ID_AND_URI("DELETE FROM Favorites WHERE UserID = ? AND Uri = ?"),

        CREATE_TABLE_USERS("CREATE TABLE Users (ID INTEGER PRIMARY KEY AUTOINCREMENT, Name TEXT NOT NULL, Password TEXT NOT NULL, Admin INTEGER NOT NULL);"),
        CREATE_TABLE_PLAYER("CREATE TABLE Players (ID INTEGER PRIMARY KEY, Paused INTEGER NOT NULL, TrackCount INTEGER NOT NULL, Track TEXT, Playlist TEXT);"),
        CREATE_TABLE_SESSIONS("CREATE TABLE Sessions (UserID INTEGER NOT NULL, Token TEXT NOT NULL, Valid DATETIME NOT NULL, FOREIGN KEY(UserID) REFERENCES Users(ID));"),
        CREATE_TABLE_FAVORITES("CREATE TABLE Favorites (UserID INTEGER NOT NULL, Uri TEXT NOT NULL, Title Text NOT NULL, FOREIGN KEY(UserID) REFERENCES Users(ID));"),;

        private final String statement;

        Statements(String statement) {
            this.statement = statement;
        }

        String getStatement() {
            return this.statement;
        }
    }

    /**
     * Returns a prepared statement to interact with the database.
     *
     * @param statement the statement that is needed
     * @return the prepared statement ready for use
     * @throws SQLException in case something went wrong
     */
    public static PreparedStatement getPreparedStatement(Statements statement) throws SQLException {
        Connection connection = getConnection();
        return connection.prepareStatement(statement.getStatement());
    }

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(SQLite.class);

    /**
     * The static connection to the database.
     */
    private static Connection connection = null;

    /**
     * The path to the database.
     */
    private static final String DB_PATH = "jdbc:sqlite:data/bass.db";

    /**
     * Returns either a valid database connection or null if something went wrong while connecting to the database.
     *
     * @return either a valid database connection or null on error
     * @throws SQLException on database access error
     */
    private static Connection getConnection() throws SQLException {
        if (connection == null || !connection.isValid(0)) {
            logger.debug("Connection is no longer valid, reconnecting...");
            connection = DriverManager.getConnection(DB_PATH);
            logger.debug("Connection reestablished.");
        }

        return connection;
    }

    /**
     * Checks whether or not a table exists in the database.
     *
     * @param table the name of the table to check
     * @return if the table already exists
     * @throws SQLException on database access error
     */
    public static boolean tableExists(String table) throws SQLException {
        logger.debug("Checking if table exists: " + table);
        Connection connection = getConnection();
        ResultSet tables = connection.getMetaData().getTables(null, null, table.toUpperCase(), null);
        return tables.next();
    }
}
