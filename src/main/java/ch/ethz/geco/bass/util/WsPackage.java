package ch.ethz.geco.bass.util;

import ch.ethz.geco.bass.Main;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;

public class WsPackage {
    private String method;
    private String type;
    private JsonElement data;

    public static WsPackage create() {
        return new WsPackage();
    }

    public WsPackage method(String method) {
        this.method = method;

        return this;
    }

    public WsPackage type(String type) {
        this.type = type;

        return this;
    }

    public WsPackage data(JsonElement data) {
        this.data = data;

        return this;
    }

    private JsonObject makeJson() {
        JsonObject paeckli = new JsonObject();

        paeckli.addProperty("method", method);
        paeckli.addProperty("type", type);
        paeckli.add("data", data);

        return paeckli;
    }

    public void broadcast() {
        Main.server.broadcast(makeJson());
    }

    public void send(WebSocket webSocket) {
        webSocket.send(makeJson().toString());
    }
}
