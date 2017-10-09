package ch.ethz.geco.bass;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static ch.ethz.geco.bass.Main.player;

/**
 * Server class
 *
 * Somewhen in the future it should handle all kinds of api
 * requests to modify the queue. Maybe even provide a web-
 * interface.
 */
public class Server implements HttpHandler {

    Server() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/music", this);
        server.start();
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        switch (t.getRequestMethod()) {
            case "GET": handleGet(t);
            case "PUT": break;
            case "POST": handlePost(t);
            case "DELETE": break;
        }
    }

    private void handlePost(HttpExchange t) throws IOException {
        // Read the request
        BufferedReader br = new BufferedReader(new InputStreamReader(t.getRequestBody()));
        StringBuilder bodyBuilder = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null)
            bodyBuilder.append(line);

        String body = bodyBuilder.toString();
        System.out.println("request received");

        String response;
        if (player.add(body))
            response = "Request accepted";
        else
            response = "URL invalid";


        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();

        player.update();
    }

    private void handleGet(HttpExchange t) throws IOException {
        String response = "Nothing playing";
        if (player.getCurrent() != null)
            response = "Current song: " + player.getCurrent().title + "\nDuration: " + player.getCurrent().duration;
        if (player.getNext() != null)
            response += "\nNext song: " + player.getNext().title + "\nDuration: " + player.getNext().duration;

        System.out.println(response);

        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
