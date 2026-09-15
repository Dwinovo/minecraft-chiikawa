package com.dwinovo.chiikawa.entity.brain.intent;

import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Ring buffer of a pet's most recent intent switches, kept in memory only while
 * {@code /chiikawa debug intent log on} is active for that pet.
 */
public final class IntentSwitchLog {
    public static final int CAPACITY = 20;
    private static final int TOP_SCORES = 3;

    /**
     * @param gameTime when the switch happened
     * @param from the intent switched away from, {@code null} if there was none
     * @param to the intent switched to, {@code null} if nothing could run
     * @param cause why the pet evaluated or why the previous intent ended
     * @param top the best scores of the evaluation, highest first
     */
    public record Entry(long gameTime, @Nullable ResourceLocation from, @Nullable ResourceLocation to,
            String cause, List<IntentSelector.Scored> top) {
    }

    private final ArrayDeque<Entry> entries = new ArrayDeque<>(CAPACITY);

    void record(long gameTime, @Nullable ResourceLocation from, @Nullable ResourceLocation to, String cause,
            List<IntentSelector.Scored> ranking) {
        if (entries.size() == CAPACITY) {
            entries.removeFirst();
        }
        entries.addLast(new Entry(gameTime, from, to, cause, List.copyOf(ranking.subList(0, Math.min(TOP_SCORES, ranking.size())))));
    }

    /** @return the recorded switches, oldest first */
    public List<Entry> entries() {
        return List.copyOf(entries);
    }
}
