package ch.ethz.geco.bass.server;

import ch.ethz.geco.bass.Main;
import ch.ethz.geco.bass.audio.AudioManager;
import ch.ethz.geco.bass.audio.handle.BASSAudioResultHandler;
import ch.ethz.geco.bass.server.auth.User;
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
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;

/**
 * Server class
 */
public class Server extends AuthWebSocketServer {
    private static final String API_VERSION = "v2";
    public enum Resource {APP, PLAYER, QUEUE, USER, FAVORITES, TRACK}

    public enum Action {
        GET, SET, ADD, DELETE, LOGIN, LOGOUT, INFORM, VOTE, SUCCESS, ERROR, DATA, UPDATE;

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
        logger.info(clientHandshake.getFieldValue(webSocket.getRemoteSocketAddress().getHostString() + " connected!"));

        JsonObject responseData = new JsonObject();
        responseData.addProperty("apiVersion", API_VERSION);
        WsPackage.create().resource(Resource.APP).action(Action.SUCCESS).data(responseData).send(webSocket);
    }

    @Override
    public void onClose(AuthWebSocket webSocket, int i, String s, boolean b) {
        if (webSocket != null) {
            if (webSocket.getRemoteSocketAddress() != null) {
                logger.info(webSocket.getRemoteSocketAddress().getHostString() + " disconnected!");
            } else {
                logger.warn("Connection without address disconnected!");
            }

            if (webSocket.getUser() != null) {
                VoteHandler.scheduleExpiry(webSocket.getUser().getUserID());
            }
        } else {
            logger.warn("Websocket was null on disconnect!");
        }
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

        switch (action) {
            case INFORM:
                responseData.addProperty("action", action.toString());

                WsPackage.create().resource(Resource.APP).action(Action.SUCCESS).data(responseData).send(webSocket);
                break;

            case UPDATE:
                if (webSocket.isAuthorized() && webSocket.getUser().isAdmin()) {
                    String branch = data.get("branch").isJsonObject() ? data.getAsJsonObject("branch").getAsString() : "dev";
                    BufferedInputStream inStream;
                    FileOutputStream outStream;
                    try {
                        // Update on progress
                        responseData.addProperty("status", "Downloading new jar...");
                        WsPackage.create().resource(Resource.APP).action(Action.UPDATE).data(responseData).send(webSocket);

                        // Download stuff
                        URL fileUrlObj=new URL("https://jenkins.stammgruppe.eu/job/BASS/job/" + branch + "/lastSuccessfulBuild/artifact/target/BASS-shaded.jar");
                        inStream = new BufferedInputStream(fileUrlObj.openStream());
                        outStream = new FileOutputStream("./bass.jar");

                        byte fileData[] = new byte[1024];
                        int count;
                        while ((count = inStream.read(fileData, 0, 1024)) != -1) {
                            outStream.write(fileData, 0, count);
                        }
                        inStream.close();
                        outStream.close();

                        // Update on restart
                        responseData = new JsonObject();
                        responseData.addProperty("status", "Download finished, restarting...");
                        WsPackage.create().resource(Resource.APP).action(Action.UPDATE).data(responseData).send(webSocket);

                        this.stopSocket();
                        System.exit(8);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        }
    }

    private void handlePlayer(AuthWebSocket ws, Action action, JsonObject data) {
        JsonObject responseData = new JsonObject();

        switch (action) {
            case GET:
                RequestSender.sendPlayerState(ws);
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

            case ADD:
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

            case ADD:
                if (!ws.isAuthorized() || !ws.getUser().isAdmin()) {
                    handleUnauthorized(ws, resource, action);
                    return;
                }

                if (data.get("username") != null && data.get("password") != null) {
                    UserManager.register(ws, data.get("username").getAsString(), data.get("password").getAsString());
                }

                break;

            case UPDATE:

                if (data.get("userID") != null && ws.isAuthorized()) {
                    int updatedRows = 0;
                    int userID = data.get("userID").getAsInt();

                    if (data.get("password") != null && (ws.getUser().getUserID() == userID || ws.getUser().isAdmin()))
                        updatedRows += UserManager.setPassword(ws, data.get("userID").getAsInt(), data.get("password").getAsString());

                    if (data.get("admin") != null && ws.getUser().isAdmin())
                        updatedRows += UserManager.setAdmin(ws, data.get("userID").getAsInt(), data.get("admin").getAsBoolean());

                    if (data.get("name") != null && ws.getUser().isAdmin())
                        updatedRows += UserManager.setUsername(ws, data.get("userID").getAsInt(), data.get("name").getAsString());

                    if (updatedRows == 0) {
                        responseData = new JsonObject();
                        responseData.addProperty("message", "User with that ID was not found. Nothing changed.");
                        RequestSender.sendError(ws, Resource.USER, responseData);
                    } else {
                        Type listType = new TypeToken<List<User>>(){}.getType();
                        JsonArray userList = (JsonArray) Main.GSON.toJsonTree(UserManager.getUsers(), listType);
                        WsPackage.create().resource(Resource.USER).action(Action.INFORM).data(userList).send(ws);
                    }
                }

                break;

            case GET:
                if (!ws.isAuthorized() || !ws.getUser().isAdmin()) {
                    handleUnauthorized(ws, resource, action);
                    return;
                }

                Type listType = new TypeToken<List<User>>(){}.getType();
                JsonArray userList = (JsonArray) Main.GSON.toJsonTree(UserManager.getUsers(), listType);
                WsPackage.create().resource(Resource.USER).action(Action.INFORM).data(userList).send(ws);
        }
    }

    private void handleFavorites(AuthWebSocket ws, Action action, JsonObject data) {
        JsonObject responseData = new JsonObject();
        Type listType = new TypeToken<List<FavoriteTrack>>(){}.getType();
        JsonArray ele;

        if (!ws.isAuthorized()) {
            handleUnauthorized(ws, Resource.FAVORITES, action);
            return;
        }

        switch (action) {
            case GET:
                ele = (JsonArray) Main.GSON.toJsonTree(UserManager.getFavorites(ws.getUser().getUserID()), listType);
                WsPackage.create().resource(Resource.FAVORITES).action(Action.DATA).data(ele).send(ws);
                break;

            case ADD:
                UserManager.addFavorite(ws.getUser().getUserID(), data.get("uri").getAsString(), data.get("title").getAsString());

                responseData.addProperty("action", action.toString());
                WsPackage.create().resource(Resource.FAVORITES).action(Action.SUCCESS).data(responseData).send(ws);

                ele = (JsonArray) Main.GSON.toJsonTree(UserManager.getFavorites(ws.getUser().getUserID()), listType);
                WsPackage.create().resource(Resource.FAVORITES).action(Action.DATA).data(ele).send(ws);
                break;

            case DELETE:
                UserManager.removeFavorite(ws.getUser().getUserID(), data.get("uri").getAsString());

                responseData.addProperty("action", action.toString());
                WsPackage.create().resource(Resource.FAVORITES).action(Action.SUCCESS).data(responseData).send(ws);

                ele = (JsonArray) Main.GSON.toJsonTree(UserManager.getFavorites(ws.getUser().getUserID()), listType);
                WsPackage.create().resource(Resource.FAVORITES).action(Action.DATA).data(ele).send(ws);
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

                Byte vote = data.get("vote").getAsByte();
                int trackID = data.get("id").getAsInt();

                VoteHandler.handle(ws, trackID, vote);
                break;
        }
    }

    // TODO add to error handler
    private void handleUnauthorized(AuthWebSocket webSocket, Resource resource, Action action) {
        JsonObject data = new JsonObject();
        data.addProperty("action", action.toString());
        data.addProperty("message", "Your connection is unauthorized. Log in or upgrade to admin for only 9.99 to perform this action.");

        WsPackage.create().resource(resource).action(Action.ERROR).data(data).send(webSocket);
    }

    public void stopSocket() {
        // Inform connections about stopping the playback
        JsonObject data = new JsonObject();
        data.addProperty("state", "stopped");
        WsPackage.create().resource(Resource.PLAYER).action(Action.DATA).data(data).broadcast();

        // Shutdown socket to free port
        try {
            this.stop(100000);
        } catch (InterruptedException e) {
            ErrorHandler.handleLocal(e);
        }
    }

    public void broadcast(JsonObject jo) {
        broadcast(jo.toString());
    }
}
