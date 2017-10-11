package ch.ethz.geco.bass.audio;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.AudioPlayerInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Consumes audio frames from the audio player decodes them and plays them
 */
public class AudioConsumer extends Thread {
    /**
     * The logger of this class
     */
    private static final Logger logger = LoggerFactory.getLogger(AudioConsumer.class);

    /**
     * The output format
     */
    public static final AudioDataFormat outputFormat = new AudioDataFormat(2, 44100, 960, AudioDataFormat.Codec.PCM_S16_LE);

    /**
     * The audio output line
     */
    private static SourceDataLine output;

    @Override
    public void run() {
        AudioInputStream stream = AudioPlayerInputStream.createStream(AudioManager.getPlayer(), outputFormat, outputFormat.frameDuration(), true);
        SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat());
        try {
            output = (SourceDataLine) AudioSystem.getLine(info);
            output.open(stream.getFormat());

            output.start();

            byte[] buffer = new byte[outputFormat.bufferSize(2)];
            int chunkSize;

            logger.info("Started AudioConsumer!");
            try {
                while ((chunkSize = stream.read(buffer)) >= 0) {
                    output.write(buffer, 0, chunkSize);
                    sleep(outputFormat.frameDuration()); // Back-off for one frame
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
