package ch.ethz.geco.bass.server;

import com.google.gson.*;
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
        webSocket.send("Welcome to BASS");
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

        switch (type) {
            case "queue/all":
                System.out.println("I should now reply with the playlist");
                break;
            case "player/current":
                System.out.println("i should now reply with the current track");
                break;
        }
    }

    private void handleUpdate(WebSocket webSocket, String type, JsonObject jo) {

        switch (type) {
            case "queue/track/vote":
                break;
            case "player/control/play":
                break;
            case "player/control/pause":
                break;
        }
    }

    private void handleCreate(WebSocket webSocket, String type, JsonObject jo) {

        switch (type) {
            case "queue/uri":
                String uri = jo.getAsJsonObject("data").get("uri").getAsString();
                System.out.println(uri);

                JsonObject response = new JsonObject();
                response.addProperty("method", "flush");
                response.addProperty("type", "ack");
                response.add("data", JsonNull.INSTANCE);
                webSocket.send(response.toString());
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
