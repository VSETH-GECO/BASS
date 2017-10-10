package ch.ethz.geco.bass;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

/**
 * Server class
 *
 * Somewhen in the future it should handle all kinds of api
 * requests to modify the queue. Maybe even provide a web-
 * interface.
 */
public class Server extends WebSocketServer {
    Player player;

    public Server(Player player, int port) {
        super(new InetSocketAddress(port));
        this.player = player;
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        webSocket.send("Welcome to BASS");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
    }

    @Override
    public void onMessage(WebSocket webSocket, String msg) {
        String[] args = msg.split(",");
        switch(args[0]) {
            case "GET": handleGet(webSocket); break;
            case "POST": handlePost(webSocket, args[1]); break;
            default:
        }
    }

    private void handlePost(WebSocket webSocket, String arg) {
        if (arg.equalsIgnoreCase("play")) {
            player.resume();
        } else if (arg.equalsIgnoreCase("pause")) {
            player.pause();
        }

        String response;
        System.out.println(arg);
        if (player.add(arg.trim()))
            response = "Request accepted";
        else
            response = "URL invalid";

        player.update();

        webSocket.send(response);
    }

    private void handleGet(WebSocket webSocket) {
        String response = "Nothing playing";
        if (player.getCurrent() != null)
            response = "Current song: " + player.getCurrent().title + "\nDuration: " + player.getCurrent().duration;
        if (player.getNext() != null)
            response += "\nNext song: " + player.getNext().title + "\nDuration: " + player.getNext().duration;

        webSocket.send(response);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {

    }

    @Override
    public void onStart() {

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
