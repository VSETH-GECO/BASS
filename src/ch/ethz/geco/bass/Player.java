package ch.ethz.geco.bass;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TimerTask;

/**
 * PLayer class
 *
 * Responsible for handling both playback and queue
 */
public class Player extends TimerTask {
    // Subclasses and enums
    enum Status {Queued, Downloading, Downloaded, Playing, Finished}
    class Track {
        String id;
        String url;
        String loc;
        String titel;
        String duration;

        Status status;
    }

    // Vars
    Queue<Track> tracks;
    Track current;

    // Methods
    Player() {
        tracks = new PriorityQueue<>();

        // Add a 'finished' track to prevent NP-exceptions
        current = new Track();
        current.status = Status.Finished;
    }

    /**
     * Checks in intervals of 1sec (see main()) the status of the
     * playback and plays a new track if the last one finished.
     */
    public void run() {
        if (current.status.equals(Status.Finished)) {
            Track next = tracks.poll();

            if (next.status.equals(Status.Downloaded)) {
                play(next);
                next.status = Status.Playing;
                current = next;

            } else if (next.status.equals(Status.Downloading)) {
                return;
                // Wait for download to finish

            } else {
                //TODO implement start Download

            }
        }
    }

    private void play(Track next) {
        //TODO implement
    }

    /**
     * Add a new track to the queue.
     *
     * @param url of the track
     * @return false if the url was not valid
     */
    public boolean add(String url) {
        Track newTrack = new Track();
        YoutubeDL yt = new YoutubeDL();

        if ((newTrack.id = yt.getVideoId(url)) != null) {
            newTrack.url      = url;
            newTrack.titel    = yt.getVideoTitel(url);
            newTrack.status   = Status.Queued;
            newTrack.duration = yt.getVideoDuration(url);
            return true;
        }

        return false;
    }
}
