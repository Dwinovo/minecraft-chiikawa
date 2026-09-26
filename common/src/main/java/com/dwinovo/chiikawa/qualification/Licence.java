package com.dwinovo.chiikawa.qualification;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * Where one pet stands with one licence. Kept with the pet, saved with it, and carried
 * into its doll, so a pet that falls between its exam and the results still hears them.
 *
 * @param held grades passed, 0 for none
 * @param practice slips of the licence's practice finished since the last exam
 * @param fails exams failed since the last pass
 * @param read whether it has read the book since the last exam
 * @param pending the result of an exam sat but not yet announced: whether it passed
 * @param decidedDay the exam day it last made up its mind about, -1 for none
 * @param wantsToSit whether, that day, it wanted to sit the exam
 */
public record Licence(int held, int practice, int fails, boolean read, Optional<Boolean> pending,
                      long decidedDay, boolean wantsToSit) {
    /** A pet that has never had anything to do with the licence. */
    public static final Licence NONE = new Licence(0, 0, 0, false, Optional.empty(), -1L, false);

    public static final Codec<Licence> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(0, 10).optionalFieldOf("held", 0).forGetter(Licence::held),
        Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("practice", 0).forGetter(Licence::practice),
        Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("fails", 0).forGetter(Licence::fails),
        Codec.BOOL.optionalFieldOf("read", false).forGetter(Licence::read),
        Codec.BOOL.optionalFieldOf("pending").forGetter(Licence::pending),
        Codec.LONG.optionalFieldOf("decided_day", -1L).forGetter(Licence::decidedDay),
        Codec.BOOL.optionalFieldOf("wants_to_sit", false).forGetter(Licence::wantsToSit)
    ).apply(instance, Licence::new));

    /** One more slip of practice. */
    public Licence practised() {
        return new Licence(held, practice + 1, fails, read, pending, decidedDay, wantsToSit);
    }

    /** Has read the book, for the next exam. */
    public Licence withBookRead() {
        return new Licence(held, practice, fails, true, pending, decidedDay, wantsToSit);
    }

    /** Has made up its mind about sitting the exam on {@code day}. */
    public Licence decided(long day, boolean wants) {
        return new Licence(held, practice, fails, read, pending, day, wants);
    }

    /**
     * Has sat the exam and handed the paper in: the practice and the book are spent on it,
     * and the result waits for the morning.
     */
    public Licence sat(boolean passed) {
        return new Licence(held, 0, fails, false, Optional.of(passed), decidedDay, wantsToSit);
    }

    /** Has heard the result: a pass goes up a grade and forgets the fails, a fail adds one. */
    public Licence announced() {
        if (pending.isEmpty()) {
            return this;
        }
        return pending.get()
            ? new Licence(held + 1, practice, 0, read, Optional.empty(), decidedDay, wantsToSit)
            : new Licence(held, practice, fails + 1, read, Optional.empty(), decidedDay, wantsToSit);
    }

    /** Holds {@code grades} grades, as a wild pet turns up. */
    public static Licence holding(int grades) {
        return new Licence(grades, 0, 0, false, Optional.empty(), -1L, false);
    }
}
