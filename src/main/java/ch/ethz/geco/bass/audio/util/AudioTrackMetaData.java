package ch.ethz.geco.bass.audio.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to store additional information about an audio track.
 */
public class AudioTrackMetaData {
    /**
     * A map of users and their votes (1 = positive, -1 = negative).
     */
    private final Map<Integer, Byte> votes = new HashMap<>();

    /**
     * The ID of the track in the player. <p>
     * Note: This is NOT the same as the ID of the track given by lava-player.
     */
    private final Integer trackID;

    /**
     * The ID of the user who added the track.
     */
    private final Integer userID;

    /**
     * The Name of the user who added the track.
     */
    private final String userName;

    /**
     * Creates a new meta data object with the given user and an empty vote map.
     *
     * @param userID the user who added the track
     */
    public AudioTrackMetaData(Integer trackID, Integer userID, String userName) {
        this.trackID = trackID;
        this.userID = userID;
        this.userName = userName;
    }

    /**
     * Returns a map with all voters and their vote.
     *
     * @return a map with all voters and their vote
     */
    public Map<Integer, Byte> getVotes() {
        return votes;
    }

    /**
     * Returns the ID of the track in the player. <p>
     * Note: This is NOT the same as the ID of the track given by lava-player.
     *
     * @return the ID of the track in the player
     */
    public Integer getTrackID() {
        return trackID;
    }

    /**
     * Returns the userID of the user who added the track.
     *
     * @return the userID of the user who added the track
     */
    public Integer getUserID() {
        return userID;
    }

    /**
     * Returns the username of the user who added the track.
     *
     * @return the username of the user who added the track
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Returns the sum of all votes of this track.
     *
     * @return the sum of all votes of this track
     */
    public int getVoteCount() {
        int voteCount = 0;
        for (Byte vote : votes.values()) {
            voteCount += vote;
        }

        return voteCount;
    }
}
