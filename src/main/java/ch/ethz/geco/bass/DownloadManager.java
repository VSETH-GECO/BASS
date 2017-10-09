package ch.ethz.geco.bass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DownloadManager class
 *
 * Responsible for handling the download status of tracks.
 * Also caches the downloads.
 */
class DownloadManager extends Thread {
    private static Logger logger = LoggerFactory.getLogger(DownloadManager.class);
    private Player.Track track;

    private DownloadManager(Player.Track track) {
        this.track = track;
    }

    static void download(Player.Track track) {

        track.status = Player.Status.Downloading;

        // Check if file is in cache
        String[] files = YoutubeDL.cacheDir.list();
        for (String file : files != null ? files : new String[0]) {
            if (file.contains(track.id + ".")) {
                logger.info("File already in cache: " + file);
                track.loc = file;
                track.status = Player.Status.Downloaded;
                return;
            }
        }

        // If not we go ahead and download it
        DownloadManager dm = new DownloadManager(track);
        dm.start();
    }

    public void run() {
        logger.info("Downloading " + track.title + "/" + track.id + "...");
        YoutubeDL yt = new YoutubeDL();
        track.loc = yt.download(track.url);
        track.status = Player.Status.Downloaded;
        logger.info("Download finished!");
    }
}
