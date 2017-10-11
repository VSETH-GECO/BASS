package ch.ethz.geco.bass.server;

/**
 * Archetype for all packages that get sent over
 * the websocket. It includes method, status and
 * a arbitrary data.
 */
public class WSPackage {
    enum Method {create, retrieve, update, UPDATE}

    Method method;
    int status;
    Object data;
}
