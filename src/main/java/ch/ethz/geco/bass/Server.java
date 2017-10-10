package ch.ethz.geco.bass;

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
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private Player player;

    Server(Player player, int port) {
        super(new InetSocketAddress(port));
        this.player = player;
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
        switch (args[0].trim()) {
            case "GET":
                handleGet(webSocket, args.length > 1 ? args[1] : null);
                break;
            case "POST":
                handlePost(webSocket, args[1].trim());
                break;
            default:
        }
    }

    private void handlePost(WebSocket webSocket, String arg) {
        logger.debug(arg);
        String response;
        switch (arg.toLowerCase()) {
            case "play": {
                if(player.resume())
                    response = "Playback resumed";
                else
                    response = "Nothing to resume";
                break;
            }

            case "pause": {
                logger.debug("was here");
                if (player.pause())
                    response = "Playback paused";
                else
                    response = "Nothing is playing";
                break;
            }

            case "next": {
                if (player.nextTrack())
                    response = "Playing next track";
                else
                    response = "No next track available";
                break;
            }

            case "stop": {
                if(player.stop())
                    response = "Playback stopped";
                else
                    response = "Nothing to stop";
                break;
            }

            default: {   // Default would be checking for a valid url
                logger.debug("Somehow managed to get into this branch");
                if (player.add(arg))
                    response = "Request accepted";
                else
                    response = "URL invalid";

                player.update();
            }
        }



        webSocket.send(response);

        if (response.equals("Request accepted"))
            broadcast("New track added: " + new YoutubeDL().getVideoTitle(arg));
    }

    private void handleGet(WebSocket webSocket,String arg) {
        String response;

        if(arg != null && arg.equalsIgnoreCase("help")) {
            response = "POST:\n pause\n play\n next\n stop\n a url\nGET:\n\nhelp";
        } else {
            response = "Nothing playing";
            if (player.getCurrent() != null)
                response = "Current track: " + player.getCurrent().title +
                        "\nDuration: " + player.getPosition() + "/" + player.getCurrent().duration;

            if (player.getNext() != null)
                response += "\nNext track: " + player.getNext().title +
                        "\nDuration: " + player.getNext().duration;
        }

        webSocket.send(response);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        logger.error(e.getMessage());
    }

    @Override
    public void onStart() {
        logger.info("WS Server started!");
    }

    void broadcast(String text) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                c.send(text);
            }
        }
    }
}
