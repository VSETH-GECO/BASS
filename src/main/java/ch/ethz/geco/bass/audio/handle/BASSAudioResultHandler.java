package ch.ethz.geco.bass.audio.handle;

import ch.ethz.geco.bass.audio.AudioManager;
import ch.ethz.geco.bass.audio.util.AudioTrackMetaData;
import ch.ethz.geco.bass.server.AuthWebSocket;
import ch.ethz.geco.bass.server.RequestSender;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BASSAudioResultHandler implements AudioLoadResultHandler {
    private static final AtomicInteger trackCount = new AtomicInteger(0);
    private AuthWebSocket webSocket;

    public BASSAudioResultHandler(AuthWebSocket webSocket) {
        this.webSocket = webSocket;
    }

    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        // Add metadata
        AudioTrackMetaData metaData = new AudioTrackMetaData(trackCount.getAndIncrement(), webSocket.getUser().getUserID().toString());
        audioTrack.setUserData(metaData);

        // Queue track
        AudioManager.getScheduler().queue(audioTrack);

        // Reply to user
        JsonObject response = new JsonObject();
        response.addProperty("method", "post");
        response.addProperty("type", "ack");
        response.add("data", JsonNull.INSTANCE);
        webSocket.send(response.toString());

        // Inform all connected users
        RequestSender.broadcastPlaylist();
    }

    @Override
    public void noMatches() {
        // Reply to user
        JsonObject data = new JsonObject();
        data.addProperty("message", "No matches found");

        RequestSender.sendError(webSocket, data);
    }

    @Override
    public void loadFailed(FriendlyException e) {
        // Reply to user
        JsonObject data = new JsonObject();
        data.addProperty("message", e.getMessage());

        RequestSender.sendError(webSocket, data);
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        List<AudioTrack> playlist = audioPlaylist.getTracks();

        for (AudioTrack track : playlist) {
            track.setUserData(new AudioTrackMetaData(trackCount.getAndIncrement(), webSocket.getUser().getUserID().toString()));
            AudioManager.getScheduler().queue(track);
        }
    }
}
