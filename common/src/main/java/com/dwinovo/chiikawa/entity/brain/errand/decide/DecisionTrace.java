package com.dwinovo.chiikawa.entity.brain.errand.decide;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * How one decision went, errand by errand: which could not start and why, which could and with what
 * score, and what was picked. Made only when somebody asks, for the debug command; a decision made
 * without one does no more than it needs to.
 */
public final class DecisionTrace {
    /**
     * One errand, as the decision found it.
     *
     * @param errand which
     * @param priority its priority
     * @param score what the pet made of it, when it could start
     * @param verdict {@code startable}, or why it could not
     */
    public record Line(ResourceLocation errand, int priority, Optional<Float> score, String verdict) {
    }

    public static final String STARTABLE = "startable";
    /** The verdict on an errand the owner's directive does not permit. */
    public static final String NOT_PERMITTED = "not_permitted";
    /** The verdict on an errand the pet is resting from. */
    public static final String RESTING = "resting";
    /** The verdict on an errand with no settings, which is switched off. */
    public static final String SWITCHED_OFF = "switched_off";

    private final List<Line> lines = new ArrayList<>();
    @Nullable
    private Decision.Pick pick;

    /** Notes an errand that could not start, and why. */
    public void excluded(ResourceLocation errand, int priority, String reason) {
        lines.add(new Line(errand, priority, Optional.empty(), reason));
    }

    /** Notes an errand that could start. */
    public void startable(Scored scored) {
        lines.add(new Line(scored.errand(), scored.priority(), Optional.of(scored.score()), STARTABLE));
    }

    /** Notes what was picked. */
    public void picked(Decision.Pick pick) {
        this.pick = pick;
    }

    /** @return the errands, in the order the decision looked at them */
    public List<Line> lines() {
        return List.copyOf(lines);
    }

    /** @return what was picked, or nothing if nothing could be */
    public Optional<Decision.Pick> pick() {
        return Optional.ofNullable(pick);
    }
}
