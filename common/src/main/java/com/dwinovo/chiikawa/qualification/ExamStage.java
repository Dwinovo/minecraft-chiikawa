package com.dwinovo.chiikawa.qualification;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;

/**
 * Where a pet is with a licence's exam: nothing on, called by its owner to sit one at a
 * board today, or sat it there and waiting for the morning to hear how it did. One at a
 * time: a pet is called only with nothing on, and hears its results before it can be
 * called again.
 */
public sealed interface ExamStage {
    /** Nothing on. */
    None NONE = new None();

    Codec<ExamStage> CODEC = Codec.STRING.dispatch("stage", ExamStage::kind, ExamStage::codecOf);

    /** @return what the stage is called in a save */
    String kind();

    private static MapCodec<? extends ExamStage> codecOf(String kind) {
        return switch (kind) {
            case Called.KIND -> Called.CODEC;
            case Sat.KIND -> Sat.CODEC;
            default -> MapCodec.unit(NONE);
        };
    }

    record None() implements ExamStage {
        static final String KIND = "none";

        @Override
        public String kind() {
            return KIND;
        }
    }

    /**
     * Called to sit an exam.
     *
     * @param board the board the owner opened it at
     * @param day the day it was opened, and the only day it can be sat
     */
    record Called(GlobalPos board, long day) implements ExamStage {
        static final String KIND = "called";
        static final MapCodec<Called> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            GlobalPos.CODEC.fieldOf("board").forGetter(Called::board),
            Codec.LONG.fieldOf("day").forGetter(Called::day)
        ).apply(instance, Called::new));

        @Override
        public String kind() {
            return KIND;
        }
    }

    /**
     * Sat an exam and handed the paper in; the result is decided and heard the morning after.
     *
     * @param board where it sat, and where the results go up
     * @param day the day it sat
     * @param passed whether it passed
     */
    record Sat(GlobalPos board, long day, boolean passed) implements ExamStage {
        static final String KIND = "sat";
        static final MapCodec<Sat> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            GlobalPos.CODEC.fieldOf("board").forGetter(Sat::board),
            Codec.LONG.fieldOf("day").forGetter(Sat::day),
            Codec.BOOL.fieldOf("passed").forGetter(Sat::passed)
        ).apply(instance, Sat::new));

        @Override
        public String kind() {
            return KIND;
        }
    }
}
