package ch.ethz.geco.bass;

/**
 * DownloadManager class
 *
 * Responsible for handling the download status of tracks.
 * Periodically polls the queue and begins download of a
 * track before the current one finishes.
 * Also caches the downloads.
 */
class DownloadManager {

    static void download(Player.Track track) {
        if (track == null)
            return;

        System.out.println("Downloading " + track.title + "/" + track.id + "...");

        track.status = Player.Status.Downloading;

        // Check if file is in cache
        String[] files = YoutubeDL.cacheDir.list();
        for (String file : files) {
            if (file.contains(track.id + ".")) {
                System.out.println("Already downloaded");
                return;
            }
        }

        // If not we go ahead and download it
        YoutubeDL yt = new YoutubeDL();
        track.loc = yt.download(track.url);
    }
}
