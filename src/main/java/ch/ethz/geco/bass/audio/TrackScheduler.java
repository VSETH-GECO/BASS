package ch.ethz.geco.bass.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final AtomicInteger trackCount;
    private final ConcurrentSkipListMap<Integer, AudioTrack> queue;

    /**
     * @param player The audio player this scheduler uses
     */
    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.trackCount = new AtomicInteger(0);
        this.queue = new ConcurrentSkipListMap<>();
    }

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     *
     * @param track The track to play or add to queue.
     */
    public void queue(AudioTrack track) {
        // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
        // something is playing, it returns false and does nothing. In that case the player was already playing so this
        // track goes to the queue instead.
        if (!player.startTrack(track, true)) {
            queue.put(trackCount.getAndIncrement(), track);
        }
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     */
    private void nextTrack() {
        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
        // giving null to startTrack, which is a valid argument and will simply stop the player.
        player.startTrack(queue.pollLastEntry().getValue(), false);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
        if (endReason.mayStartNext) {
            nextTrack();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {

    }

    /**
     * Returns the playlist containing all currently queued tracks.
     *
     * @return the playlist
     */
    public ConcurrentSkipListMap<Integer, AudioTrack> getPlaylist() {
        return queue;
    }
}