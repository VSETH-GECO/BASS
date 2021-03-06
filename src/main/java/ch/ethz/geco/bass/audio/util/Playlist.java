package ch.ethz.geco.bass.audio.util;

import ch.ethz.geco.bass.server.Server;
import ch.ethz.geco.bass.server.util.RequestSender;
import ch.ethz.geco.bass.server.util.WsPackage;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.*;

/**
 * Represents a playlist.
 */
public class Playlist {
    /**
     * A comparator which can be used to compare tracks by votes and by ID. It first compares the tracks by votes and if they are equal by ID.
     * If you sort ascending with this comparator, you will get a list of tracks with descending votes and ascending ID's.
     */
    private final Comparator<AudioTrack> compareByVotesAndID = Comparator.comparing((AudioTrack track) -> ((AudioTrackMetaData) track.getUserData()).getVoteCount()).reversed().thenComparing(Comparator.comparing((AudioTrack track) -> ((AudioTrackMetaData) track.getUserData()).getTrackID()));

    /**
     * The internal mapping of trackIDs to tracks.
     */
    private final HashMap<Integer, AudioTrack> trackSet = new HashMap<>();

    /**
     * The playlist in a sorted format.
     */
    private ArrayList<AudioTrack> sortedPlaylist = new ArrayList<>();

    /**
     * Adds a track to the playlist if the track is not already in it.
     *
     * @param track the track to add
     * @return true on success, false if the track is already in the playlist
     */
    public boolean add(AudioTrack track, boolean resort) {
        synchronized (this) {
            Integer trackID = ((AudioTrackMetaData) track.getUserData()).getTrackID();
            if (trackSet.putIfAbsent(trackID, track) == null) {
                if (sortedPlaylist.add(track)) {
                    if (resort)
                        resort();
                    return true;
                } else {
                    trackSet.remove(trackID);
                }
            }

            return false;
        }
    }

    /**
     * Returns the next track in order.
     *
     * @return the next track in order, or null if the playlist is empty
     */
    public AudioTrack poll() {
        synchronized (this) {
            if (sortedPlaylist.isEmpty()) {
                // Broadcast to users
                JsonObject data = new JsonObject();

                data.addProperty("state", "stopped");
                data.add("track", null);
                WsPackage.create().resource(Server.Resource.PLAYER).action(Server.Action.DATA).data(data).broadcast();

                return null;
            }
            AudioTrack track = sortedPlaylist.remove(0);
            trackSet.remove(((AudioTrackMetaData) track.getUserData()).getTrackID());

            RequestSender.broadcastPlaylist();

            return track;
        }
    }

    /**
     * Returns a sorted list of all audio tracks.
     *
     * @return a sorted list of all audio tracks
     */
    public List<AudioTrack> getSortedList() {
        synchronized (this) {
            return sortedPlaylist;
        }
    }

    /**
     * Sets the vote of a user for the given track.
     * <p>
     * vote = 0 means that the vote gets removed for that user.
     *
     * @param trackID the ID of the track
     * @param userID  the ID of the user who voted
     * @param vote    the vote
     * @return false if there is no track with the given ID in the playlist, true otherwise
     */
    public boolean setVote(Integer trackID, Integer userID, Byte vote) {
        AudioTrack track = trackSet.get(trackID);
        if (track != null) {
            ((AudioTrackMetaData) track.getUserData()).getVotes().put(userID, vote);
            resort();

            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the AudioTrack with the given ID or null of none was found.
     *
     * @param trackID the ID of the track
     * @return the AudioTrack with the given ID or null of none was found
     */
    public AudioTrack getTrack(Integer trackID) {
        return trackSet.get(trackID);
    }

    /**
     * Skips the the AudioTrack with the given ID.
     *
     * @param trackID the ID of the track to skip
     * @return if the operation was successful
     */
    public boolean skipTrack(Integer trackID) {
        AudioTrack removedTrack = trackSet.remove(trackID);
        if (removedTrack != null) {
            sortedPlaylist.remove(removedTrack);
            resort();
            return true;
        }

        return false;
    }

    /**
     * Resorts the playlist
     */
    public void resort() {
        synchronized (this) {
            // Insertion sort the playlist according to https://en.wikipedia.org/wiki/Insertion_sort
            AudioTrack[] tracks = sortedPlaylist.toArray(new AudioTrack[sortedPlaylist.size()]);
            for (int i = 1; i < tracks.length; i++) {
                AudioTrack x = tracks[i];
                int j = i - 1;
                while (j >= 0 && compareByVotesAndID.compare(tracks[j], x) > 0) {
                    tracks[j + 1] = tracks[j];
                    j--;
                }
                tracks[j + 1] = x;
            }

            sortedPlaylist = new ArrayList<>(Arrays.asList(tracks));

            RequestSender.broadcastPlaylist();
        }
    }
}
