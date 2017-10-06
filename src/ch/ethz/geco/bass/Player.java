package ch.ethz.geco.bass;

import java.io.IOException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TimerTask;

/**
 * Player class
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
        tracks = new PriorityQueue<>();
    }

    /**
     * Checks in intervals of 1sec (see main()) the status of the
     * playback and plays a new track if the last one finished.
     */
    public void run() {
        System.out.println(current);
        // Nothing to do
        if (current == null && tracks.isEmpty())
            return;

        if (current == null || current.status.equals(Status.Finished)) {
            current = tracks.poll();

            if (current == null) {
                // Do nothing

            } else if (current.status.equals(Status.Downloaded)) {
                play(current);
                current.status = Status.Playing;
                DownloadManager.download(tracks.peek());

            } else if (current.status.equals(Status.Downloading)) {
                // Wait for download to finish

            } else if (current.status.equals(Status.Queued)){
                DownloadManager.download(current);
                current.status = Status.Downloaded;
            }
        } else if (current.status.equals(Status.Playing)) {
            if (!isPlaying())
                current.status = Status.Finished;
        }
    }

    /**
     * Play the specified track and save it's
     * process to process variable of the class
     *
     * @param track to be played
     */
    private void play(Track track) {
        try {
            p = Runtime.getRuntime().exec("ffplay -nodisp ./" + track.loc, null, YoutubeDL.cacheDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isPlaying() {
        return p != null && p.isAlive();
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
            //TODO fix

            System.out.println(newTrack.title + " added.");
            return true;
        }

        return false;
    }

    Track getCurrent() {
        return current;
    }

    Track getNext() {
        return tracks.peek();
    }
}
