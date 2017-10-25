package ch.ethz.geco.bass.server.util;

import ch.ethz.geco.bass.Main;
import ch.ethz.geco.bass.audio.AudioManager;
import ch.ethz.geco.bass.server.AuthWebSocket;
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
        JsonArray trackList = (JsonArray) Main.GSON.toJsonTree(AudioManager.getScheduler().getPlaylist().getSortedList(), playlistType);
        WsPackage.create().method("post").type("queue/all").data(trackList).broadcast();
    }

    /**
     * Broadcasts the current track to all connected web sockets.
     */
    public static void broadcastCurrentTrack() {
        WsPackage.create().method("post").type("player/current").data(Main.GSON.toJsonTree(AudioManager.getPlayer().getPlayingTrack(), AudioTrack.class)).broadcast();
    }

    /**
     * Broadcast new player state to all connected web sockets.
     *
     * @param state the state as string [playing|paused|stopped]
     */
    public static void broadcastState(String state) {
        JsonObject data = new JsonObject();
        data.addProperty("state", state);
        WsPackage.create().method("post").type("player/control").data(data).broadcast();
    }

    /**
     * Sends an error to the given web socket.
     *
     * @param ws   the web socket to send to
     * @param data the data to send
     */
    public static void sendError(AuthWebSocket ws, JsonObject data) {
        WsPackage.create().method("post").type("err").data(data).send(ws);
    }

    /**
     * Sends a token to the given web socket.
     *
     * @param ws    the web socket to send to
     * @param token the token to send
     */
    public static void sendUserToken(AuthWebSocket ws, String token) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        WsPackage.create().method("post").type("user/token").data(data).send(ws);
    }
}
