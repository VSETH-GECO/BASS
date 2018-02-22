package ch.ethz.geco.bass.server;

import ch.ethz.geco.bass.server.auth.User;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.WebSocketListener;
import org.java_websocket.drafts.Draft;

import java.util.List;

/**
 * This web socket extends the default web socket by authorization checking.
 * It saves the user who uses this web socket and if the user is authorized (logged in).
 */
@SuppressWarnings("JavaDoc")
public class AuthWebSocket extends WebSocketImpl {
    /**
     * The user who uses this web socket.
     * If the user is null, this means that the user who uses this connection did not yet log in.
     */
    private User authorizedUser = null;

    /**
     * If this connection is authorized (logged in with a user account).
     */
    private boolean isAuthorized = false;

    /**
     * @see org.java_websocket.server.WebSocketServer.WebSocketWorker
     **/
    public volatile AuthWebSocketServer.WebSocketWorker workerThread;

    /**
     * @see WebSocketImpl#WebSocketImpl(WebSocketListener, List)
     */
    public AuthWebSocket(WebSocketListener listener, List<Draft> drafts) {
        super(listener, drafts);
    }

    /**
     * @see WebSocketImpl#WebSocketImpl(WebSocketListener, Draft)
     */
    public AuthWebSocket(WebSocketListener listener, Draft draft) {
        super(listener, draft);
    }

    /**
     * Sets an authorized user for this web socket. This automatically sets the web socket to authorized.
     *
     * @param user the user which will be now associated with this web socket
     */
    public void setAuthorizedUser(User user) {
        if (user != null) {
            authorizedUser = user;
            isAuthorized = true;
        }
    }

    /**
     * Sets this web socket into logged out state. This will also remove the associated user.
     */
    public void logout() {
        authorizedUser = null;
        isAuthorized = false;
    }

    /**
     * Returns whether or not this web socket is authorized. This means that the user using this web socket is currently logged in.
     *
     * @return whether or not this web socket is authorized
     */
    public boolean isAuthorized() {
        return isAuthorized;
    }

    /**
     * Returns whether or not this web socket is a admin. This means that the user using this web socket is currently logged in
     * and has administrator rights.
     *
     * @return whether or not this web socket is a admin.
     */
    public boolean isAdmin() {
        return isAuthorized && authorizedUser.isAdmin();

    }

    /**
     * Returns the user who uses this web socket. This can be null if the connection is anonymous (unauthorized).
     *
     * @return the user who uses this web socket
     */
    public User getUser() {
        return authorizedUser;
    }
}
