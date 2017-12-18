package ch.ethz.geco.bass.audio.handle;

import ch.ethz.geco.bass.audio.AudioManager;
import ch.ethz.geco.bass.audio.util.AudioTrackMetaData;
import ch.ethz.geco.bass.server.AuthWebSocket;
import ch.ethz.geco.bass.server.Server;
import ch.ethz.geco.bass.server.util.RequestSender;
import ch.ethz.geco.bass.server.util.WsPackage;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

/**
 * Implementation for the AudioLoadResultHandler
 * <p>
 * Takes care of what happens after a track has been loaded. Informing users and adding information to the tracks.
 */
public class BASSAudioResultHandler implements AudioLoadResultHandler {
    private AuthWebSocket webSocket;

    /**
     * Initialises the result handler
     *
     * @param webSocket the web socket to respond to
     */
    public BASSAudioResultHandler(AuthWebSocket webSocket) {
        this.webSocket = webSocket;
    }

    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        // Add metadata
        AudioTrackMetaData metaData = new AudioTrackMetaData(AudioManager.getScheduler().trackCount.getAndIncrement(), webSocket.getUser().getUserID(), webSocket.getUser().getName());
        audioTrack.setUserData(metaData);

        // Queue track
        AudioManager.getScheduler().queue(audioTrack, true);

        // Reply to user
        WsPackage.create(Server.Resource.QUEUE, Server.Action.SUCCESS).send(webSocket);
    }

    @Override
    public void noMatches() {
        // Reply to user
        JsonObject data = new JsonObject();
        data.addProperty("message", "No matches found");

        RequestSender.sendError(webSocket, Server.Resource.QUEUE, data);
    }

    @Override
    public void loadFailed(FriendlyException e) {
        // Reply to user
        JsonObject data = new JsonObject();
        data.addProperty("message", e.getMessage());

        RequestSender.sendError(webSocket, Server.Resource.QUEUE, data);
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        List<AudioTrack> playlist = audioPlaylist.getTracks();

        for (AudioTrack track : playlist) {
            track.setUserData(new AudioTrackMetaData(AudioManager.getScheduler().trackCount.getAndIncrement(), webSocket.getUser().getUserID(), webSocket.getUser().getName()));
            AudioManager.getScheduler().queue(track, false);
        }

        AudioManager.getScheduler().getPlaylist().resort();

        // Reply to user
        WsPackage.create(Server.Resource.QUEUE, Server.Action.SUCCESS).send(webSocket);
    }
}
