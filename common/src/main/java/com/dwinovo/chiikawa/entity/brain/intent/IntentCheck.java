package com.dwinovo.chiikawa.entity.brain.intent;

import org.jetbrains.annotations.Nullable;

/**
 * Result of an intent's start or continue condition.
 *
 * @param ok whether the condition holds
 * @param reasonKey translation key explaining a failure, {@code null} when {@code ok}
 */
public record IntentCheck(boolean ok, @Nullable String reasonKey) {
    public static final IntentCheck OK = new IntentCheck(true, null);

    /**
     * @param reason the part after {@code intent.chiikawa.fail.}
     * @return a failed check with that reason
     */
    public static IntentCheck fail(String reason) {
        return new IntentCheck(false, "intent.chiikawa.fail." + reason);
    }
}
