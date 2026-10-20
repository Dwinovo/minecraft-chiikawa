package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.voice.VoiceMoment;

/**
 * Says something fitting the moment, if the pet has something to say and has not said too much of late. Done in
 * the tick it takes, whether it spoke or not.
 */
public final class Say implements Step {
    private final VoiceMoment moment;

    /** @param moment what the pet says something for */
    public Say(VoiceMoment moment) {
        this.moment = moment;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        handle.say(moment);
        return StepStatus.DONE;
    }
}
