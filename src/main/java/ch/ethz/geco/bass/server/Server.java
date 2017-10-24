package ch.ethz.geco.bass.server;

import ch.ethz.geco.bass.Main;
import ch.ethz.geco.bass.audio.AudioManager;
import ch.ethz.geco.bass.audio.handle.BASSAudioResultHandler;
import ch.ethz.geco.bass.audio.util.AudioTrackMetaData;
import ch.ethz.geco.bass.server.auth.UserManager;
import ch.ethz.geco.bass.util.ErrorHandler;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * Server class
 * <p>
 * Somewhen in the future it should handle all kinds of api
 * requests to modify the queue. Maybe even provide a web-
 * interface.
 */
public class Server extends AuthWebSocketServer {
    enum Method {get, post, patch, delete}

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public Server(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(AuthWebSocket webSocket, ClientHandshake clientHandshake) {
        logger.info(webSocket.getRemoteSocketAddress().getHostString() + " connected!");

        JsonObject jo = new JsonObject();
        jo.addProperty("method", "post");
        jo.addProperty("type", "app/welcome");
        jo.add("data", JsonNull.INSTANCE);

        webSocket.send(jo.toString());
    }

    @Override
    public void onClose(AuthWebSocket webSocket, int i, String s, boolean b) {
        logger.info(webSocket.getRemoteSocketAddress().getHostString() + " disconnected!");
    }

    @Override
    public void onError(AuthWebSocket webSocket, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onStart() {
        logger.info("WS Server started!");
    }

    @Override
    public void onMessage(AuthWebSocket webSocket, String msg) {
        logger.debug("Message from (" + webSocket.getRemoteSocketAddress().getHostString() + "): " + msg);

        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(msg);

        if (je.isJsonObject()) {
            JsonObject wsPacket = je.getAsJsonObject();
            JsonObject data = wsPacket.get("data").isJsonObject() ? wsPacket.getAsJsonObject("data") : null;

            Method method = Method.valueOf(wsPacket.get("method").getAsString());
            String type = wsPacket.get("type").getAsString();

            switch (method) {
                case get:
                    handleGet(webSocket, type, data);
                    break;
                case post:
                    handlePost(webSocket, type, data);
                    break;
                case patch:
                    handlePatch(webSocket, type, data);
                    break;
                case delete:
                    handleDelete(webSocket, type, data);
                    break;

                default:
                    break;
            }

        } else {
            JsonObject jo = new JsonObject();
            JsonObject data = new JsonObject();

            data.addProperty("message", "Json parse error");
            jo.addProperty("method", "post");
            jo.addProperty("type", "error");
            jo.add("data", data);

            webSocket.send(Main.GSON.toJson(jo));
        }
    }

    /**
     * Implemented api endpoints:
     * <p>
     * - queue/all
     * - player/current
     * - player/state
     *
     * @param webSocket the websocket(connection) that send the msg
     * @param type      or api endpoint that should be reached
     * @param data      object that holds more information on what to do
     */
    private void handleGet(AuthWebSocket webSocket, String type, JsonObject data) {
        JsonObject responseData = new JsonObject();
        JsonObject response = new JsonObject();

        switch (type) {
            case "queue/all":
                Type listType = new TypeToken<List<AudioTrack>>(){}.getType();
                JsonArray trackList = (JsonArray) Main.GSON.toJsonTree(AudioManager.getScheduler().getPlaylist().getSortedList(), listType);

                response.addProperty("method", "post");
                response.addProperty("type", "queue/all");
                response.add("data", trackList);
                webSocket.send(response.toString());

                break;

            case "player/current":
                AudioTrack at = AudioManager.getPlayer().getPlayingTrack();

                responseData = at != null ? (JsonObject) Main.GSON.toJsonTree(at, AudioTrack.class) : null;

                response.addProperty("method", "post");
                response.addProperty("type", "player/current");
                response.add("data", responseData);

                webSocket.send(response.toString());

                break;

            case "player/state":
                AudioPlayer ap = AudioManager.getPlayer();
                // It feels dirty but may actually do what it should should work
                String state = ap.isPaused() ? "paused" : ap.getPlayingTrack() == null ? "stopped" : "playing";
                responseData.addProperty("state", state);

                response.addProperty("method", "post");
                response.addProperty("type", "player/control");
                response.add("data", responseData);
                webSocket.send(response.toString());
        }
    }

    /**
     * Implemented api endpoints:
     * <p>
     * - track/vote
     *
     * @param webSocket the websocket(connection) that send the msg
     * @param type      or api endpoint that should be reached
     * @param data      object that holds more information on what to do
     */
    private void handlePatch(AuthWebSocket webSocket, String type, JsonObject data) {

        switch (type) {
            case "track/vote":
                String userID = data.get("userID").getAsString();
                Byte vote = data.get("vote").getAsByte();
                int trackID = data.get("id").getAsInt();

                if (vote <= 1 && vote >= -1) {
                    if (trackID == 0) {
                        ((AudioTrackMetaData) AudioManager.getPlayer().getPlayingTrack().getUserData()).getVotes().put(userID, vote);
                    } else {
                        AudioManager.getScheduler().getPlaylist().setVote(trackID, userID, vote);
                    }
                }

                break;


            case "player/control":
                AudioManager.getPlayer().setPaused(
                        // Note that also 'stopped' and totally invalid parameters will set it to playing, but I guess that's ok
                        data.get("state").getAsString().equals("pause")
                );
                break;
        }
    }

    /**
     * Implemented api endpoints:
     * <p>
     * - queue/uri
     * - player/control
     *
     * @param webSocket the websocket(connection) that send the msg
     * @param type      or api endpoint that should be reached
     * @param data      object that holds more information on what to do
     */
    private void handlePost(AuthWebSocket webSocket, String type, JsonObject data) {

        switch (type) {
            case "queue/uri":
                String uri = data.get("uri").getAsString();
                AudioManager.loadAndPlay(uri, new BASSAudioResultHandler(webSocket, data));
                break;

            case "user/login":
                if (data.get("token").isJsonObject()) {
                    String token = data.get("token").getAsString();
                } else {
                    String username = data.get("username").getAsString();
                    String password = data.get("password").getAsString();
                }

                // TODO upgrade connection to logged in connection

                break;
        }
    }

    /**
     * Implemented api endpoints:
     * <p>
     * none
     *
     * @param webSocket the websocket(connection) that send the msg
     * @param type      or api endpoint that should be reached
     * @param data      object that holds more information on what to do
     */
    private void handleDelete(AuthWebSocket webSocket, String type, JsonObject data) {

    }

    public void stopSocket() {
        // Inform connections about stopping the playback
        JsonObject jo = new JsonObject();
        JsonObject data = new JsonObject();

        data.addProperty("state", "stopped");

        jo.addProperty("method", "post");
        jo.addProperty("type", "player/control");
        jo.add("data", data);
        broadcast(jo);

        // Shutdown socket to free port
        try {
            this.setReuseAddr(true);
            this.stop(1000);
        } catch (InterruptedException e) {
            ErrorHandler.handleLocal(e);
        }
    }

    public void broadcast(JsonObject jo) {
        broadcast(jo.toString());
    }
}
