package ch.ethz.geco.bass;

/**
 * DownloadManager class
 *
 * Responsible for handling the download status of tracks.
 * Also caches the downloads.
 */
class DownloadManager extends Thread {
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
                System.out.println("Already downloaded"); //TODO add to logger
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
        System.out.println("Downloading " + track.title + "/" + track.id + "..."); //TODO add to logger
        YoutubeDL yt = new YoutubeDL();
        track.loc = yt.download(track.url);
        track.status = Player.Status.Downloaded;
        System.out.println("Download finished");
    }
}
