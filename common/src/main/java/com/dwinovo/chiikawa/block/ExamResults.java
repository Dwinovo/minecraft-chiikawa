package com.dwinovo.chiikawa.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

/**
 * Who sat which exam at a labor board and how they did, for the board to post the morning
 * after. The pets hear their own results from what they carry; this is the board's copy,
 * for the players reading it.
 */
public final class ExamResults {
    public static final Codec<ExamResults> CODEC = Sitting.CODEC.listOf()
        .xmap(ExamResults::new, results -> results.sittings);

    private final List<Sitting> sittings;

    public ExamResults() {
        this(List.of());
    }

    private ExamResults(List<Sitting> sittings) {
        this.sittings = new ArrayList<>(sittings);
    }

    /** Someone handed a paper in. The board posts it the morning after. */
    public void record(Sitting sitting) {
        sittings.removeIf(earlier -> earlier.pet().equals(sitting.pet())
            && earlier.qualification().equals(sitting.qualification()));
        sittings.add(sitting);
    }

    /**
     * What the board has posted today: the exams sat on the last day before today that any
     * were; what is sat today goes up tomorrow.
     */
    public List<Sitting> posted(long today) {
        long latest = sittings.stream().mapToLong(Sitting::day).filter(day -> day < today).max().orElse(-1L);
        return sittings.stream().filter(sitting -> sitting.day() == latest).toList();
    }

    /** Takes down what was sat on days before {@code day}, once a new exam is sat. */
    public void forgetBefore(long day) {
        sittings.removeIf(sitting -> sitting.day() < day);
    }

    /**
     * An exam sat at this board.
     *
     * @param name what the pet was called, as the board posts it
     * @param rank the grade sat, as the player reads it: 5 for the first
     * @param passed whether it passed
     * @param day the day it was sat
     */
    public record Sitting(UUID pet, String name, ResourceLocation qualification, int rank, boolean passed, long day) {
        static final Codec<Sitting> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("pet").forGetter(Sitting::pet),
            Codec.STRING.fieldOf("name").forGetter(Sitting::name),
            ResourceLocation.CODEC.fieldOf("qualification").forGetter(Sitting::qualification),
            Codec.INT.fieldOf("rank").forGetter(Sitting::rank),
            Codec.BOOL.fieldOf("passed").forGetter(Sitting::passed),
            Codec.LONG.fieldOf("day").forGetter(Sitting::day)
        ).apply(instance, Sitting::new));
    }
}
