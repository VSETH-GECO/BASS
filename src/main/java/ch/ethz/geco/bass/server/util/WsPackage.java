package ch.ethz.geco.bass.server.util;

import ch.ethz.geco.bass.Main;
import ch.ethz.geco.bass.server.AuthWebSocket;
import ch.ethz.geco.bass.server.Server.Action;
import ch.ethz.geco.bass.server.Server.Resource;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.istack.internal.NotNull;
import jdk.internal.jline.internal.Nullable;

public class WsPackage {
    private Resource resource;
    private Action action;
    private JsonElement data;
    private JsonObject dynamicData;

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
     * Only use this if data is going to be a single JsonElement.
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

    /**
     * Set the resource property of the package
     *
     * @param resource to be set
     * @return this object for chaining
     */
    public WsPackage resource(Resource resource) {
        this.resource = resource;

        return this;
    }

    /**
     * Set the action property of the package
     *
     * @param action to be set
     * @return this object for chaining
     */
    public WsPackage action(Action action) {
        this.action = action;

        return this;
    }

    /**
     * Set the data property of the package. Only use this if the
     * data is going to be a single JsonElement. For multiple elements use WsPackage#addData.
     * @param data to be set
     * @return this object for chaining
     */
    public WsPackage data(JsonElement data) {
        this.data = data;

        return this;
    }

    /**
     * Add a element to the data of the package
     *
     * @param property the name of the property
     * @param value    of the property
     * @return this object for chaining
     */
    @NotNull
    public WsPackage addData(String property, String value) {
        if (dynamicData == null) {
            dynamicData = new JsonObject();
        }

        dynamicData.addProperty(property, value);

        return this;
    }

    /**
     * Add a element to the data of the package
     *
     * @param property the name of the property
     * @param value    of the property
     * @return this object for chaining
     */
    @NotNull
    public WsPackage addData(String property, Number value) {
        if (dynamicData == null) {
            dynamicData = new JsonObject();
        }

        dynamicData.addProperty(property, value);

        return this;
    }

    /**
     * Add a element to the data of the package
     *
     * @param property the name of the property
     * @param value    of the property
     * @return this object for chaining
     */
    public WsPackage addData(String property, Boolean value) {
        if (dynamicData == null) {
            dynamicData = new JsonObject();
        }

        dynamicData.addProperty(property, value);

        return this;
    }

    @Nullable
    public WsPackage addDataElement(String property, JsonElement value) {
        if (dynamicData == null) {
            dynamicData = new JsonObject();
        }

        dynamicData.add(property, value);

        return this;
    }

    private JsonObject makeJson() {
        JsonObject paeckli = new JsonObject();

        paeckli.addProperty("resource", resource.toString().toLowerCase());
        paeckli.addProperty("action", action.toString().toLowerCase());

        if (data != null && dynamicData == null)
            paeckli.add("data", data);

        if (data == null && dynamicData != null)
            paeckli.add("data", dynamicData);

        if (data == null && dynamicData == null)
            paeckli.add("data", null);

        if (data != null && dynamicData != null) {
            dynamicData.add("data", data);
            paeckli.add("data", dynamicData);
        }

        return paeckli;
    }

    /**
     * Sends this package to all open websocket connections
     * @throws NullPointerException if resource or action are not set.
     */
    public void broadcast() {
        Main.server.broadcast(makeJson());
    }

    /**
     * Sends this package to the given connection
     * @param webSocket to be contacted
     * @throws NullPointerException if resource or action are not set.
     */
    public void send(AuthWebSocket webSocket) {
        webSocket.send(makeJson().toString());
    }

    @Override
    public String toString() {
        return makeJson().toString();
    }
}
