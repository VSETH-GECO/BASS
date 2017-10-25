package ch.ethz.geco.bass.server;

import ch.ethz.geco.bass.Main;
import ch.ethz.geco.bass.audio.AudioManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.lang.reflect.Type;
import java.util.List;

public class RequestSender {
    private static final Type playlistType = new TypeToken<List<AudioTrack>>() {
    }.getType();

    /**
     * Broadcasts the current state of the playlist to all connected web sockets.
     */
    public static void broadcastPlaylist() {
        JsonArray trackList = (JsonArray) Main.GSON.toJsonTree(AudioManager.getScheduler().getPlaylist(), playlistType);

        JsonObject response = new JsonObject();
        response.addProperty("method", "post");
        response.addProperty("type", "queue/all");
        response.add("data", trackList);
        Main.server.broadcast(response);
    }

    /**
     * Broadcasts the current track to all connected web sockets.
     */
    public static void broadcastCurrentTrack() {
        JsonObject response = new JsonObject();
        response.addProperty("method", "post");
        response.addProperty("type", "player/current");
        response.add("data", Main.GSON.toJsonTree(AudioManager.getPlayer().getPlayingTrack(), AudioTrack.class));
        Main.server.broadcast(response);
    }

    /**
     * Broadcast new player state to all connected web sockets.
     *
     * @param state the state as string [playing|paused|stopped]
     */
    public static void broadcastState(String state) {
        JsonObject jo = new JsonObject();
        JsonObject data = new JsonObject();

        data.addProperty("state", state);

        jo.addProperty("method", "post");
        jo.addProperty("type", "player/control");
        jo.add("data", data);

        Main.server.broadcast(jo);
    }

    /**
     * Sends an error to the given web socket.
     *
     * @param ws   the web socket to send to
     * @param data the data to send
     */
    public static void sendError(AuthWebSocket ws, JsonObject data) {
        JsonObject response = new JsonObject();
        response.addProperty("method", "post");
        response.addProperty("type", "err");
        response.add("data", data);
        ws.send(response.toString());
    }

    public static void sendUserToken(AuthWebSocket ws, String token) {

    }
}
