package com.dwinovo.chiikawa.entity.brain.errand.decide;

import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A pet's most recent errands and what became of them, newest last: what it started, why, and how
 * each ended. Kept in memory only, for the debug command.
 */
public final class DecisionLog {
    public static final int CAPACITY = 20;
    private static final int TOP_SCORES = 3;

    /** Something that happened to what the pet was doing. */
    public sealed interface Entry {
        /** @return when it happened, in game time */
        long gameTime();
    }

    /**
     * The pet went from one errand to another.
     *
     * @param from the errand switched away from, {@code null} if there was none
     * @param to the errand switched to, {@code null} if nothing could be started
     * @param cause why the pet decided or why the previous errand ended
     * @param top the best of the priority the decision chose from, highest score first
     */
    public record Switched(long gameTime, @Nullable ResourceLocation from, @Nullable ResourceLocation to,
                           String cause, List<Scored> top) implements Entry {
    }

    /**
     * An errand ended.
     *
     * @param errand which
     * @param outcome how
     * @param ranTicks for how long it had been under way
     */
    public record Ended(long gameTime, ResourceLocation errand, Outcome outcome, long ranTicks) implements Entry {
    }

    private final ArrayDeque<Entry> entries = new ArrayDeque<>(CAPACITY);

    /** Notes that the pet switched. */
    public void switched(long gameTime, @Nullable ResourceLocation from, @Nullable ResourceLocation to, String cause,
            List<Scored> ranking) {
        add(new Switched(gameTime, from, to, cause, List.copyOf(ranking.subList(0, Math.min(TOP_SCORES, ranking.size())))));
    }

    /** Notes that an errand ended. */
    public void ended(long gameTime, ResourceLocation errand, Outcome outcome, long ranTicks) {
        add(new Ended(gameTime, errand, outcome, ranTicks));
    }

    /** @return the last errand that ended, if the log still has one */
    public Optional<Ended> lastEnded() {
        for (Iterator<Entry> newest = entries.descendingIterator(); newest.hasNext();) {
            if (newest.next() instanceof Ended ended) {
                return Optional.of(ended);
            }
        }
        return Optional.empty();
    }

    /** @return what is recorded, oldest first */
    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    private void add(Entry entry) {
        if (entries.size() == CAPACITY) {
            entries.removeFirst();
        }
        entries.addLast(entry);
    }
}
