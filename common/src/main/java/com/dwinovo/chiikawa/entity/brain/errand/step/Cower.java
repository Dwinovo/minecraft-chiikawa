package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepTag;
import com.dwinovo.chiikawa.entity.brain.errand.view.Attacker;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Stays where it is, scared, until it is safe: nothing has hurt the pet for a while and what did is far enough off. Every
 * hurt starts the wait again. The pet cowers as its character does, with the first of the animations its character has
 * among those the tuning names for it. Always interruptible.
 */
public final class Cower implements Step {
    private final ErrandTunings.Flee tuning;
    private String performance = "";
    private long quietSince;
    private UUID attacker;

    /** @param tuning how long and how far, and how each character cowers */
    public Cower(ErrandTunings.Flee tuning) {
        this.tuning = tuning;
    }

    @Override
    public void begin(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        performance = tuning.cowers().performanceOf(BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType()));
        quietSince = handle.now();
        attacker = Attacker.of(pet).map(Attacker::id).orElse(null);
        handle.stopWalking();
        handle.clearLook();
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        AbstractPet pet = live.pet();
        if (pet.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY)) {
            quietSince = handle.now();
            attacker = Attacker.of(pet).map(Attacker::id).orElse(attacker);
        }
        boolean quiet = handle.now() - quietSince >= tuning.safeTicks();
        boolean farOff = attacker == null
            || live.creature(attacker).filter(near -> near.distanceTo(pet) < tuning.safeDistance()).isEmpty();
        return quiet && farOff ? StepStatus.DONE : StepStatus.RUNNING;
    }

    @Override
    public String performance() {
        return performance;
    }

    @Override
    public Set<StepTag> tags() {
        return Set.of(StepTag.SCARED);
    }
}
