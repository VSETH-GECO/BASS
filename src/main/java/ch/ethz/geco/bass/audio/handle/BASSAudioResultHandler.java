package ch.ethz.geco.bass.audio.handle;

import ch.ethz.geco.bass.audio.AudioManager;
import ch.ethz.geco.bass.audio.AudioTrackMetaData;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.java_websocket.WebSocket;

import java.util.List;

public class BASSAudioResultHandler implements AudioLoadResultHandler {
    private WebSocket webSocket;
    private JsonObject jo;

    public BASSAudioResultHandler(WebSocket websocket, JsonObject jo) {
        this.webSocket = websocket;
        this.jo = jo;
    }

    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        // Add metadata
        AudioTrackMetaData metaData = new AudioTrackMetaData(jo.get("userID").getAsString());
        audioTrack.setUserData(metaData);

        // Reply to user
        JsonObject response = new JsonObject();
        response.addProperty("method", "post");
        response.addProperty("type", "ack");
        response.add("data", JsonNull.INSTANCE);
        webSocket.send(response.toString());

        AudioManager.getScheduler().queue(audioTrack);
        //super.trackLoaded(audioTrack);
    }

    @Override
    public void noMatches() {
        // Reply to user
        JsonObject data = new JsonObject();
        data.addProperty("message", "No matches found");

        JsonObject response = new JsonObject();
        response.addProperty("method", "post");
        response.addProperty("type", "err");
        response.add("data", data);
        webSocket.send(response.toString());
    }

    @Override
    public void loadFailed(FriendlyException e) {
        // Reply to user
        JsonObject data = new JsonObject();
        data.addProperty("message", e.getMessage());

        JsonObject response = new JsonObject();
        response.addProperty("method", "post");
        response.addProperty("type", "err");
        response.add("data", data);
        webSocket.send(response.toString());
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        List<AudioTrack> playlist = audioPlaylist.getTracks();

        for (AudioTrack track : playlist) {
            track.setUserData(new AudioTrackMetaData(jo.get("userId").getAsString())); // FIXME: Somehow get the user who added the track
            AudioManager.getScheduler().queue(track);
        }
    }

}
