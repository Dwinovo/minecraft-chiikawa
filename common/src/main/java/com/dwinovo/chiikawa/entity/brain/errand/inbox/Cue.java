package com.dwinovo.chiikawa.entity.brain.errand.inbox;

import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.UUID;

/**
 * Something a pet does in answer to another, at once and in passing, that is not an errand: it is
 * never queued, never moves the pet's body, and reaches a pet whose own mind is off as well as one
 * at work, so a pet that cannot decide for itself still plays along.
 */
public sealed interface Cue {
    /**
     * Whoever is being photographed turns to the camera, strikes a pose and says something.
     *
     * @param photographer who holds the camera
     * @param lookTicks for how long to look at it
     * @param gesture the pose, by name
     * @param line what to say
     */
    record Pose(UUID photographer, int lookTicks, String gesture, VoiceMoment line) implements Cue {
    }
}
