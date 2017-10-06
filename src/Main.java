import java.util.Timer;

public class Main {

    public static void main(String[] args) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new Player(), 0, 1000);

        //TODO start DownloadManager
    }
}
