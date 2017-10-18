package ch.ethz.geco.bass.server.auth;

/**
 * This class represents a user of BASS
 */
public class User {
    /**
     * The ID of the user.
     */
    private final String userID;

    /**
     * The username.
     */
    private String name;

    /**
     * Creates a new user with the given unique ID.
     * @param userID the unique ID of the user
     */
    public User(String userID) {
        this.userID = userID;
    }

    /**
     * Sets the name of this user.
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }
}
