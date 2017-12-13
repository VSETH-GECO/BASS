package ch.ethz.geco.bass.server;

import ch.ethz.geco.bass.Main;
import ch.ethz.geco.bass.audio.AudioManager;
import ch.ethz.geco.bass.audio.util.AudioTrackMetaData;
import ch.ethz.geco.bass.audio.util.Playlist;
import ch.ethz.geco.bass.server.util.RequestSender;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.java_websocket.WebSocket;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class VoteHandler {
    /**
     * A map of users and their vote expiration time.
     * The key is the user ID and the value is the UNIX time when the votes of the user will expire.
     */
    private static final Map<Integer, Long> expiringUsers = new HashMap<>();

    /**
     * The time in milliseconds after which the votes of a disconnected user will expire.
     */
    private static final int EXPIRATION_TIME = 60000;

    static {
        // Periodically expire votes of disconnected users
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                expireVotes();
            }
        }, 1000, 1000);
    }

    /**
     * Handles a vote, removing tracks if they have too many negative votes.
     *
     * @param webSocket The web socket who voted.
     * @param trackID   The track which was voted for.
     * @param vote      The value of the vote.
     */
    public static void handle(AuthWebSocket webSocket, int trackID, byte vote) {
        int userID = webSocket.getUser().getUserID();

        if (vote <= 1 && vote >= -1) {
            int authorizedUsers = 0;
            for (WebSocket connection : Main.server.connections()) {
                if (((AuthWebSocket) connection).getUser() != null) {
                    authorizedUsers++;
                }
            }

            // TODO: maybe remove tracks before setting vote to avoid sending player updates multiple times
            Playlist playlist = AudioManager.getScheduler().getPlaylist();
            AudioTrack currentTrack = AudioManager.getPlayer().getPlayingTrack();
            if (playlist.setVote(trackID, userID, vote)) {
                AudioTrackMetaData trackMetaData = (AudioTrackMetaData) playlist.getTrack(trackID).getUserData();
                if (trackMetaData.getVoteCount() < Math.negateExact((int) Math.ceil(((double) authorizedUsers / 2) - 0.5))) {
                    playlist.skipTrack(trackID);
                }
            } else if (currentTrack != null) { // Maybe it's the current track
                AudioTrackMetaData trackMetaData = (AudioTrackMetaData) currentTrack.getUserData();
                
                // Check if we actually want to vote on the current track or if the trackID is simply invalid
                if (trackMetaData.getTrackID() == trackID) {
                    trackMetaData.getVotes().put(userID, vote);

                    if (trackMetaData.getVoteCount() < Math.negateExact((int) Math.ceil(((double) authorizedUsers / 2) - 0.5))) {
                        AudioManager.getScheduler().nextTrack();
                    } else {
                        RequestSender.broadcastCurrentTrack();
                    }
                }
            }
        }
    }

    /**
     * Rechecks every track in the playlist including the current running track if they can be removed now.
     * Call this if a user disconnected.
     */
    public static void recheckPlaylist() {
        int authorizedUsers = 0;
        for (WebSocket connection : Main.server.connections()) {
            if (((AuthWebSocket) connection).getUser() != null) {
                authorizedUsers++;
            }
        }

        // Check current track
        AudioTrackMetaData currentTrack = AudioManager.getPlayer().getPlayingTrack().getUserData(AudioTrackMetaData.class);
        if (currentTrack.getVoteCount() < Math.negateExact((int) Math.ceil(((double) authorizedUsers / 2) - 0.5))) {
            AudioManager.getScheduler().nextTrack();
        }

        // Check playlist
        for (AudioTrack track : AudioManager.getScheduler().getPlaylist().getSortedList()) {
            AudioTrackMetaData trackMetaData = track.getUserData(AudioTrackMetaData.class);

            if (trackMetaData.getVoteCount() < Math.negateExact((int) Math.ceil(((double) authorizedUsers / 2) - 0.5))) {
                AudioManager.getScheduler().nextTrack();
            }
        }
    }

    /**
     * Schedules the vote expiration of the given user.
     *
     * @param userID The ID of the user.
     */
    public static void scheduleExpiry(int userID) {
        expiringUsers.put(userID, System.currentTimeMillis() + EXPIRATION_TIME);
    }

    /**
     * Removes the user from the vote expiration.
     *
     * @param userID The ID of the user.
     */
    public static void removeExpiry(int userID) {
        expiringUsers.remove(userID);
    }

    /**
     * Removes expired votes from the current track and playlist.
     */
    private static void expireVotes() {
        boolean expiredSomething = false;

        for (Integer userID : expiringUsers.keySet()) {
            // If the votes of the current user expired
            if (expiringUsers.get(userID) > System.currentTimeMillis()) {
                // Remove vote from current track
                ((AudioTrackMetaData) AudioManager.getPlayer().getPlayingTrack().getUserData()).getVotes().put(userID, (byte) 0);

                // Remove votes from playlist
                for (AudioTrack track : AudioManager.getScheduler().getPlaylist().getSortedList()) {
                    ((AudioTrackMetaData) track.getUserData()).getVotes().put(userID, (byte) 0);
                }

                expiredSomething = true;
            }
        }

        if (expiredSomething) {
            VoteHandler.recheckPlaylist();
        }
    }
}
