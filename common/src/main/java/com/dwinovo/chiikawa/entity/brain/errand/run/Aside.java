package com.dwinovo.chiikawa.entity.brain.errand.run;

import com.dwinovo.chiikawa.voice.VoiceMoment;
import net.minecraft.world.entity.Entity;

/**
 * What an {@link Overlay} may do to the pet it runs beside: say something and glance at someone, and nothing
 * that moves its body. That an overlay cannot walk is what lets it run beside any errand without taking the pet
 * from it.
 */
public interface Aside {
    /** Says something fitting the moment, if the pet has something to say and has not said too much of late. */
    void say(VoiceMoment moment);

    /** A passing look at someone for this long, unless the step the pet is at holds its gaze itself. */
    void glance(Entity target, int ticks);
}
