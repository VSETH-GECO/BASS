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
        AudioTrackMetaData metaData = new AudioTrackMetaData(trackCount.getAndIncrement(), webSocket.getUser().getUserID(), webSocket.getUser().getName());
        audioTrack.setUserData(metaData);

        // Queue track
        AudioManager.getScheduler().queue(audioTrack, true);

        // Reply to user
        WsPackage.create().resource(Server.Resource.QUEUE).action(Server.Action.SUCCESS).send(webSocket);

        // Inform all connected users
        RequestSender.broadcastPlaylist();
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

        for (int i = 0; i < playlist.size(); i++) {
            AudioTrack track = playlist.get(i);
            track.setUserData(new AudioTrackMetaData(trackCount.getAndIncrement(), webSocket.getUser().getUserID(), webSocket.getUser().getName()));
            AudioManager.getScheduler().queue(track, i == playlist.size() - 1);
        }

        // Reply to user
        WsPackage.create().resource(Server.Resource.QUEUE).action(Server.Action.SUCCESS).send(webSocket);
    }
}
