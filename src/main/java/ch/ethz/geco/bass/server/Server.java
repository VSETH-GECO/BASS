package ch.ethz.geco.bass.server;

import ch.ethz.geco.bass.Main;
import ch.ethz.geco.bass.audio.AudioManager;
import ch.ethz.geco.bass.audio.handle.BASSAudioResultHandler;
import ch.ethz.geco.bass.audio.util.AudioTrackMetaData;
import ch.ethz.geco.bass.server.auth.UserManager;
import ch.ethz.geco.bass.server.util.RequestSender;
import ch.ethz.geco.bass.server.util.WsPackage;
import ch.ethz.geco.bass.util.ErrorHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * Server class
 * <p>
 * Somewhen in the future it should handle all kinds of api
 * requests to modify the queue.
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

        WsPackage.create().method("post").type("app/welcome").send(webSocket);
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
            JsonObject data = new JsonObject();
            data.addProperty("message", "Json parse error");

            RequestSender.sendError(webSocket, data);
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
        Type listType;

        switch (type) {
            case "queue/all":
                listType = new TypeToken<List<AudioTrack>>(){}.getType();
                JsonArray trackList = (JsonArray) Main.GSON.toJsonTree(AudioManager.getScheduler().getPlaylist().getSortedList(), listType);

                WsPackage.create().method("post").type("queue/all").data(trackList).send(webSocket);
                break;

            case "player/current":
                AudioTrack at = AudioManager.getPlayer().getPlayingTrack();
                responseData = at != null ? (JsonObject) Main.GSON.toJsonTree(at, AudioTrack.class) : null;

                WsPackage.create().method("post").type("player/current").data(responseData).send(webSocket);
                break;

            case "player/state":
                AudioPlayer ap = AudioManager.getPlayer();
                // It feels dirty but may actually do what it should should work
                String state = ap.isPaused() ? "paused" : ap.getPlayingTrack() == null ? "stopped" : "playing";
                responseData.addProperty("state", state);

                WsPackage.create().method("post").type("player/control").data(responseData).send(webSocket);
                break;

            case "user/favorite":
                listType = new TypeToken<Map<String, String>>(){}.getType();
                JsonArray favoritesList = (JsonArray) Main.GSON.toJsonTree(UserManager.getFavorites(webSocket.getUser().getUserID()), listType);
                WsPackage.create().method("post").type("user/favorite").data(favoritesList).send(webSocket);
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
        // Unauthorized connection should not be able to patch
        if (!webSocket.isAuthorized()) {
            handleUnauthorized(webSocket, type);
            return;
        }

        switch (type) {
            case "track/vote":
                String userID = webSocket.getUser().getUserID().toString();
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
                if (!webSocket.isAuthorized()) {
                    handleUnauthorized(webSocket, type);
                    return;
                }

                String uri = data.get("uri").getAsString();
                AudioManager.loadAndPlay(uri, new BASSAudioResultHandler(webSocket));
                break;

            case "user/favorite":
                if (!webSocket.isAuthorized()) {
                    handleUnauthorized(webSocket, type);
                    return;
                }

                UserManager.favorite(webSocket, data);
                break;

            case "user/login":
                if (data.get("token") != null) {
                    String token = data.get("token").getAsString();
                    UserManager.login(webSocket, token);
                } else {
                    String username = data.get("username").getAsString();
                    String password = data.get("password").getAsString();
                    UserManager.login(webSocket, username, password);
                }

                break;

            case "user/register":
                if (!webSocket.isAuthorized() || !webSocket.getUser().isAdmin()) {
                    handleUnauthorized(webSocket, type);
                    return;
                }

                if (data.get("username") != null && data.get("password") != null) {
                    UserManager.register(webSocket, data.get("username").getAsString(), data.get("password").getAsString());
                }

                break;

            case "user/setadmin":
                if (!webSocket.isAuthorized() || !webSocket.getUser().isAdmin()) {
                    handleUnauthorized(webSocket, type);
                    return;
                }

                if (data.get("username") != null && data.get("admin") != null) {
                    UserManager.setAdmin(webSocket, data.get("username").getAsString(), data.get("admin").getAsBoolean());
                }

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
        switch (type) {
            case "user/logout":
                UserManager.logout(webSocket, data.get("token").getAsString());
                break;

        }
    }

    // TODO add to error handler
    private void handleUnauthorized(AuthWebSocket webSocket, String type) {
        JsonObject data = new JsonObject();
        data.addProperty("message", "Your connection is unauthorized. Log in or upgrade to admin to perform this action.");
        data.addProperty("type", type);

        WsPackage.create().method("post").type("user/unauthorized").data(data).send(webSocket);
    }

    public void stopSocket() {
        // Inform connections about stopping the playback
        JsonObject data = new JsonObject();
        data.addProperty("state", "stopped");
        WsPackage.create().method("post").type("player/control").data(data).broadcast();

        // Shutdown socket to free port
        try {
            this.stop(1000);
        } catch (InterruptedException e) {
            ErrorHandler.handleLocal(e);
        }
    }

    public void broadcast(JsonObject jo) {
        broadcast(jo.toString());
    }
}
