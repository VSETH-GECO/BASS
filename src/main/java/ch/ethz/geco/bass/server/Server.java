package ch.ethz.geco.bass.server;

import ch.ethz.geco.bass.audio.AudioManager;
import ch.ethz.geco.bass.audio.AudioTrackMetaData;
import ch.ethz.geco.bass.audio.gson.AudioTrackSerializer;
import ch.ethz.geco.bass.audio.handle.BASSAudioResultHandler;
import com.google.gson.*;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;

/**
 * Server class
 * <p>
 * Somewhen in the future it should handle all kinds of api
 * requests to modify the queue. Maybe even provide a web-
 * interface.
 */
public class Server extends WebSocketServer {
    enum Method {get, post, patch, delete}

    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(AudioTrack.class, new AudioTrackSerializer())
            .setPrettyPrinting()
            .create();

    public Server(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        logger.info(webSocket.getRemoteSocketAddress().getHostString() + " connected!");

        JsonObject jo = new JsonObject();
        jo.addProperty("method", "post");
        jo.addProperty("type", "app/welcome");
        jo.add("data", JsonNull.INSTANCE);

        webSocket.send(jo.toString());
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        logger.info(webSocket.getRemoteSocketAddress().getHostString() + " disconnected!");
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        logger.error(e.getMessage());
    }

    @Override
    public void onStart() {
        logger.info("WS Server started!");
    }

    @Override
    public void onMessage(WebSocket webSocket, String msg) {
        logger.debug("Message from (" + webSocket.getRemoteSocketAddress().getHostString() + "): " + msg);

        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(msg);

        if (je.isJsonObject()) {
            JsonObject jo = je.getAsJsonObject();
            Method method = Method.valueOf(jo.get("method").getAsString());
            String type = jo.get("type").getAsString();

            switch (method) {
                case get:
                    handleGet(webSocket, type, jo);
                    break;
                case post:
                    handlePost(webSocket, type, jo);
                    break;
                case patch:
                    handlePatch(webSocket, type, jo);
                    break;
                case delete:
                    handleDelete(webSocket, type, jo);
                    break;

                default:
                    break;
            }

        } else {
            JsonObject jo = new JsonObject();
            jo.addProperty("method", "post");
            jo.addProperty("type", "error");
            jo.addProperty("message", "Json parse error.");

            webSocket.send(gson.toJson(jo));
        }
    }

    private void handleGet(WebSocket webSocket, String type, JsonObject jo) {
        JsonObject data = new JsonObject();
        JsonObject response = new JsonObject();

        switch (type) {
            case "queue/all":
                JsonArray trackList = (JsonArray) gson.toJsonTree(AudioManager.getScheduler().getPlaylist());

                response.addProperty("method", "post");
                response.addProperty("type", "queue/all");
                response.add("data", trackList);
                webSocket.send(response.toString());

                break;

            case "player/current":
                AudioTrack at = AudioManager.getPlayer().getPlayingTrack();
                data.addProperty("id", -1);
                data.addProperty("title", at.getInfo().title);
                data.addProperty("votes", ((AudioTrackMetaData) at.getUserData()).getVoteCount());
                data.addProperty("userID", ((AudioTrackMetaData) at.getUserData()).getUserID());

                response.addProperty("method", "post");
                response.addProperty("type", "player/current");
                response.add("data", data);
                webSocket.send(response.toString());

                break;
        }
    }

    private void handlePatch(WebSocket webSocket, String type, JsonObject jo) {

        switch (type) {
            case "track/vote":
                String userID = jo.getAsJsonObject("data").get("userID").getAsString();
                Byte vote = jo.getAsJsonObject("data").get("vote").getAsByte();
                int id = jo.getAsJsonObject("data").get("id").getAsInt();

                Map<String, Byte> votes;
                /*if (id == 0) {
                    votes = ((AudioTrackMetaData) AudioManager.getPlayer().getPlayingTrack().getUserData()).getVotes();
                } else {
                    votes = ((AudioTrackMetaData) AudioManager.getScheduler().getPlaylist().get(id).getUserData()).getVotes();
                }*/
                votes = ((AudioTrackMetaData) AudioManager.getScheduler().getPlaylist().get(id).getUserData()).getVotes();


                if (votes.containsKey(userID))
                    votes.replace(userID, vote);
                else
                    votes.put(userID, vote);

                break;
        }
    }

    private void handlePost(WebSocket webSocket, String type, JsonObject jo) {

        switch (type) {
            case "queue/uri":
                String uri = jo.getAsJsonObject("data").get("uri").getAsString();
                AudioManager.loadAndPlay(uri, new BASSAudioResultHandler(webSocket, jo.getAsJsonObject("data")));
                break;

            case "player/control/play":
                AudioManager.getPlayer().setPaused(false);
                break;

            case "player/control/pause":
                AudioManager.getPlayer().setPaused(true);
                break;
        }
    }

    private void handleDelete(WebSocket webSocket, String type, JsonObject jo) {
    }


    public void broadcast(String text) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                c.send(text);
            }
        }
    }

    public void broadcast(JsonObject jo) {
        broadcast(jo.toString());
    }
}
