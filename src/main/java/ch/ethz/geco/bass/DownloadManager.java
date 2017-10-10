package ch.ethz.geco.bass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;

/**
 * DownloadManager class
 * <p>
 * Responsible for handling the download status of tracks.
 * Also caches the downloads.
 */
class DownloadManager extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(DownloadManager.class);
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
        Thread.currentThread().setName("Download");

        File logFile = new File(YoutubeDL.cacheDir, ".log");
        try {
            // Check if there is enough cache space
            long size = Files.walk(YoutubeDL.cacheDir.toPath()).mapToLong(p -> p.toFile().length() ).sum();
            logger.debug("Current cache size: " + size / 1_048_576 + "MB");

            // If not this will delete the oldest file in cache
            // TODO optimize to delete the lease used not oldest
            if (Main.cacheSize * 1_048_576 < size) {
                Scanner sc = new Scanner(logFile);
                while (sc.hasNext() && Main.cacheSize * 1_048_576 < size) {
                    File File = new File(YoutubeDL.cacheDir, sc.nextLine());
                    size -= File.length();
                    logger.debug("File deleted: " + File.delete());
                }

                // Update logfile
                File newLog = new File(YoutubeDL.cacheDir, ".newlog");
                FileWriter fw = new FileWriter(newLog);
                while (sc.hasNext()) {
                    fw.append(sc.nextLine());
                }
                fw.close();
                sc.close();
                logFile.delete();
                newLog.renameTo(logFile);
            }

            // Download new track
            logger.info("Downloading " + track.title + "/" + track.id + "...");
            YoutubeDL yt = new YoutubeDL();
            track.loc = yt.download(track.url);
            track.status = Player.Status.Downloaded;
            new FileWriter(logFile).append(track.loc).close();
            logger.info("Download finished!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
