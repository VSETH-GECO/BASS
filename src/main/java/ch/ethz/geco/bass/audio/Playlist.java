package ch.ethz.geco.bass.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a playlist
 * <p>
 * Requirements:
 * - The key of a track doesn't change.
 * - The map is sorted ascending by key (insertion order) and then descending by value (votes of an audio track).
 * <p>
 * Implementation:
 * - Changing votes reinserts the changed element.
 */
public class Playlist {
    private final Comparator<AudioTrack> comparator = Comparator.comparing((AudioTrack track) -> ((AudioTrackMetaData) track.getUserData()).getTrackID()).reversed()
            .thenComparing((AudioTrack track) -> ((AudioTrackMetaData) track.getUserData()).getVoteCount()).reversed();

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
    public boolean add(AudioTrack track) {
        synchronized (trackSet) {
            synchronized (sortedPlaylist) {
                Integer trackID = ((AudioTrackMetaData) track.getUserData()).getTrackID();
                if (trackSet.putIfAbsent(trackID, track) == null) {
                    if (sortedPlaylist.add(track)) {
                        resort();
                        return true;
                    } else {
                        trackSet.remove(trackID);
                    }
                }

                return false;
            }
        }
    }

    /**
     * Returns the next track in order.
     *
     * @return the next track in order, or null if the playlist is empty
     */
    public AudioTrack poll() {
        synchronized (trackSet) {
            synchronized (sortedPlaylist) {
                AudioTrack track = sortedPlaylist.remove(0);
                trackSet.remove(((AudioTrackMetaData) track.getUserData()).getTrackID());
                return track;
            }
        }
    }

    /**
     * Returns a sorted list of all audio tracks.
     *
     * @return a sorted list of all audio tracks
     */
    public List<AudioTrack> getSortedList() {
        return sortedPlaylist;
    }

    /**
     * Resorts the playlist
     */
    public void resort() {
        synchronized (trackSet) {
            synchronized (sortedPlaylist) {
                sortedPlaylist = (ArrayList<AudioTrack>) sortedPlaylist.stream().sorted(comparator).collect(Collectors.toList()); // FIXME: Performance can be improved by using insertion or bubble sort
                // TODO: Broadcast change in WS Server
            }
        }
    }
}
