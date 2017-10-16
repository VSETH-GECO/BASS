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
        broadcastState("paused");
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        broadcastState("playing");
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        broadcastState("playing");
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        broadcastState("stopped");
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
    }

    /**
     * Broadcast new player state to all connected websockets
     *
     * @param state the state as string [playing|paused|stopped]
     */
    private void broadcastState(String state) {
        JsonObject jo = new JsonObject();
        JsonObject data = new JsonObject();

        data.addProperty("state", state);

        jo.addProperty("method", "post");
        jo.addProperty("type", "player/control");
        jo.add("data", data);

        Main.server.broadcast(jo);
    }
}