package ch.ethz.geco.bass;

import ch.ethz.geco.bass.audio.AudioConsumer;
import ch.ethz.geco.bass.audio.PlayerStateKeeper;
import ch.ethz.geco.bass.audio.gson.AudioTrackSerializer;
import ch.ethz.geco.bass.server.Server;
import ch.ethz.geco.bass.util.Stats;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
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

        // Start audio consumer
        AudioConsumer audioConsumer = new AudioConsumer();
        audioConsumer.setName("AudioConsumer");
        audioConsumer.start();

        // Start web socket server
        //SecureServer server = new SecureServer(8455);
        Main.server = new Server(8455);
        Main.server.setReuseAddr(true);
        server.start();

        // Disabled this because of missing local database
        //Stats.getInstance().connect();

        // Restore player state
        PlayerStateKeeper.load(0);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Main.server.stopSocket();

            PlayerStateKeeper.save(0);
        }));
    }
}
