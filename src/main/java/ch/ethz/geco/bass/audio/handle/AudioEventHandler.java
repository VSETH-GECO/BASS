package ch.ethz.geco.bass.audio.handle;

import ch.ethz.geco.bass.audio.AudioManager;
import ch.ethz.geco.bass.server.util.RequestSender;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to handle all lava-player audio events.
 */
public class AudioEventHandler extends AudioEventAdapter {
    private static final Logger logger = LoggerFactory.getLogger(AudioEventHandler.class);

    @Override
    public void onPlayerPause(AudioPlayer player) {
        RequestSender.broadcastPlayerState();
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        RequestSender.broadcastPlayerState();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        // Inform users of new player state
        RequestSender.broadcastPlayerState();

        // Log for statistics
        // Disabled this because of missing local database
        //Stats.getInstance().trackPlayed(track);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // Reset track counter if player is stopped
        if (AudioManager.getPlayer().getPlayingTrack() == null) {
            AudioManager.getScheduler().trackCount.set(0);
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
    }


}