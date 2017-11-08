package ch.ethz.geco.bass.server.util;

import ch.ethz.geco.bass.Main;
import ch.ethz.geco.bass.server.Server.Action;
import ch.ethz.geco.bass.server.Server.Resource;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;

public class WsPackage {
    private Resource resource;
    private Action action;
    private JsonElement data;

    public static WsPackage create() {
        return new WsPackage();
    }

    public static WsPackage create(Resource resource, Action action) {
        return new WsPackage(resource, action);
    }

    private WsPackage() {}
    private WsPackage(Resource resource, Action action) {
        this.resource = resource;
        this.action = action;
    }

    public WsPackage resource(Resource resource) {
        this.resource = resource;

        return this;
    }

    public WsPackage action(Action action) {
        this.action = action;

        return this;
    }

    public WsPackage data(JsonElement data) {
        this.data = data;

        return this;
    }

    private JsonObject makeJson() {
        JsonObject paeckli = new JsonObject();

        paeckli.addProperty("resource", resource.toString().toLowerCase());
        paeckli.addProperty("action", action.toString().toLowerCase());
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
