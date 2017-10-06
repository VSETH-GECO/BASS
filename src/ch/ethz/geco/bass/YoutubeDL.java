package ch.ethz.geco.bass;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

class YoutubeDL {
    final File chacheDir = new File("./chache");

    String id       = null;
    String titel    = null;
    String duration = null;

    /**
     * Checks if youtube-dl is installed and can be found.
     * This should be run before starting anything else.
     *
     * @return false if youtube-dl cannot be executed
     */
    int checkInstall() {
        int returnValue = 0;
        try {
            Runtime.getRuntime().exec("youtube-dl -v");

        } catch (IOException e) {
            returnValue += 1;
        }

        try {
            Runtime.getRuntime().exec("ffmpeg -version");

        } catch (IOException e) {
            returnValue += 2;
        }

        return returnValue;
    }

    /**
     * Downloads
     *
     * @param url
     */
    String download(String url) {
        String filename = null;
        try {
            // -x for only downloading music
            // -o for reducing the filename to the yt id
            Process p = Runtime.getRuntime().exec("youtube-dl -x -o '%(id)s.%(ext)s' " + url, null, chacheDir);

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while((line = br.readLine()) != null) {
                if (line.contains("[ffmpg]")) {
                    filename = line.replace("[ffmpg] Destination: ", "");
                }
            }
            p.waitFor();
            br.close();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return filename;
    }

    /**
     * Fetches the title of the video based on it's url
     * Will return null if the url is invalid/title does
     * not exist.
     *
     * @param url of the video
     * @return title of the video or null if it doesn't exist
     */
    String getVideoTitel(String url) {
        if (!getVideoInfo(url))
            return null;

        return titel;
    }

    /**
     * Fetches the duration of the video based on it's url
     * Will return null if the url is invalid/title does
     * not exist.
     *
     * @param url of the video
     * @return duration of the video or null if it doesn't exist
     */
    String getVideoDuration(String url) {
        if (!getVideoInfo(url))
            return null;

        return duration;
    }

    /**
     * Fetches the id of the video based on it's url
     * Will return null if the url is invalid/title does
     * not exist.
     *
     * @param url of the video
     * @return id of the video or null if it doesn't exist
     */
    String getVideoId(String url) {
        if (!getVideoInfo(url))
            return null;

        return id;
    }

    private boolean getVideoInfo(String url) {
        if (id != null)
            return true;

        try {
            Process p = Runtime.getRuntime().exec("youtube-dl --get-id -e --get-duration " + url);

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            id = br.readLine();
            titel = br.readLine();
            duration = br.readLine();

            p.waitFor();
            br.close();

        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }
}
