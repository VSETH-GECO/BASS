package ch.ethz.geco.bass.server.auth;

/**
 * This class represents a user of BASS
 */
public class User {
    /**
     * The ID of the user.
     */
    private final Integer userID;

    /**
     * The username.
     */
    private String name;

    /**
     * If the user is a admin that can create new users
     */
    private boolean isAdmin;

    /**
     * Creates a new user with the given unique ID.
     * @param userID the unique ID of the user
     * @param name of the user
     */
    public User(Integer userID, String name) {
        this.userID = userID;
        this.name = name;
        this.isAdmin = false;
    }

    /**
     * Creates a new user with the given unique ID.
     * @param userID of the user (unique)
     * @param name of the user
     * @param isAdmin Flag if the user is a admin
     */
    public User(Integer userID, String name, boolean isAdmin) {
        this.userID = userID;
        this.name = name;
        this.isAdmin = isAdmin;
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
    public Integer getUserID() {
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

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
}
