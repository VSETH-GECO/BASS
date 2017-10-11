package ch.ethz.geco.bass;

import ch.ethz.geco.bass.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static Player player;

    static Server server;
    static final int cacheSize = 200; // in MiB

    public static void main(String[] args) throws IOException {
        logger.info("BASS");
        logger.info("The GECO Byro Audio Speaker System. Copyright (c) 2017, Licensed under MIT");

        // Create cache folder if none exists
        if (!YoutubeDL.cacheDir.exists()) {
            YoutubeDL.cacheDir.mkdir();
            new File(YoutubeDL.cacheDir, ".log").createNewFile();
        }

        player = new Player();

        // Start webserver to handel queue requests
        server = new Server(player, 8080);
        server.start();

        // Stop current playback in case the program is exited
        Runtime.getRuntime().addShutdownHook(new Thread(() -> player.stop()));
    }
}
