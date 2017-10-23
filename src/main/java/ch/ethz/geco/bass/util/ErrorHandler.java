package ch.ethz.geco.bass.util;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides useful functions for error handling.
 */
public class ErrorHandler {
    /**
     * The logger of this class
     */
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    /**
     * Reports the error in a more compact form into stderr.
     *
     * @param e the error to handle
     */
    public static void handleLocal(Throwable e) {
        StackTraceElement[] stackTraceElements = e.getStackTrace();

        List<StackTraceElement> bassTrace = new ArrayList<>();
        List<StackTraceElement> lavaPlayerTrace = new ArrayList<>();
        List<StackTraceElement> javaTrace = new ArrayList<>();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            if (stackTraceElement.getClassName().startsWith("ch.ethz.geco.bass.")) {
                bassTrace.add(stackTraceElement);
            } else if (stackTraceElement.getClassName().startsWith("com.sedmelluq.discord.lavaplayer.")) {
                lavaPlayerTrace.add(stackTraceElement);
            } else {
                javaTrace.add(stackTraceElement);
            }
        }

        StringBuilder builder = new StringBuilder();

        List<StackTraceElement> listToUse;
        if (bassTrace.size() > 0) {
            listToUse = bassTrace;
        } else if (lavaPlayerTrace.size() > 0) {
            listToUse = lavaPlayerTrace;
        } else {
            listToUse = javaTrace;
        }

        for (int i = 0; i < listToUse.size(); i++) {
            StackTraceElement traceElement = listToUse.get(i);
            String[] packagePath = traceElement.getClassName().split("\\.");
            builder.append("        at ").append(packagePath[packagePath.length - 1]).append(".").append(traceElement.getMethodName())
                    .append("(").append(traceElement.getFileName()).append(":").append(traceElement.getLineNumber()).append(")");

            if (i < listToUse.size() - 1) {
                builder.append("\n");
            }
        }

        logger.error(e.getClass().getSimpleName() + ": " + e.getMessage());
        System.err.println(builder.toString());
    }

    /**
     * Sends the error to the given web socket. This will also trigger a local handle.
     *
     * @param e  the error to handle
     * @param ws the web socket to send the error to
     */
    public static void handleRemote(Throwable e, WebSocket ws) {
        // TODO: Format the error and send it
        //ws.send(<error json>);

        handleLocal(e);
    }

    /**
     * Sends the error to all connected clients on the web socket.
     *
     * @param e the error to handle
     */
    public static void broadcast(Throwable e) {
        // TODO: Format error and broadcast it
        //Main.server.broadcast(<error json>);

        handleLocal(e);
    }
}
