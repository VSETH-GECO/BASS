package ch.ethz.geco.bass.audio;

import ch.ethz.geco.bass.TrackScheduler;
import ch.ethz.geco.bass.audio.handle.AudioEventHandler;
import ch.ethz.geco.bass.audio.handle.DefaultAudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;

/**
 * Holder for both the player and a track scheduler for one guild.
 */
public class AudioManager {
    /**
     * The AudioPlayerManager of this AudioManager.
     */
    private static final AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();

    /**
     * Audio player of this AudioManager.
     */
    private static final AudioPlayer player = audioPlayerManager.createPlayer();

    /**
     * Track scheduler for the player.
     */
    private static final TrackScheduler scheduler = new TrackScheduler(player);

    /**
     * Initialize static variables
     */
    static {
        player.addListener(scheduler);
        player.addListener(new AudioEventHandler());

        audioPlayerManager.setPlayerCleanupThreshold(Long.MAX_VALUE);
    }

    /**
     * Returns the AudioPlayer of this AudioManager.
     *
     * @return the AudioPlayer
     */
    public static AudioPlayer getPlayer() {
        return player;
    }

    /**
     * Returns the TrackScheduler of this AudioManager.
     *
     * @return the TrackScheduler
     */
    public static TrackScheduler getScheduler() {
        return scheduler;
    }

    /**
     * Returns the AudioPlayerManager.
     *
     * @return the AudioPlayerManager
     */
    public static AudioPlayerManager getAudioPlayerManager() {
        return audioPlayerManager;
    }

    /**
     * Load and play a url with the given result handler.
     *
     * @param url           the url to load
     * @param resultHandler the result handler to use
     */
    public static void loadAndPlay(String url, AudioLoadResultHandler resultHandler) {
        audioPlayerManager.loadItem(url, resultHandler);
    }

    /**
     * Load and play a url with the default result handler needing a channel to respond to.
     *
     * @param url     the url to load
     */
    public void loadAndPlay(String url) {
        audioPlayerManager.loadItem(url, new DefaultAudioLoadResultHandler());
    }
}