package ch.ethz.geco.bass;

import java.util.Timer;

public class Main {

    public static void main(String[] args) {
        YoutubeDL yt = new YoutubeDL();
        switch (yt.checkInstall()) {
            case 0: break;
            case 1:
                System.out.println("Error: youtube-dl not found");
            case 2:
                System.out.println("Error: ffmpeg not found");
            case 3:
                System.out.println("Error: youtube-dl and ffmpeg not found");
                return;
            default: return;
        }

        if (!yt.chacheDir.exists())
            yt.chacheDir.mkdir();


        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new Player(), 0, 1000);
        timer.scheduleAtFixedRate(new DownloadManager(), 0, 1000);
    }
}
