package ch.ethz.geco.bass.audio.handle;

import ch.ethz.geco.bass.audio.AudioManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

/**
 * The default audio-load result handler. Nothing fancy
 */
public class DefaultAudioLoadResultHandler implements AudioLoadResultHandler {
    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        AudioManager.getScheduler().queue(audioTrack);
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        AudioTrack selectedTrack = audioPlaylist.getSelectedTrack();
        List<AudioTrack> playlist = audioPlaylist.getTracks();

        if (selectedTrack == null) {
            for (AudioTrack track : playlist) {
                AudioManager.getScheduler().queue(track);
            }
        } else {
            AudioManager.getScheduler().queue(selectedTrack);
        }
    }

    @Override
    public void noMatches() {
    }

    @Override
    public void loadFailed(FriendlyException e) {
    }
}
