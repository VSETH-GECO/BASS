package ch.ethz.geco.bass.audio.handle;

import ch.ethz.geco.bass.Main;
import ch.ethz.geco.bass.server.Server;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

/**
 * A class to handle all lava-player audio events.
 */
public class AudioEventHandler extends AudioEventAdapter {
    @Override
    public void onPlayerPause(AudioPlayer player) {
        JsonObject jo = new JsonObject();
        jo.addProperty("method", "post");
        jo.addProperty("type", "player/control/pause");
        jo.add("data", JsonNull.INSTANCE);

        Main.server.broadcast(jo);
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        setPlay();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        setPlay();
    }

    private void setPlay() {
        JsonObject jo = new JsonObject();
        jo.addProperty("method", "post");
        jo.addProperty("type", "player/control/play");
        jo.add("data", JsonNull.INSTANCE);

        Main.server.broadcast(jo);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        JsonObject jo = new JsonObject();
        jo.addProperty("method", "post");
        jo.addProperty("type", "player/control/stop");
        jo.add("data", JsonNull.INSTANCE);

        Main.server.broadcast(jo);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
    }
}