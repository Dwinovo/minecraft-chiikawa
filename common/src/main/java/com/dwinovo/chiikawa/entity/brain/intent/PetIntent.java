package com.dwinovo.chiikawa.entity.brain.intent;

import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

/**
 * One thing a pet may decide to do. Intents are stateless: conditions and score
 * read only the {@link IntentContext}; per-pet progress lives in brain memories.
 *
 * <p>An intent says whether and how much a pet wants to do something; the behaviors
 * of its {@link #activity()} say how. The selector filters intents by the
 * directive's permission table before any condition is checked.
 */
public interface PetIntent {
    ResourceLocation id();

    IntentCategory category();

    /** The activity entered while this intent runs, registered through {@code PetActivities}. */
    Activity activity();

    /** Condition to start. */
    IntentCheck canRun(IntentContext ctx);

    /** Condition to keep running; may be looser than {@link #canRun}. */
    default IntentCheck canContinue(IntentContext ctx) {
        return canRun(ctx);
    }

    /** Base score from 0 to 1. */
    float score(IntentContext ctx);

    /** Stages of a long intent; empty for intents that are just their activity. */
    default List<IntentStep> steps() {
        return List.of();
    }

    /** Ticks the intent may not start again after it ended without completing. */
    default int failureCooldown() {
        return 0;
    }

    /** Ticks the intent may not start again after its last step completed. */
    default int successCooldown() {
        return 0;
    }

    /** Called when the selector switches away from this intent, before its behaviors are stopped. */
    default void onStop(IntentRuntime runtime) {
    }

    /**
     * Memories holding this intent's progress that only make sense while the intent
     * is available; the selector erases them whenever the pet's capability no longer
     * offers the intent or its directive no longer permits it.
     */
    default Set<MemoryModuleType<?>> forgetWhenUnavailable() {
        return Set.of();
    }
}
