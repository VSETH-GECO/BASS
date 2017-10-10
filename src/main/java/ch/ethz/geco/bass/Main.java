package ch.ethz.geco.bass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static Server server;
    private static Player player;

    public static void main(String[] args) throws IOException {
        logger.info("BASS");
        logger.info("The GECO Byro Audio Speaker System. Copyright (c) 2017, Licensed under MIT");

        player = new Player();

        // Start webserver to handel queue requests
        server = new Server(player, 8080);
        server.start();

        // Stop current playback in case the program is exited
        Runtime.getRuntime().addShutdownHook(new Thread(() -> player.stop()));
    }
}
