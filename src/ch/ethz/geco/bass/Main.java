package ch.ethz.geco.bass;

import java.io.IOException;
import java.util.Timer;

public class Main {
    private static Server server;
    static Player player;

    public static void main(String[] args) throws IOException {
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

        if (!YoutubeDL.cacheDir.exists())
            YoutubeDL.cacheDir.mkdir();


        Timer timer = new Timer();

        player = new Player();
        timer.scheduleAtFixedRate(player, 0, 1000);

        // Start webserver to handel queue requests
        server = new Server();

        // Stop current playback in case the program is exited
        Runtime.getRuntime().addShutdownHook(new Thread(() -> player.stop()));
    }
}
