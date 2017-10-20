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
    public User(String userID, String name) {
        this.userID = userID;
        this.name = name;
    }

    /**
     * Sets the name of this user.
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the ID of this user.
     * @return the ID of this user
     */
    public String getUserID() {
        return userID;
    }

    /**
     * Returns the name of this user.
     * @return the name of this user
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof User && userID.equals(((User) obj).getUserID());
    }
}
