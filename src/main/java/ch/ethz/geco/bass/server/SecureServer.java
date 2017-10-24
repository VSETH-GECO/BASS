package ch.ethz.geco.bass.server;

import ch.ethz.geco.bass.util.ConfigManager;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

public class SecureServer {
    private int port;

    public SecureServer(int port) {
        this.port = port;
    }

    public Server start() {
        WebSocketImpl.DEBUG = true;

        Server server = new Server(port);

        // load up the key store
        final String STORETYPE = "JKS";
        final String KEYSTORE = "keystore.jks";
        String STOREPASSWORD = ConfigManager.getKeystorePassword();
        String KEYPASSWORD = ConfigManager.getKeyPassword();

        try {
            KeyStore ks = KeyStore.getInstance(STORETYPE);
            File kf = new File(KEYSTORE);

            /*if (!kf.exists()) {
                Runtime.getRuntime().exec(
                        "/home/bermos/bin/Java/jdk-9/bin/keytool " +
                                "-genkey " +
                                "-validity 3650 " +
                                "-keystore " + KEYSTORE + " " +
                                "-storepass " + STOREPASSWORD + " " +
                                "-keypass " + KEYPASSWORD + " " +
                                "-alias 'default' " +
                                "-dname 'CN=127.0.0.1, OU=BermosInc, O=BermosOrg, L=Zurich, S=Zurich, C=Switzerland"
                ).waitFor();

                System.out.println(kf.getAbsolutePath());
            }*/

            ks.load(new FileInputStream(kf), STOREPASSWORD.toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, KEYPASSWORD.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            server.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));

            server.start();

            return server;
        } catch (Exception e) { // gotta catch em all!
            e.printStackTrace();
        }

        return null;
    }
}
