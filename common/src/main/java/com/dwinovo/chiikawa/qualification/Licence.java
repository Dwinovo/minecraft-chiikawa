package com.dwinovo.chiikawa.qualification;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.GlobalPos;

/**
 * Where one pet stands with one licence. Kept with the pet, saved with it, and carried
 * into its doll, so a pet that falls between its exam and the results still hears them.
 *
 * @param held grades passed, 0 for none
 * @param practice slips of the licence's practice finished since the last exam
 * @param fails exams failed since the last pass
 * @param read whether it has read the book since the last exam
 * @param exam where it is with an exam: called to one, or waiting on the results of one
 */
public record Licence(int held, int practice, int fails, boolean read, ExamStage exam) {
    /** A pet that has never had anything to do with the licence. */
    public static final Licence NONE = new Licence(0, 0, 0, false, ExamStage.NONE);

    public static final Codec<Licence> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(0, 10).optionalFieldOf("held", 0).forGetter(Licence::held),
        Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("practice", 0).forGetter(Licence::practice),
        Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("fails", 0).forGetter(Licence::fails),
        Codec.BOOL.optionalFieldOf("read", false).forGetter(Licence::read),
        ExamStage.CODEC.optionalFieldOf("exam", ExamStage.NONE).forGetter(Licence::exam)
    ).apply(instance, Licence::new));

    /** One more slip of practice. */
    public Licence practised() {
        return new Licence(held, practice + 1, fails, read, exam);
    }

    /** Has read the book, for the next exam. */
    public Licence withBookRead() {
        return new Licence(held, practice, fails, true, exam);
    }

    /** Signed up by its owner to sit the exam at {@code desk} today. */
    public Licence called(GlobalPos desk, long day) {
        return new Licence(held, practice, fails, read, new ExamStage.Called(desk, day));
    }

    /** Let off an exam it was signed up for and never sat: as if it had not been. */
    public Licence excused() {
        return call().isPresent() ? new Licence(held, practice, fails, read, ExamStage.NONE) : this;
    }

    /**
     * Has sat the exam it was signed up for and handed the paper in: the practice and the book
     * are spent on it, and the result waits for the morning.
     */
    public Licence sat(boolean passed) {
        return call()
            .map(called -> new Licence(held, 0, fails, false, new ExamStage.Sat(called.desk(), called.day(), passed)))
            .orElse(this);
    }

    /** Has heard the result: a pass goes up a grade and forgets the fails, a fail adds one. */
    public Licence announced() {
        return paper()
            .map(sat -> sat.passed()
                ? new Licence(held + 1, practice, 0, read, ExamStage.NONE)
                : new Licence(held, practice, fails + 1, read, ExamStage.NONE))
            .orElse(this);
    }

    /** @return the exam it has been signed up for, if any */
    public Optional<ExamStage.Called> call() {
        return exam instanceof ExamStage.Called called ? Optional.of(called) : Optional.empty();
    }

    /** @return the exam it sat and has not heard about yet, if any */
    public Optional<ExamStage.Sat> paper() {
        return exam instanceof ExamStage.Sat sat ? Optional.of(sat) : Optional.empty();
    }

    /** Holds {@code grades} grades, as a wild pet turns up. */
    public static Licence holding(int grades) {
        return new Licence(grades, 0, 0, false, ExamStage.NONE);
    }
}
