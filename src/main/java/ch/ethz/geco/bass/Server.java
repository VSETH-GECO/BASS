package ch.ethz.geco.bass;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Server class
 * <p>
 * Somewhen in the future it should handle all kinds of api
 * requests to modify the queue. Maybe even provide a web-
 * interface.
 */
public class Server extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

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
    public void onMessage(WebSocket webSocket, String msg) {
        logger.debug("Message from (" + webSocket.getRemoteSocketAddress().getHostString() + "): " + msg);
        String[] args = msg.split(",");
        switch (args[0]) {
            case "GET":
                handleGet(webSocket);
                break;
            case "POST":
                handlePost(webSocket, args[1]);
                break;
            default:
        }
    }

    private void handlePost(WebSocket webSocket, String arg) {
    }

    private void handleGet(WebSocket webSocket) {
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        logger.error(e.getMessage());
    }

    @Override
    public void onStart() {
        logger.info("WS Server started!");
    }

    // TODO maybe useful
    /*
    public void broadcast(String text) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                c.send(text);
            }
        }
    }*/
}
