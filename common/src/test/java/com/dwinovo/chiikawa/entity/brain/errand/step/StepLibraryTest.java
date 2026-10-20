package com.dwinovo.chiikawa.entity.brain.errand.step;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.testing.FakeHandle;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The steps of the library that need no pet in a world: the ones that only wait and speak. */
class StepLibraryTest {
    @Test
    void waitingIsDoneOnceItsTimeHasPassed() {
        FakeHandle handle = new FakeHandle();
        handle.now = 100;
        Wait wait = new Wait(3);

        wait.begin(handle);
        assertEquals(StepStatus.RUNNING, wait.tick(handle));
        handle.now = 102;
        assertEquals(StepStatus.RUNNING, wait.tick(handle));
        handle.now = 103;
        assertEquals(StepStatus.DONE, wait.tick(handle));
    }

    @Test
    void sayingSaysOnceAndIsDoneInTheTickItTakes() {
        FakeHandle handle = new FakeHandle();
        Say say = new Say(VoiceMoment.HUNT);

        say.begin(handle);
        assertEquals(List.of(), handle.said);
        assertEquals(StepStatus.DONE, say.tick(handle));
        assertEquals(List.of(VoiceMoment.HUNT), handle.said);
    }
}
