package ch.ethz.geco.bass.audio.gson;

import ch.ethz.geco.bass.audio.util.AudioTrackMetaData;
import com.google.gson.*;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Serializes an AudioTrack to json.
 */
public class AudioTrackSerializer implements JsonSerializer<AudioTrack> {
    @Override
    public JsonElement serialize(AudioTrack src, Type typeOfSrc, JsonSerializationContext context) {
        AudioTrackInfo info = src.getInfo();

        // Get meta data
        AudioTrackMetaData metaData = (AudioTrackMetaData) src.getUserData();

        // Parse votes to json array
        JsonArray votes = new JsonArray();
        for (Map.Entry<String, Byte> entry : metaData.getVotes().entrySet()) {
            JsonObject vote = new JsonObject();
            vote.addProperty(entry.getKey(), entry.getValue());
            votes.add(vote);
        }

        JsonObject jsonTrack = new JsonObject();
        jsonTrack.addProperty("id", metaData.getTrackID());
        jsonTrack.addProperty("uri", info.uri);
        jsonTrack.addProperty("userID", metaData.getUserID());
        jsonTrack.addProperty("userName", metaData.getUserName());
        jsonTrack.addProperty("title", info.title);
        jsonTrack.add("voters", votes);
        jsonTrack.addProperty("votes", metaData.getVoteCount());
        jsonTrack.addProperty("length", info.length);
        jsonTrack.addProperty("position", src.getPosition());

        return jsonTrack;
    }
}
