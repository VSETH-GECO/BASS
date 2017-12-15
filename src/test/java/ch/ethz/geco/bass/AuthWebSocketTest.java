package ch.ethz.geco.bass;

import ch.ethz.geco.bass.server.AuthWebSocket;
import ch.ethz.geco.bass.server.auth.User;
import org.java_websocket.WebSocketListener;
import org.java_websocket.drafts.Draft;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class AuthWebSocketTest {
    private User user = new User(0, "Username");
    private User user2 = new User(1, "Username2");

    private WebSocketListener wsl = Mockito.mock(WebSocketListener.class);
    private Draft draft = Mockito.mock(Draft.class);

    @Test
    @DisplayName("Normal lifecycle")
    void lifecycle() {
        AuthWebSocket ws = new AuthWebSocket(wsl, draft);

        ws.setAuthorizedUser(user);
        assertTrue(ws.isAuthorized());
        assertNotNull(ws.getUser());

        ws.logout();
        assertFalse(ws.isAuthorized());
        assertNull(ws.getUser());
    }

    @Test
    @DisplayName("Double login")
    void doubleLogin() {
        AuthWebSocket ws = new AuthWebSocket(wsl, draft);

        ws.setAuthorizedUser(user);
        ws.setAuthorizedUser(user2);

        assertTrue(ws.isAuthorized());
        assertEquals(user2, ws.getUser());
    }

    @Test
    @DisplayName("Login after logout")
    void logoutAfterLogin() {
        AuthWebSocket ws = new AuthWebSocket(wsl, draft);

        ws.setAuthorizedUser(user);
        ws.logout();

        ws.setAuthorizedUser(user2);
        assertTrue(ws.isAuthorized());
        assertEquals(user2, ws.getUser());
    }

    @Test
    @DisplayName("Double logout")
    void doubleLogout() {
        AuthWebSocket ws = new AuthWebSocket(wsl, draft);

        ws.setAuthorizedUser(user);
        ws.logout();
        ws.logout();
    }
}
