package com.dwinovo.chiikawa.entity.brain.intent;

import net.minecraft.resources.ResourceLocation;

/**
 * The {@code CURRENT_INTENT} memory. Not saved; a loaded pet picks again.
 *
 * @param id the running intent
 * @param step index into the intent's steps; equal to the step count once the last
 *             step is done
 * @param startTick game time the intent was chosen
 * @param stepStartTick game time the current step began
 */
public record RunningIntent(ResourceLocation id, int step, long startTick, long stepStartTick) {
    public static RunningIntent start(ResourceLocation id, long gameTime) {
        return new RunningIntent(id, 0, gameTime, gameTime);
    }

    public RunningIntent nextStep(long gameTime) {
        return new RunningIntent(id, step + 1, startTick, gameTime);
    }
}
