package ch.ethz.geco.bass.server;

import ch.ethz.geco.bass.audio.AudioManager;
import ch.ethz.geco.bass.audio.AudioTrackMetaData;
import ch.ethz.geco.bass.audio.handle.BASSAudioResultHandler;
import com.google.gson.*;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import javafx.util.Pair;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
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
    enum Method {create, retrieve, update, delete, flush}

    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public Server(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        logger.info(webSocket.getRemoteSocketAddress().getHostString() + " connected!");
        //webSocket.send("Welcome to BASS");
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
                case retrieve:
                    handleRetrieve(webSocket, type, jo);
                    break;
                case update:
                    handleUpdate(webSocket, type, jo);
                    break;
                case create:
                    handleCreate(webSocket, type, jo);
                    break;
                case delete:
                    handleDelete(webSocket, type, jo);
                    break;
                case flush:
                    handleFlush(webSocket, type, jo);
                    break;

                default:
                    break;
            }

        } else {
            JsonObject jo = new JsonObject();
            jo.addProperty("method", "flush");
            jo.addProperty("type", "error");
            jo.addProperty("message", "Json parse error.");

            webSocket.send(gson.toJson(jo));
        }
    }

    private void handleRetrieve(WebSocket webSocket, String type, JsonObject jo) {
        JsonObject data = new JsonObject();
        JsonObject response = new JsonObject();
        JsonArray data1 = new JsonArray();

        switch (type) {
            case "queue/all":
                for (Map.Entry<Integer, AudioTrack> at : AudioManager.getScheduler().getPlaylist().entrySet()) {
                    JsonObject track = new JsonObject();
                    track.addProperty("id", at.getKey());
                    track.addProperty("title", at.getValue().getInfo().title);
                    track.addProperty("votes", ((AudioTrackMetaData) at.getValue().getUserData()).getVoteCount());
                    track.addProperty("userID", ((AudioTrackMetaData) at.getValue().getUserData()).getUserID());

                    data1.add(track);
                }

                response.addProperty("method", "update");
                response.addProperty("type", "queue/all");
                response.add("data", data1);
                webSocket.send(response.toString());

                break;

            case "player/current":
                AudioTrack at = AudioManager.getPlayer().getPlayingTrack();
                data.addProperty("id", 0);
                data.addProperty("title", at.getInfo().title);
                data.addProperty("votes", ((AudioTrackMetaData) at.getUserData()).getVoteCount());
                data.addProperty("userID", ((AudioTrackMetaData) at.getUserData()).getUserID());

                response.addProperty("method", "update");
                response.addProperty("type", "player/current");
                response.add("data", data);
                webSocket.send(response.toString());

                break;
        }
    }

    private void handleUpdate(WebSocket webSocket, String type, JsonObject jo) {

        switch (type) {
            case "queue/track/vote":
                String userID = jo.getAsJsonObject("data").get("userID").getAsString();
                Byte vote = jo.getAsJsonObject("data").get("vote").getAsByte();
                int id = jo.getAsJsonObject("data").get("id").getAsInt();

                Map<String, Byte> votes;
                if (id == 0) {
                    votes = ((AudioTrackMetaData) AudioManager.getPlayer().getPlayingTrack().getUserData()).getVotes();
                } else {
                    votes = ((AudioTrackMetaData) AudioManager.getScheduler().getPlaylist().get(id).getUserData()).getVotes();
                }


                if (votes.containsKey(userID))
                    votes.replace(userID, vote);
                else
                    votes.put(userID, vote);

                break;

            case "player/control/play":
                AudioManager.getPlayer().setPaused(false);
                break;

            case "player/control/pause":
                AudioManager.getPlayer().setPaused(true);
                break;
        }
    }

    private void handleCreate(WebSocket webSocket, String type, JsonObject jo) {

        switch (type) {
            case "queue/uri":
                String uri = jo.getAsJsonObject("data").get("uri").getAsString();
                AudioManager.loadAndPlay(uri, new BASSAudioResultHandler(webSocket, jo.getAsJsonObject("data")));
                break;
        }
    }

    private void handleDelete(WebSocket webSocket, String type, JsonObject jo) {
    }

    private void handleFlush(WebSocket webSocket, String type, JsonObject jo) {
    }


    public void broadcast(String text) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                c.send(text);
            }
        }
    }
}
