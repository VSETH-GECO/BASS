package ch.ethz.geco.bass;


import ch.ethz.geco.bass.server.AuthWebSocket;
import ch.ethz.geco.bass.server.Server;
import ch.ethz.geco.bass.server.util.WsPackage;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("WsPackage test")
class WsPackageTest {
    private String normalPackage = "{\"resource\":\"app\",\"action\":\"data\",\"data\":null}";
    private String normalPackageWData = "{\"resource\":\"app\",\"action\":\"data\",\"data\":{\"test\":1}}";
    private JsonObject jo;

    WsPackageTest() {
        jo = new JsonObject();
        jo.addProperty("test", 1);
    }

    @Test
    @DisplayName("Normal package creation")
    void createNorm() {
        assertAll("websocket",
                () -> assertEquals(normalPackage, WsPackage.create(Server.Resource.APP, Server.Action.DATA).toString()),
                () -> assertEquals(normalPackage, WsPackage.create().resource(Server.Resource.APP).action(Server.Action.DATA).toString()),
                () -> assertEquals(normalPackage, WsPackage.create().action(Server.Action.DATA).resource(Server.Resource.APP).toString())
        );
    }

    @Test
    @DisplayName("Normal package creation with data")
    void createNormWData() {
        assertAll("websocket",
                () -> assertEquals(normalPackageWData, WsPackage.create(Server.Resource.APP, Server.Action.DATA, jo).toString()),
                () -> assertEquals(normalPackageWData, WsPackage.create().resource(Server.Resource.APP).action(Server.Action.DATA).data(jo).toString()),
                () -> assertEquals(normalPackageWData, WsPackage.create().resource(Server.Resource.APP).data(jo).action(Server.Action.DATA).toString()),
                () -> assertEquals(normalPackageWData, WsPackage.create().action(Server.Action.DATA).resource(Server.Resource.APP).data(jo).toString()),
                () -> assertEquals(normalPackageWData, WsPackage.create().action(Server.Action.DATA).data(jo).resource(Server.Resource.APP).toString()),
                () -> assertEquals(normalPackageWData, WsPackage.create().data(jo).action(Server.Action.DATA).resource(Server.Resource.APP).toString()),
                () -> assertEquals(normalPackageWData, WsPackage.create().data(jo).resource(Server.Resource.APP).action(Server.Action.DATA).toString())
        );
    }

    @Test
    @DisplayName("Faulty package creation")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void createFaulty() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> WsPackage.create().toString()),
                () -> assertThrows(NullPointerException.class, () -> WsPackage.create().resource(Server.Resource.APP).toString()),
                () -> assertThrows(NullPointerException.class, () -> WsPackage.create().action(Server.Action.DATA).toString())
        );
    }

    @Test
    @DisplayName("Send normal package")
    void sendNorm() {
        AuthWebSocket ws = Mockito.mock(AuthWebSocket.class);

        WsPackage.create(Server.Resource.APP, Server.Action.DATA).send(ws);

        verify(ws).send(ArgumentMatchers.eq(normalPackage));

        verifyNoMoreInteractions(ws);
    }

    @Test
    @DisplayName("Send normal package with data")
    void sendNormWData() {
        AuthWebSocket ws = Mockito.mock(AuthWebSocket.class);

        WsPackage.create(Server.Resource.APP, Server.Action.DATA, jo).send(ws);

        verify(ws).send(ArgumentMatchers.eq(normalPackageWData));

        verifyNoMoreInteractions(ws);
    }
}
