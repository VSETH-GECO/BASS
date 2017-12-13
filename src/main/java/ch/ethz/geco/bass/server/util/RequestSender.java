package ch.ethz.geco.bass.server.util;

import ch.ethz.geco.bass.Main;
import ch.ethz.geco.bass.audio.AudioManager;
import ch.ethz.geco.bass.server.AuthWebSocket;
import ch.ethz.geco.bass.server.Server.Action;
import ch.ethz.geco.bass.server.Server.Resource;
import ch.ethz.geco.bass.util.ErrorHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
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
        WsPackage.create().resource(Resource.QUEUE).action(Action.DATA).data(trackList).broadcast();
    }

    private static JsonObject getPlayerState() {
        JsonObject responseData = new JsonObject();
        AudioPlayer ap = AudioManager.getPlayer();
        AudioTrack at = ap.getPlayingTrack();
        responseData.addProperty("state", ap.isPaused() ? "paused" : ap.getPlayingTrack() == null ? "stopped" : "playing");
        responseData.add("track", at != null ? (JsonObject) Main.GSON.toJsonTree(at, AudioTrack.class) : null);

        return responseData;
    }

    /**
     * Broadcasts the player state and current track to all connected web sockets.
     */
    public static void broadcastPlayerState() {
        WsPackage.create().resource(Resource.PLAYER).action(Action.DATA).data(getPlayerState()).broadcast();
    }

    /**
     * Sends the player state and current track to the specified websocket.
     *
     * @param ws to send the player state to
     */
    public static void sendPlayerState(AuthWebSocket ws) {
        WsPackage.create().resource(Resource.PLAYER).action(Action.DATA).data(getPlayerState()).send(ws);
    }

    /**
     * Sends an error to the given web socket.
     *
     * @param ws   the web socket to send to
     * @param data the data to send
     */
    public static void sendError(AuthWebSocket ws, Resource resource, JsonObject data) {
        WsPackage.create().resource(resource).action(Action.ERROR).data(data).send(ws);
    }

    /**
     * Handles an internal error by sending a notification to the web socket who triggered the error and also
     * handles the error locally.
     *
     * @param webSocket the web socket who triggered the internal error
     * @param e         the error
     */
    public static void handleInternalError(AuthWebSocket webSocket, Throwable e) {
        JsonObject data = new JsonObject();
        data.addProperty("code", 500);
        data.addProperty("message", "Internal Server Error");
        WsPackage.create().resource(Resource.APP).action(Action.ERROR).data(data).send(webSocket);

        ErrorHandler.handleLocal(e);
    }

    /**
     * Sends a token to the given web socket.
     *
     * @param ws    the web socket to send to
     * @param token the token to send
     */
    public static void sendUserToken(AuthWebSocket ws, String token, String username, int userID, boolean isAdmin) {
        JsonObject data = new JsonObject();
        data.addProperty("id", userID);
        data.addProperty("token", token);
        data.addProperty("admin", isAdmin);
        data.addProperty("username", username);
        WsPackage.create().resource(Resource.USER).action(Action.DATA).data(data).send(ws);
    }
}
