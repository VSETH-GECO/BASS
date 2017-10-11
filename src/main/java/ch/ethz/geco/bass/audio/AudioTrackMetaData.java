package ch.ethz.geco.bass.audio;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to store additional information about an audio track.
 */
public class AudioTrackMetaData {
    /**
     * A map of users and their votes (1 = positive, -1 = negative).
     */
    private final Map<String, Byte> votes = new HashMap<>();

    /**
     * The ID of the user who added the track.
     */
    private final String userID;

    /**
     * Creates a new meta data object with the given user and an empty vote map.
     *
     * @param userID the user who added the track
     */
    public AudioTrackMetaData(String userID) {
        this.userID = userID;
    }

    /**
     * Returns a map with all voters and their vote.
     *
     * @return a map with all voters and their vote
     */
    public Map<String, Byte> getVotes() {
        return votes;
    }

    /**
     * Returns the userID of the user who added the track.
     *
     * @return the userID of the user who added the track
     */
    public String getUserID() {
        return userID;
    }

    /**
     * Returns the absolute number of votes of this track.
     * This means every negative vote counts as -1 and every positive vote counts as +1.
     *
     * @return the absolute number of votes of this track
     */
    public int getVoteCount() {
        int voteCount = 0;
        for (Byte vote : votes.values()) {
            voteCount += vote;
        }

        return voteCount;
    }
}
