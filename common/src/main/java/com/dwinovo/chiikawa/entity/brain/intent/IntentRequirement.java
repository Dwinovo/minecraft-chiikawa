package com.dwinovo.chiikawa.entity.brain.intent;

/**
 * An extra start condition an intent is built with, checked before its own.
 */
@FunctionalInterface
public interface IntentRequirement {
    IntentCheck check(IntentContext ctx);

    /**
     * Makes an intent strictly lower in priority than {@code other}: it can only start
     * or continue while {@code other} cannot start. The two never compete on score, so
     * no personality multiplier or randomness can reorder them.
     *
     * <p>{@code other} must be offered and permitted whenever the yielding intent is,
     * i.e. come from the same capability and category.
     *
     * @param other the intent that goes first
     * @return the requirement
     */
    static IntentRequirement yieldsTo(PetIntent other) {
        return ctx -> other.canRun(ctx).ok() ? IntentCheck.fail("higher_priority") : IntentCheck.OK;
    }
}
