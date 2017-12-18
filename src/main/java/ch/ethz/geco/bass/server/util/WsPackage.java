package ch.ethz.geco.bass.server.util;

import ch.ethz.geco.bass.Main;
import ch.ethz.geco.bass.server.AuthWebSocket;
import ch.ethz.geco.bass.server.Server.Action;
import ch.ethz.geco.bass.server.Server.Resource;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class WsPackage {
    private Resource resource;
    private Action action;
    private JsonElement data;

    /**
     * Create a new and empty WebSocketPackage object. Cannot be send without resource and action set!
     *
     * @return new WebSocketPackage object
     */
    public static WsPackage create() {
        return new WsPackage();
    }

    /**
     * Create a new WebSocketPackage object with the given resource and action.
     *
     * @param resource of the package
     * @param action of the package
     * @return new WebSocketPackage object
     */
    public static WsPackage create(Resource resource, Action action) {
        return new WsPackage(resource, action);
    }

    /**
     * Create a new WebSocketPackage object with the given resource, action and data.
     *
     * @param resource of the package
     * @param action of the package
     * @param data for the package
     * @return new WebSocketPackage object
     */
    public static WsPackage create(Resource resource, Action action, JsonObject data) {
        return new WsPackage(resource, action, data);
    }

    private WsPackage() {}
    private WsPackage(Resource resource, Action action) {
        this.resource = resource;
        this.action = action;
    }

    private WsPackage(Resource resource, Action action, JsonObject data) {
        this.resource = resource;
        this.action = action;
        this.data = data;
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

    public void send(AuthWebSocket webSocket) {
        webSocket.send(makeJson().toString());
    }

    @Override
    public String toString() {
        return makeJson().toString();
    }
}
