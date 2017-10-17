package ch.ethz.geco.bass.audio;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.AudioPlayerInputStream;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.plugin2.jvm.CircularByteBuffer;

import javax.sound.sampled.*;
import java.io.IOException;

/**
 * Consumes audio frames from the audio player and plays them on the default output.
 */
public class AudioConsumer extends Thread {
    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(AudioConsumer.class);

    /**
     * The output format. This is the format we request from lavaplayer and the conversion is handled by it.
     */
    public static final AudioDataFormat outputFormat = new AudioDataFormat(2, 48000, 960, AudioDataFormat.Codec.PCM_S16_LE);

    /**
     * The audio output line.
     */
    private static SourceDataLine output;

    @Override
    public void run() {
        AudioInputStream stream = AudioPlayerInputStream.createStream(AudioManager.getPlayer(), outputFormat, outputFormat.frameDuration(), true);
        SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat());
        try {
            output = (SourceDataLine) AudioSystem.getLine(info);
            output.open(stream.getFormat(), outputFormat.chunkSampleCount * 5);

            output.start();

            byte[] buffer = new byte[outputFormat.bufferSize(2)];
            int chunkSize;

            logger.debug("Audio Format:");
            logger.debug(String.format("%-25s | %-25s | %-25s", "Codec: " + outputFormat.codec.name(), "Channels: " + outputFormat.channelCount, "Sample Rate: " + outputFormat.sampleRate + " [Samples/s]"));
            logger.debug(String.format("%-25s | %-25s | %-25s", "Frame Duration: " + outputFormat.frameDuration() + " [ms]", "Frame Size: " + outputFormat.chunkSampleCount + " [Samples]", "Output Buffer Size: " + output.getBufferSize() + " [Bytes]"));

            logger.info("Started AudioConsumer!");
            try {
                long frameDuration = outputFormat.frameDuration();
                AudioPlayer player = AudioManager.getPlayer();

                // We write as much data as we can to the audio output. This will completely fill it's buffer which
                // contains approximately 1 second of audio data.
                while (true) {
                    if (!player.isPaused()) {
                        if ((chunkSize = stream.read(buffer)) >= 0) {
                            // If the buffer is full, this will simply block
                            output.write(buffer, 0, chunkSize);
                        } else {
                            logger.error("Reached end of stream, this should not happen!");
                            logger.error("Stopped AudioConsumer!");
                            break;
                        }
                    } else {
                        // Drain the rest of the buffer
                        output.drain();

                        // Back-off for a frame to prevent unnecessary usage of cpu time even when the thread has
                        // to wait for an unpause of the player.
                        sleep(frameDuration);
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
