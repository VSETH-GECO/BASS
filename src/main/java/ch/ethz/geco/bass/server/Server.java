package ch.ethz.geco.bass.server;

import com.google.gson.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;


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
            Method method = Method.valueOf(jo.getAsJsonObject("method").getAsString());

            switch (method) {
                case retrieve:
                    handleRetrieve(webSocket, jo);
                    break;
                case update:
                    handleUpdate(webSocket, jo);
                    break;
                case create:
                    handleCreate(webSocket, jo);
                    break;
                case delete:
                    handleDelete(webSocket, jo);
                    break;
                case flush:
                    handleFlush(webSocket, jo);
                    break;

                default:
                    break;
            }

            JsonObject data = jo.getAsJsonObject("data");


        } else {
            JsonObject jo = new JsonObject();
            jo.addProperty("method", "flush");
            jo.addProperty("type", "error");
            jo.addProperty("message", "Json parse error.");

            webSocket.send(gson.toJson(jo));
        }
    }

    private void handleRetrieve(WebSocket webSocket, JsonObject jo) {
        String type = jo.getAsJsonObject("type").getAsString();

        switch (type) {
            case "queue/all":
                break;
            case "player/all":
                break;
        }
    }

    private void handleUpdate(WebSocket webSocket, JsonObject jo) {
        String type = jo.getAsJsonObject("type").getAsString();

        switch (type) {
            case "queue/track/vote":
                break;
            case "player/control/play":
                break;
            case "player/control/pause":
                break;
        }
    }

    private void handleCreate(WebSocket webSocket, JsonObject jo) {
        String type = jo.getAsJsonObject("type").getAsString();
        String response;

        switch (type) {
            case "queue/uri":
                break;
        }
    }

    private void handleDelete(WebSocket webSocket, JsonObject jo) {
    }

    private void handleFlush(WebSocket webSocket, JsonObject jo) {
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
