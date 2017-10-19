package ch.ethz.geco.bass;

import ch.ethz.geco.bass.audio.AudioConsumer;
import ch.ethz.geco.bass.audio.AudioManager;
import ch.ethz.geco.bass.audio.gson.AudioTrackSerializer;
import ch.ethz.geco.bass.server.Server;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static Server server;

    /**
     * The global gson object.
     */
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(AudioTrack.class, new AudioTrackSerializer())
            .setPrettyPrinting()
            .create();

    public static void main(String[] args) throws IOException {
        logger.info("BASS");
        logger.info("The GECO Byro Audio Speaker System. Copyright (c) 2017, Licensed under MIT");

        // Start web socket server
        server = new Server(8455);
        server.start();

        // Start audio consumer
        AudioConsumer audioConsumer = new AudioConsumer();
        audioConsumer.setName("AudioConsumer");
        audioConsumer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stopSocket()));
    }
}
