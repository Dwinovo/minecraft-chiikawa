package com.dwinovo.chiikawa.entity.brain.intent;

import net.minecraft.resources.Identifier;

/**
 * The {@code CURRENT_INTENT} memory. Not saved; a loaded pet picks again.
 *
 * @param id the running intent
 * @param startTick game time the intent was chosen
 */
public record RunningIntent(Identifier id, long startTick) {
}
