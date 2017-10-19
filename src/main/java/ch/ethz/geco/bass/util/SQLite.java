package ch.ethz.geco.bass.util;

import com.sun.istack.internal.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A small helper to make database communication easier.
 */
public class SQLite {
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
    private static final String DB_PATH = "bass.db";

    /**
     * Returns either a valid database connection or null if something went wrong while connecting to the database.
     *
     * @return either a valid database connection or null on error
     */
    @Nullable
    public static Connection getConnection() throws SQLException {
        logger.debug("Checking for a valid connection...");

        if (connection == null || !connection.isValid(0)) {
            logger.debug("Connection is no longer valid, reconnecting...");
            connection = DriverManager.getConnection(DB_PATH);
            logger.debug("Connection reestablished.");
        } else {
            logger.debug("Connection is still valid.");
        }

        return connection;
    }
}
