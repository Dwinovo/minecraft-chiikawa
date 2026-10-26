package com.dwinovo.chiikawa.entity.brain.errand.run;

import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.Optional;
import net.minecraft.world.entity.Entity;

/**
 * What an {@link Overlay}, or a remark, may do to the pet it runs beside: say something, glance at someone and make a
 * move once, and nothing that moves its body from where it is going. That an aside cannot walk is what lets it run
 * beside any errand without taking the pet from it.
 */
public interface Aside {
    /** Says something fitting the moment, if the pet has something to say and has not said too much of late. */
    default boolean say(VoiceMoment moment) {
        return say(moment, Optional.empty());
    }

    /**
     * Says something fitting the moment about something, if the pet has something to say and has not said too much of late.
     *
     * @param subject the translation key of what the line is about, for a line that has the place for it
     * @return whether it said it
     */
    boolean say(VoiceMoment moment, Optional<String> subject);

    /** Makes a move of its own once, by the name of its animation, if the pet's model has one. */
    void gesture(String name);

    /** A passing look at someone for this long, unless the step the pet is at holds its gaze itself. */
    void glance(Entity target, int ticks);
}
