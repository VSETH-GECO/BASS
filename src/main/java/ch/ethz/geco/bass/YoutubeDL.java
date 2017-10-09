package ch.ethz.geco.bass;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

class YoutubeDL {
    static final File cacheDir = new File("./cache");

    private String id       = null;
    private String title = null;
    private String duration = null;

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
     * Downloads the song corresponding to the url
     *
     * @param url of the song to be downloaded
     * @return filename of the downloaded track
     */
    String download(String url) {
        String filename = null;
        try {
            // -x for only downloading music
            // -o for reducing the filename to the yt id
            Process p = Runtime.getRuntime().exec("youtube-dl -x --audio-format wav -o %(id)s.%(ext)s " + url, null, cacheDir);

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while((line = br.readLine()) != null) {
                if (line.contains("[ffmpeg]")) {
                    filename = line.replace("[ffmpeg] Destination: ", "");
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
    String getVideoTitle(String url) {
        if (!getVideoInfo(url))
            return null;

        return title;
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
        System.out.println("request to find out id");
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
            title = br.readLine();
            id = br.readLine();
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
