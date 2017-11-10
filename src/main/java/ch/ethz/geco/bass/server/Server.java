package ch.ethz.geco.bass.server;

import ch.ethz.geco.bass.Main;
import ch.ethz.geco.bass.audio.AudioManager;
import ch.ethz.geco.bass.audio.handle.BASSAudioResultHandler;
import ch.ethz.geco.bass.audio.util.AudioTrackMetaData;
import ch.ethz.geco.bass.server.auth.UserManager;
import ch.ethz.geco.bass.server.util.FavoriteTrack;
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

/**
 * Server class
 * <p>
 * Somewhen in the future it should handle all kinds of api
 * requests to modify the queue.
 */
public class Server extends AuthWebSocketServer {
    private static final String API_VERSION = "v1";
    public enum Resource {APP, PLAYER, QUEUE, USER, FAVORITES, TRACK}
    public enum Action {GET, SET, ADD, DELETE, LOGIN, LOGOUT, INFORM, URI, REGISTER, VOTE, SETADMIN, SUCCESS, ERROR, DATA;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public Server(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(AuthWebSocket webSocket, ClientHandshake clientHandshake) {
        logger.info(webSocket.getRemoteSocketAddress().getHostString() + " connected!");

        JsonObject responseData = new JsonObject();
        responseData.addProperty("apiVersion", API_VERSION);
        WsPackage.create().resource(Resource.APP).action(Action.SUCCESS).data(responseData).send(webSocket);
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
    public void onMessage(AuthWebSocket ws, String msg) {
        if (msg.toLowerCase().contains("password"))
            logger.debug("Redacted message from (" + ws.getRemoteSocketAddress().getHostString() + ")");
        else
            logger.debug("Message from (" + ws.getRemoteSocketAddress().getHostString() + "): " + msg);

        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(msg);

        if (je.isJsonObject()) {

            JsonObject wsPacket = je.getAsJsonObject();
            JsonObject data = wsPacket.get("data").isJsonObject() ? wsPacket.getAsJsonObject("data") : null;

            Resource resource = Resource.valueOf(wsPacket.get("resource").getAsString().toUpperCase());
            Action action = Action.valueOf(wsPacket.get("action").getAsString().toUpperCase());

            switch (resource) {
                case APP:
                    handleApp(ws, action, data);
                    break;

                case PLAYER:
                    handlePlayer(ws, action, data);
                    break;

                case QUEUE:
                    handleQueue(ws, action, data);
                    break;

                case USER:
                    handleUser(ws, action, data);
                    break;

                case FAVORITES:
                    handleFavorites(ws, action, data);
                    break;

                case TRACK:
                    handleTrack(ws, action, data);

                default:
                    break;
            }

        } else {
            JsonObject data = new JsonObject();
            data.addProperty("message", "Json parse error");

            RequestSender.sendError(ws, Resource.APP, data);
        }
    }

    private void handleApp(AuthWebSocket webSocket, Action action, JsonObject data) {
        JsonObject responseData = new JsonObject();
        responseData.addProperty("action", action.toString());

        WsPackage.create().resource(Resource.APP).action(Action.SUCCESS).data(responseData).send(webSocket);
    }

    private void handlePlayer(AuthWebSocket ws, Action action, JsonObject data) {
        JsonObject responseData = new JsonObject();

        switch (action) {
            case GET:
                AudioPlayer ap = AudioManager.getPlayer();
                AudioTrack at = ap.getPlayingTrack();
                responseData.addProperty("status", ap.isPaused() ? "paused" : ap.getPlayingTrack() == null ? "stopped" : "playing");
                responseData.add("track", at != null ? (JsonObject) Main.GSON.toJsonTree(at, AudioTrack.class) : null);

                WsPackage.create().resource(Resource.PLAYER).action(Action.DATA).data(responseData).send(ws);
                break;

            case SET:
                AudioManager.getPlayer().setPaused(
                        // Note that also 'stopped' and totally invalid parameters will set it to playing, but I guess that's ok
                        data.get("state").getAsString().equals("pause")
                );

                responseData.addProperty("action", action.toString());
                WsPackage.create().resource(Resource.PLAYER).action(Action.SUCCESS).data(responseData).send(ws);
                break;
        }
    }

    private void handleQueue(AuthWebSocket ws, Action action, JsonObject data) {
        switch (action) {
            case GET:
                Type listType = new TypeToken<List<AudioTrack>>(){}.getType();
                JsonArray trackList = (JsonArray) Main.GSON.toJsonTree(AudioManager.getScheduler().getPlaylist().getSortedList(), listType);

                WsPackage.create().resource(Resource.QUEUE).action(Action.DATA).data(trackList).send(ws);
                break;

            case URI:
                if (!ws.isAuthorized()) {
                    handleUnauthorized(ws, Resource.QUEUE, action);
                    return;
                }

                String uri = data.get("uri").getAsString();
                AudioManager.loadAndPlay(uri, new BASSAudioResultHandler(ws));
                break;
        }
    }

    private void handleUser(AuthWebSocket ws, Action action, JsonObject data) {
        JsonObject responseData = new JsonObject();
        Resource resource = Resource.USER;

        switch (action) {
            case LOGIN:
                if (data.get("token") != null) {
                    String token = data.get("token").getAsString();
                    UserManager.login(ws, token);
                } else {
                    String username = data.get("username").getAsString();
                    String password = data.get("password").getAsString();
                    UserManager.login(ws, username, password);
                }
                break;

            case LOGOUT:
                UserManager.logout(ws, data.get("token").getAsString());
                break;

            case DELETE:
                if (!ws.isAuthorized() || !ws.getUser().isAdmin()) {
                    handleUnauthorized(ws, resource, action);
                    return;
                }

                if (data.get("userID") != null) {
                    UserManager.delete(ws, data.get("userID").getAsInt());
                }
                break;

            case REGISTER:
                if (!ws.isAuthorized() || !ws.getUser().isAdmin()) {
                    handleUnauthorized(ws, resource, action);
                    return;
                }

                if (data.get("username") != null && data.get("password") != null) {
                    UserManager.register(ws, data.get("username").getAsString(), data.get("password").getAsString());
                }

                break;

            case SETADMIN:
                if (!ws.isAuthorized() || !ws.getUser().isAdmin()) {
                    handleUnauthorized(ws, resource, action);
                    return;
                }

                if (data.get("username") != null && data.get("admin") != null) {
                    UserManager.setAdmin(ws, data.get("username").getAsString(), data.get("admin").getAsBoolean());
                }

                break;
        }
    }

    private void handleFavorites(AuthWebSocket ws, Action action, JsonObject data) {
        JsonObject responseData = new JsonObject();

        if (!ws.isAuthorized()) {
            handleUnauthorized(ws, Resource.FAVORITES, action);
            return;
        }

        switch (action) {
            case GET:
                Type listType = new TypeToken<List<FavoriteTrack>>(){}.getType();
                JsonArray ele = (JsonArray) Main.GSON.toJsonTree(UserManager.getFavorites(ws.getUser().getUserID()), listType);
                WsPackage.create().resource(Resource.FAVORITES).action(Action.DATA).data(ele).send(ws);
                break;

            case ADD:
                UserManager.addFavorite(ws.getUser().getUserID(), data.get("uri").getAsString(), data.get("title").getAsString());

                responseData.addProperty("action", action.toString());
                WsPackage.create().resource(Resource.FAVORITES).action(Action.SUCCESS).data(responseData).send(ws);
                break;

            case DELETE:
                UserManager.removeFavorite(ws.getUser().getUserID(), data.get("uri").getAsString());

                responseData.addProperty("action", action.toString());
                WsPackage.create().resource(Resource.FAVORITES).action(Action.SUCCESS).data(responseData).send(ws);
                break;

        }
    }

    private void handleTrack(AuthWebSocket ws, Action action, JsonObject data) {
        switch (action) {
            case VOTE:
                if (!ws.isAuthorized()) {
                    handleUnauthorized(ws, Resource.TRACK, action);
                    return;
                }

                String userID = ws.getUser().getUserID().toString();
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
        }
    }

    // TODO add to error handler
    private void handleUnauthorized(AuthWebSocket webSocket, Resource resource, Action action) {
        JsonObject data = new JsonObject();
        data.addProperty("action", action.toString());
        data.addProperty("message", "Your connection is unauthorized. Log in or upgrade to admin to perform this action.");

        WsPackage.create().resource(resource).action(Action.ERROR).data(data).send(webSocket);
    }

    public void stopSocket() {
        // Inform connections about stopping the playback
        JsonObject data = new JsonObject();
        data.addProperty("state", "stopped");
        WsPackage.create().resource(Resource.PLAYER).action(Action.DATA).data(data).broadcast();

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
