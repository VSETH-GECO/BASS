package ch.ethz.geco.bass;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Player class
 *
 * Responsible for handling both playback and queue
 */
class Player {

    // Subclasses and enums
    enum Status {Queued, Downloading, Downloaded, Playing, Finished}
    class Track {
        String id;
        String url;
        String loc;
        String title;
        String duration;

        Status status;
    }

    // Vars
    private Process p;
    private Queue<Track> tracks;
    private Track current;

    // Methods
    Player() {
        tracks = new ConcurrentLinkedQueue<>();
    }

    /**
     * Play the specified track and save it's
     * process to process variable of the class
     *
     * @param track to be played
     */
    private void play(Track track) {
        System.out.println(track.status);
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


            // Start process with playback
            p = Runtime.getRuntime().exec("ffplay -nodisp -autoexit ./" + track.loc, null, YoutubeDL.cacheDir);

            // Get process handle and register callback function
            ProcessHandle ph = p.toHandle();
            CompletableFuture<ProcessHandle> cf = ph.onExit();
            cf.thenAccept(ph_ -> finished());

            current.status = Status.Playing;
            System.out.println("Playback started"); //TODO add to logger

            // Download the next track if there is one
            if (!tracks.isEmpty())
                DownloadManager.download(tracks.peek());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void finished() {
        System.out.println("Playback finished"); //TODO add to logger

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
            newTrack.url      = url;
            newTrack.title    = yt.getVideoTitle(url);
            newTrack.status   = Status.Queued;
            newTrack.duration = yt.getVideoDuration(url);

            tracks.add(newTrack);

            System.out.println(newTrack.title + " added.");
            return true;
        }

        return false;
    }

    /**
     * Update the player object. Can start a playback if a new
     * Track has been added after finishing the last one.
     */
    void update() {
        System.out.println("Updating player state"); //TODO add to logger
        if (current == null || current.status == Status.Finished) {
            if (!tracks.isEmpty()) {
                current = tracks.poll();
                play(current);
            }
            // else: nothing to do
        }

    }

    /**
     * Does, well, stop the current playback
     */
    void stop() {
        if (p != null)
            p.destroy();
        System.out.println("Playback stopped");
    }

    /**
     * @return the current track
     */
    Track getCurrent() {
        return current;
    }

    /**
     * @return the next track in queue
     */
    Track getNext() {
        return tracks.peek();
    }
}
