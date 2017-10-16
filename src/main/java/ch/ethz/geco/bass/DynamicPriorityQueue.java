package ch.ethz.geco.bass;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DynamicPriorityQueue {

    public static List<AudioTrack> order(List<AudioTrack> tracks) {
        // Define comparators
        Comparator<AudioTrack> comparator = Comparator.comparing(track -> track.getPosition());
        comparator = comparator.thenComparing(Comparator.comparing(track -> track.getIdentifier()));

        // Sort using Java 8 magic
        Stream<AudioTrack> audioTrackStream = tracks.stream().sorted(comparator);

        // Get sorted list
        return audioTrackStream.collect(Collectors.toList());
    }
}
