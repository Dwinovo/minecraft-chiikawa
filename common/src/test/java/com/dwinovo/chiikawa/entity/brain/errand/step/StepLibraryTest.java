package com.dwinovo.chiikawa.entity.brain.errand.step;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepTag;
import com.dwinovo.chiikawa.testing.FakeHandle;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The steps of the library that need no pet in a world. */
class StepLibraryTest {
    @Test
    void aHoldThatOnlyTimeEndsKeepsItsPoseAndItsTagsAndIsDeafToWhatThePetIsTold() {
        FakeHandle handle = new FakeHandle();
        handle.now = 10;
        Hold hold = new Hold(20, PetActivity.EXAM, Set.of(StepTag.SEATED), Optional.empty(), Hold.TIME_ALONE);

        hold.begin(handle);
        assertEquals(PetActivity.EXAM, hold.pose());
        assertEquals(Set.of(StepTag.SEATED), hold.tags());
        assertEquals(StepStatus.RUNNING, hold.onMessage(handle, new Message.Nudge("test")));
        handle.now = 29;
        assertEquals(StepStatus.RUNNING, hold.tick(handle));
        handle.now = 30;
        assertEquals(StepStatus.DONE, hold.tick(handle));
    }
}
