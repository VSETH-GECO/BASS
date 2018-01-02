package ch.ethz.geco.bass.audio;

import ch.ethz.geco.bass.Main;
import ch.ethz.geco.bass.audio.util.AudioTrackMetaData;
import ch.ethz.geco.bass.server.auth.UserManager;
import ch.ethz.geco.bass.util.ErrorHandler;
import ch.ethz.geco.bass.util.SQLite;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Responsible for saving all relevant data of the player
 * state to restore after a restart/crash.
 */
public class PlayerStateKeeper {
    /**
     * The logger of this class
     */
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);

    /**
     * A json parser used to store and load track data
     */
    private static final JsonParser jsonParser = new JsonParser();

    static {
        try {
            Connection con = SQLite.getConnection();

            if (!SQLite.tableExists("Players")) {
                logger.debug("Player table does not exist, creating...");
                PreparedStatement statement = con.prepareStatement("CREATE TABLE Players (ID INTEGER PRIMARY KEY, Paused INTEGER NOT NULL, TrackCount INTEGER NOT NULL, Track TEXT, Playlist TEXT);");
                statement.execute();
                logger.debug("Player table created!");
            } else {
                logger.trace("Player table already exists.");
            }
        } catch (SQLException e) {
            ErrorHandler.handleLocal(e);
        }
    }

    /**
     * Saves the current player state into the database with the given ID.
     * ID:0 is used to store the last state.
     *
     * @param ID the ID of this player state
     */
    public static void save(int ID) {
        try {
            Connection con = SQLite.getConnection();

            AudioPlayer audioPlayer = AudioManager.getPlayer();

            JsonObject currentTrack;
            if (audioPlayer.getPlayingTrack() != null) {
                currentTrack = Main.GSON.toJsonTree(audioPlayer.getPlayingTrack(), AudioTrack.class).getAsJsonObject();
            } else {
                currentTrack = new JsonObject();
            }

            Type playlistType = new TypeToken<List<AudioTrack>>() {
            }.getType();
            JsonArray trackList = (JsonArray) Main.GSON.toJsonTree(AudioManager.getScheduler().getPlaylist().getSortedList(), playlistType);

            PreparedStatement insertStatement = con.prepareStatement("INSERT OR REPLACE INTO Players VALUES (?,?,?,?,?);");
            insertStatement.setInt(1, ID);
            insertStatement.setInt(2, audioPlayer.isPaused() ? 1 : 0);
            insertStatement.setInt(3, AudioManager.getScheduler().trackCount.get());
            insertStatement.setString(4, currentTrack.toString());
            insertStatement.setString(5, trackList.toString());
            insertStatement.executeUpdate();

            logger.debug("Saved player state: " + ID);
        } catch (SQLException e) {
            ErrorHandler.handleLocal(e);
        }
    }

    /**
     * Loads the player state with the given ID. ID:0 is used to store the last state.
     *
     * @param ID the ID of the player state to load
     */
    public static void load(int ID) {
        try {
            logger.debug("Loading player state: " + ID);

            Connection con = SQLite.getConnection();

            PreparedStatement queryStatement = con.prepareStatement("SELECT * FROM Players WHERE ID = ?;");
            queryStatement.setInt(1, ID);
            ResultSet result = queryStatement.executeQuery();

            if (!result.isAfterLast()) {
                AudioManager.getPlayer().setPaused(result.getBoolean("Paused"));
                String jsonTrack = result.getString("Track");
                String jsonPlaylist = result.getString("Playlist");

                // Set track counter
                AudioManager.getScheduler().trackCount.set(result.getInt("TrackCount"));

                // Load current track
                JsonObject currentTrack = jsonParser.parse(jsonTrack).getAsJsonObject();

                // Only proceed if there was actually a track running at save time
                if (currentTrack.entrySet().size() > 0) {
                    AudioManager.getAudioPlayerManager().loadItemOrdered(PlayerStateKeeper.class, currentTrack.get("uri").getAsString(), new DeserializeLoadResultHandler(currentTrack)).get();

                    // Load playlist
                    for (JsonElement element : jsonParser.parse(jsonPlaylist).getAsJsonArray()) {
                        JsonObject track = element.getAsJsonObject();
                        AudioManager.getAudioPlayerManager().loadItemOrdered(PlayerStateKeeper.class, track.get("uri").getAsString(), new DeserializeLoadResultHandler(track));
                    }

                    // Resort playlist
                    AudioManager.getScheduler().getPlaylist().resort();
                }
            }
        } catch (InterruptedException | ExecutionException |SQLException e) {
            ErrorHandler.handleLocal(e);
        }
    }

    /**
     * A customized load result handler to reload previously loaded tracks
     */
    private static class DeserializeLoadResultHandler implements AudioLoadResultHandler {
        JsonObject trackObject;

        public DeserializeLoadResultHandler(JsonObject trackObject) {
            this.trackObject = trackObject;
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            // Load user data
            AudioTrackMetaData metaData = new AudioTrackMetaData(trackObject.get("id").getAsInt(), trackObject.get("userID").getAsInt(), trackObject.get("userName").getAsString());

            // Load votes
            Map<Integer, Byte> votes = metaData.getVotes();
            for (JsonElement vote : trackObject.get("voters").getAsJsonArray()) {
                for (Map.Entry<String, JsonElement> entry : vote.getAsJsonObject().entrySet()) {
                    votes.put(Integer.valueOf(entry.getKey()), ((byte) entry.getValue().getAsInt()));
                }
            }

            // Set meta data
            track.setUserData(metaData);

            // Set track position
            track.setPosition(trackObject.get("position").getAsLong());

            AudioManager.getScheduler().queue(track, false);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            logger.warn("A previously loaded track triggered a playlist load, ignoring it.");
        }

        @Override
        public void noMatches() {
            logger.error("Couldn't find previously loaded track, this should not happen.");
        }

        @Override
        public void loadFailed(FriendlyException exception) {
            logger.error("Couldn't find previously loaded track, this should not happen.");
        }
    }
}
