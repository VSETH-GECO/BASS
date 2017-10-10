package ch.ethz.geco.bass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Player class
 * <p>
 * Responsible for handling both playback and queue
 */
class Player {

    // Subclasses and enums
    enum Status {
        Queued, Downloading, Downloaded, Playing, Paused, Finished
    }

    class Track {
        String id;
        String url;
        String loc;
        String title;
        String duration;

        Status status;
    }

    // Vars
    private static final Logger logger = LoggerFactory.getLogger(Player.class);
    private Queue<Track> tracks;
    private Track current;
    private Clip clip;

    // Methods
    Player() {
        tracks = new ConcurrentLinkedQueue<>();
    }


    /**
     * Resumes the current track
     * @return false if there was nothing paused
     */
    boolean resume() {
        if (current != null && current.status == Status.Paused) {
            logger.info("Playback resumed");
            clip.start();
            current.status = Status.Playing;

            return true;
        }

        return false;
    }

    /**
     * Pauses the current playback
     * @return false if there was nothing to pause
     */
    boolean pause() {
        // Clip.stop() is pausing the track
        if (current != null && current.status == Status.Playing) {
            logger.info("Playback paused");
            clip.stop();
            current.status = Status.Paused;

            return true;
        }

        return false;
    }

    boolean nextTrack() {
        // Stop current playback and free resources
        if (current != null) {
            clip.stop();
            clip.close();
            clip = null;
            current.status = Status.Finished;
        }

        // If available play next track
        if (!tracks.isEmpty()) {
            current = tracks.poll();
            play(current);
            return true;
        }

        return false;
    }

    /**
     * Play the specified track and save it's
     * process to process variable of the class
     *
     * @param track to be played
     */
    private void play(Track track) {
        try {
            if (track.status == Status.Queued) {
                DownloadManager.download(track);
                Thread.sleep(1000);
                play(track);
                return;
            } else if (track.status == Status.Downloading) {
                Thread.sleep(1000);
                play(track);
                return;
            }


            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(YoutubeDL.cacheDir.toString() + "/" + track.loc));
            clip = AudioSystem.getClip();
            clip.open(ais);
            clip.start();
            clip.addLineListener(event -> {
                if (event.getType() != LineEvent.Type.STOP || event.getFramePosition() != clip.getFrameLength()) {
                    return;
                }

                finished();
            });

            current.status = Status.Playing;
            logger.info("Playback started.");
            Main.server.broadcast("Now playing: " + current.title);

            // Download the next track if there is one
            if (!tracks.isEmpty())
                DownloadManager.download(tracks.peek());
        } catch (IOException | InterruptedException | UnsupportedAudioFileException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void finished() {
        logger.info("Playback finished.");

        current.status = Status.Finished;
        if (!tracks.isEmpty()) {
            current = tracks.poll();
            play(current);
        }
    }

    private boolean isPlaying() {
        return current.status == Status.Playing;
    }

    /**
     * Add a new track to the queue.
     *
     * @param url of the track
     * @return false if the url was not valid
     */
    boolean add(String url) {
        Track newTrack = new Track();
        YoutubeDL yt = new YoutubeDL();

        if ((newTrack.id = yt.getVideoId(url)) != null) {
            newTrack.url = url;
            newTrack.title = yt.getVideoTitle(url);
            newTrack.status = Status.Queued;
            newTrack.duration = yt.getVideoDuration(url);

            tracks.add(newTrack);

            logger.info(newTrack.title + " added.");
            return true;
        }

        return false;
    }

    /**
     * Update the player object. Can start a playback if a new
     * Track has been added after finishing the last one.
     */
    void update() {
        logger.info("Updating player state.");
        if (current == null || current.status == Status.Finished) {
            if (!tracks.isEmpty()) {
                current = tracks.poll();
                play(current);
            }
            // else: nothing to do
        }

        if (current != null && current.status == Status.Playing && !tracks.isEmpty() && tracks.peek().status != Status.Downloaded) {
            DownloadManager.download(tracks.peek());
        }
    }

    /**
     * Does, well, stop the current playback
     * @return false is there is nothing to be stopped;
     */
    boolean stop() {
        if (current != null && clip != null) {
            logger.info("Playback stopped");
            clip.stop();
            clip.close();
            clip = null;
            current.status = Status.Finished;

            return true;
        }

        return false;
    }

    /**
     * @return the current track
     */
    Track getCurrent() {
        return current;
    }

    String getPosition() {
        if (clip == null)
            return "0:00";

        // Format time output
        String pos = String.format("%d:%02d",
                (int) Math.floor(clip.getMicrosecondPosition() / 1_000_000 / 60),
                (int) ((clip.getMicrosecondPosition() / 1_000_000) % 60));

        if (!clip.isRunning())
            pos = "(paused) " + pos;

        return pos;
    }

    /**
     * @return the next track in queue
     */
    Track getNext() {
        return tracks.peek();
    }
}
