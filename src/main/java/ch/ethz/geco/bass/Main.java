package ch.ethz.geco.bass;

import ch.ethz.geco.bass.audio.AudioConsumer;
import ch.ethz.geco.bass.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static Server server;

    public static void main(String[] args) throws IOException {
        logger.info("BASS");
        logger.info("The GECO Byro Audio Speaker System. Copyright (c) 2017, Licensed under MIT");

        // Start web socket server
        server = new Server(8080);
        server.start();

        // Start audio consumer
        AudioConsumer audioConsumer = new AudioConsumer();
        audioConsumer.setName("AudioConsumer");
        audioConsumer.start();
    }
}
