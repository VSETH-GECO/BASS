package ch.ethz.geco.bass.audio;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.transcoder.OpusChunkDecoder;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Consumes audio frames from the audio player decodes them and plays them
 */
public class AudioConsumer extends Thread {
    /**
     * The logger of this class
     */
    private static final Logger logger = LoggerFactory.getLogger(AudioConsumer.class);

    /**
     * A thread scheduler
     */
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * The format of an opus frame
     */
    private static AudioDataFormat format;

    /**
     * The opus decoder
     */
    private static OpusChunkDecoder decoder;

    /**
     * The audio output line
     */
    private static SourceDataLine output;

    @Override
    public void run() {
        logger.debug("AudioConsumer started, waiting for first frame...");

        AudioFrame firstFrame;
        // Shitty spin-lock to wait for first frame
        while ((firstFrame = AudioManager.getPlayer().provide()) == null) {
            try {
                sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        format = firstFrame.format;
        decoder = new OpusChunkDecoder(format);

        logger.debug("First frame arrived. Encoding: " + format.codec.toString() + " | Sample Rate: " + format.sampleRate + " | Channel Count: " + format.channelCount + " | Sample Count: " + format.chunkSampleCount + " | Frame Time: " + format.frameDuration() + "[ms]");
        logger.debug(ByteOrder.nativeOrder().toString());
        logger.debug("" + firstFrame.data.length);
        // Get audio output line
        try {
            AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.sampleRate, 16,
                    format.channelCount, 2 * format.channelCount, format.sampleRate / format.channelCount, false);
            output = AudioSystem.getSourceDataLine(audioFormat);
            output.open(audioFormat);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

        scheduler.scheduleAtFixedRate(() -> {
            AudioFrame audioFrame = AudioManager.getPlayer().provide();
            if (audioFrame != null && output != null) {
                System.out.println(1);
                // Decode samples
                ShortBuffer pcmBuffer = ByteBuffer.allocateDirect(format.bufferSize(format.chunkSampleCount * 2)).order(ByteOrder.nativeOrder()).asShortBuffer();
                decoder.decode(audioFrame.data, pcmBuffer);

                System.out.println(2);

                System.out.println(pcmBuffer.array().length);

                System.out.println(3);

                // Short array to byte array
                short[] shortArray = pcmBuffer.array();
                ByteBuffer byteBuffer = ByteBuffer.allocate(2 * shortArray.length);
                for (short shortEl : shortArray) {
                    byteBuffer.putShort(shortEl);
                }

                // Write samples
                byte[] byteArray = byteBuffer.array();
                output.write(byteArray, 0, byteArray.length);
                output.drain();
            }
        }, format.frameDuration(), format.frameDuration(), TimeUnit.MILLISECONDS);
        logger.info("Periodic frame consuming started!");
    }
}
