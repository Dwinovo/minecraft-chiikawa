package com.dwinovo.chiikawa.entity.brain.intent;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

/**
 * One thing a pet may decide to do. Intents are stateless: conditions and score
 * read only the {@link IntentContext}; per-pet progress lives in brain memories. How much
 * it matters, and what it is worth to the pet, is data: {@link ErrandSettings}.
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

    /**
     * Where this errand takes the pet, as the context has it: the shop, the board, the owner, whoever
     * it goes to see. The selector admits the intent only if the pet's anchor allows the pet there, so
     * no intent checks the area itself. Nothing for one that goes nowhere in particular: wandering,
     * sitting, playing where it stands.
     */
    default Optional<GlobalPos> destination(IntentContext ctx) {
        return Optional.empty();
    }

    /**
     * How much the pet fancies this among the intents of its priority, from 0 to 1, before its
     * personality weighs it: the score in the intent's settings, unless the intent is worth more
     * or less as the pet's situation changes.
     */
    default float score(IntentContext ctx, ErrandSettings<?> settings) {
        return settings.score();
    }

    /**
     * The work counter this intent's work reports, see
     * {@link com.dwinovo.chiikawa.task.PetWorkCounters}. A pet carrying a slip that
     * counts it prefers this intent.
     */
    default Optional<ResourceLocation> workCounter() {
        return Optional.empty();
    }

    /**
     * Called when the selector switches to this intent, with what it saw when it chose it: for an
     * intent whose behavior carries out a choice made then, so that it does what was admitted.
     */
    default void onStart(IntentRuntime runtime, IntentContext ctx) {
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
