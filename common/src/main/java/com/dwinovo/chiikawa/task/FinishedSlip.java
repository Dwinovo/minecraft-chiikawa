package com.dwinovo.chiikawa.task;

import net.minecraft.resources.Identifier;

/**
 * The {@code LAST_FINISHED_SLIP} memory: the last slip a pet was paid for and when, so
 * the others can tell who has just come off a job. Not saved.
 *
 * @param type the slip's type
 * @param gameTime when it was finished
 */
public record FinishedSlip(Identifier type, long gameTime) {
}
