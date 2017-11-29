package ch.ethz.geco.bass.audio.handle;

import ch.ethz.geco.bass.server.util.RequestSender;
import ch.ethz.geco.bass.util.Stats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

/**
 * A class to handle all lava-player audio events.
 */
public class AudioEventHandler extends AudioEventAdapter {
    @Override
    public void onPlayerPause(AudioPlayer player) {
        RequestSender.broadcastCurrentTrack();
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        RequestSender.broadcastCurrentTrack();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        // Inform users of new player state
        RequestSender.broadcastCurrentTrack();

        // Log for statistics
        Stats.getInstance().trackPlayed(track);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
    }


}