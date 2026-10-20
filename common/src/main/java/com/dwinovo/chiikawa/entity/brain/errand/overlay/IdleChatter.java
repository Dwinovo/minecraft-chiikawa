package com.dwinovo.chiikawa.entity.brain.errand.overlay;

import com.dwinovo.chiikawa.entity.brain.errand.run.Aside;
import com.dwinovo.chiikawa.entity.brain.errand.run.Overlay;
import com.dwinovo.chiikawa.voice.VoiceMoment;

/**
 * A pet with nothing to do says something now and then. It tries every tick; whether it speaks, how often and
 * what it says is the pet's voice, which keeps quiet through its cooldown and in a crowd.
 */
public final class IdleChatter implements Overlay {
    @Override
    public void tick(Aside aside) {
        aside.say(VoiceMoment.IDLE);
    }
}
