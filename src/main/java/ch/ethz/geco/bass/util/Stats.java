package ch.ethz.geco.bass.util;

import ch.ethz.geco.bass.audio.util.AudioTrackMetaData;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Stats {
    private static Stats instance;
    private static InfluxDB influxDB;

    private static final String dbName = "bass_stats";
    private static final Logger logger = LoggerFactory.getLogger(Stats.class);

    /**
     * @return the only instance of the Stats class
     */
    public static Stats getInstance() {
        if (instance == null) {
            instance = new Stats();
        }

        return instance;
    }

    /**
     * Connects to the local influxDB and creates a 'bass_stats' database
     * if it doesn't exist.
     */
    public void connect() {
        influxDB = InfluxDBFactory.connect("http://localhost:8086");
        influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS, Executors.defaultThreadFactory(), (failedPoints, throwable) -> { /* custom error handling here */ });
        logger.info("Connected to InfluxDB");

        if (!influxDB.databaseExists(dbName)) {
            logger.debug("Database not found, creating...");
            influxDB.createDatabase(dbName);
            logger.debug("Database created.");
        }

        influxDB.setDatabase(dbName);
    }

    /**
     * Logs the track that has been played.
     * @param track the track to be logged
     */
    public void trackPlayed(AudioTrack track) {
        if (influxDB.ping().getResponseTime() < 1000) {
            influxDB.write(Point.measurement("track")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .addField("action", "played")
                    .addField("uri", track.getInfo().uri)
                    .addField("title", track.getInfo().title)
                    .addField("user", ((AudioTrackMetaData) track.getUserData()).getUserName())
                    .build());
        }
    }
}
