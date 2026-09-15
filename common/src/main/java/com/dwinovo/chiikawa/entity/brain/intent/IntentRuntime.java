package com.dwinovo.chiikawa.entity.brain.intent;

import com.dwinovo.chiikawa.entity.AbstractPet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;

/**
 * Live handles for switch-out cleanup ({@link PetIntent#onStop}). Scoring never sees
 * these, so an intent cannot change the world while it is only being considered.
 */
public record IntentRuntime(AbstractPet pet, ServerLevel level, Brain<AbstractPet> brain) {
}
