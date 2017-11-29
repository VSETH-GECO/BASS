package ch.ethz.geco.bass.util;

import ch.ethz.geco.bass.audio.util.AudioTrackMetaData;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Stats {
    private static Stats instance;
    private static InfluxDB influxDB;
    private static String dbName = "bass_stats";

    public static Stats getInstance() {
        if (instance == null) {
            instance = new Stats();
        }

        return instance;
    }

    public void connect() {
        influxDB = InfluxDBFactory.connect("http://localhost:8086");
        influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS, Executors.defaultThreadFactory(), (failedPoints, throwable) -> { /* custom error handling here */ });

        if (!influxDB.databaseExists(dbName)) {
            influxDB.createDatabase(dbName);
        }

        influxDB.setDatabase(dbName);

    }

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
