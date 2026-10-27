package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Waits by the owner's bed for them to wake, lying quiet as a cat does, and lying as its character sleeps, which is the
 * sleep errand's to say and nobody else's. Done once the owner is out of bed, whether they woke or left it, and given up on
 * if they are gone from the pet's world. The errand's time limit is what ends the wait of a pet whose owner sleeps on.
 * Always interruptible.
 */
public final class WaitForWaking implements Step {
    private final Optional<ErrandTunings.Performances> sleeps;
    private String performance = "";

    /** @param sleeps how each character sleeps; if the sleep errand has no settings, the pet waits standing */
    public WaitForWaking(Optional<ErrandTunings.Performances> sleeps) {
        this.sleeps = sleeps;
    }

    @Override
    public void begin(Handle handle) {
        performance = sleeps.map(found -> found.performanceOf(BuiltInRegistries.ENTITY_TYPE.getKey(LiveHandle.of(handle).pet().getType())))
            .orElse("");
    }

    @Override
    public StepStatus tick(Handle handle) {
        return LiveHandle.of(handle).owner()
            .map(owner -> owner.isSleeping() ? StepStatus.RUNNING : StepStatus.DONE)
            .orElseGet(() -> StepStatus.gaveUp("gone"));
    }

    @Override
    public String performance() {
        return performance;
    }
}
