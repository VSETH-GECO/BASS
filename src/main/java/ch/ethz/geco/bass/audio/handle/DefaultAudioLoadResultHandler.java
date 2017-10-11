package ch.ethz.geco.bass.audio.handle;

import ch.ethz.geco.bass.audio.AudioManager;
import ch.ethz.geco.bass.audio.AudioTrackMetaData;
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
        audioTrack.setUserData(new AudioTrackMetaData("placeholder")); // FIXME: Somehow get the user who added the track
        AudioManager.getScheduler().queue(audioTrack);
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        AudioTrack selectedTrack = audioPlaylist.getSelectedTrack();
        List<AudioTrack> playlist = audioPlaylist.getTracks();

        if (selectedTrack == null) {
            for (AudioTrack track : playlist) {
                track.setUserData(new AudioTrackMetaData("placeholder")); // FIXME: Somehow get the user who added the track
                AudioManager.getScheduler().queue(track);
            }
        } else {
            selectedTrack.setUserData(new AudioTrackMetaData("placeholder")); // FIXME: Somehow get the user who added the track
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
