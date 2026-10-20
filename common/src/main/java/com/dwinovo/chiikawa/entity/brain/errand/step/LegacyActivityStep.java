package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetActivities;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Scaffolding, deleted in T17: a whole errand that is still carried out by an old brain activity. It
 * puts the brain into the activity and lets the activity's behaviors do what they have always done,
 * and takes it out again as the intent selector did.
 *
 * <p>It is always interruptible and never finishes by itself: the errand ends when it is no longer
 * wanted, or when something of a higher priority takes the pet. It claims no pose and no scene, so
 * the executor does not touch what its behaviors set, and the ledger keeps no rest for it, see
 * {@code RestStore#legacy}: its behaviors keep their own until each errand goes native, and each is
 * deleted with its errand's move out of here.
 */
public final class LegacyActivityStep implements Step {
    /**
     * What an errand's old setup and teardown may touch.
     *
     * @param pet the pet
     * @param level its level
     * @param brain its brain
     */
    public record Runtime(AbstractPet pet, ServerLevel level, Brain<AbstractPet> brain) {
    }

    private final Activity activity;
    private final Consumer<Runtime> onStart;
    private final Consumer<Runtime> onStop;

    /**
     * @param activity the activity entered while the errand runs
     * @param onStart called as the errand starts, after the activity is entered
     * @param onStop called as the errand ends, before the activity is left
     */
    public LegacyActivityStep(Activity activity, Consumer<Runtime> onStart, Consumer<Runtime> onStop) {
        this.activity = activity;
        this.onStart = onStart;
        this.onStop = onStop;
    }

    /** @return a step for an activity that needs no setup or teardown of its own */
    public static LegacyActivityStep of(Activity activity) {
        return new LegacyActivityStep(activity, runtime -> { }, runtime -> { });
    }

    @Override
    public void begin(Handle handle) {
        Runtime runtime = runtime(handle);
        runtime.brain().setActiveActivityIfPossible(activity);
        onStart.accept(runtime);
    }

    @Override
    public StepStatus tick(Handle handle) {
        return StepStatus.RUNNING;
    }

    /**
     * Switch-out order, as the intent selector had it: the errand's own teardown, then the brain leaves
     * the activity (vanilla erases the memories the activity declared), then its running
     * behaviors are stopped, since vanilla keeps ticking them after an activity change. Erasing
     * the declared memories first means an interrupted behavior never acts on an abandoned target,
     * e.g. an archer told to sit does not release its drawn arrow. The executor clears the walk,
     * the gaze and the path right after.
     */
    @Override
    public void release(Handle handle) {
        Runtime runtime = runtime(handle);
        onStop.accept(runtime);
        runtime.brain().useDefaultActivity();
        for (BehaviorControl<? super AbstractPet> behavior : PetActivities.behaviorsOf(runtime.brain(), activity)) {
            if (behavior.getStatus() == Behavior.Status.RUNNING) {
                behavior.doStop(runtime.level(), runtime.pet(), handle.now());
            }
        }
    }

    private static Runtime runtime(Handle handle) {
        AbstractPet pet = ((LiveHandle) handle).pet();
        return new Runtime(pet, (ServerLevel) pet.level(), pet.getBrain());
    }
}
