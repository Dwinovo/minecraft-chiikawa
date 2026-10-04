package com.dwinovo.chiikawa.block;

import com.dwinovo.chiikawa.qualification.QualificationExam;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

/**
 * Who is signed up at an exam desk: the desk's own note of it, kept so the desk knows it is
 * taken and what sheet to show without having to find the pet, which may be anywhere. The
 * pet carries its exam itself; this is only the desk's side of it, and lapses by the clock.
 *
 * @param name what the pet was called, as the desk's screen shows it
 * @param rank the grade it is sitting, as the player reads it
 * @param day the day of the exam
 * @param passed how it did, once it has handed the paper in
 */
public record DeskBooking(UUID pet, String name, Identifier qualification, int rank, long day,
                          Optional<Boolean> passed) {
    public static final Codec<DeskBooking> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("pet").forGetter(DeskBooking::pet),
        Codec.STRING.fieldOf("name").forGetter(DeskBooking::name),
        Identifier.CODEC.fieldOf("qualification").forGetter(DeskBooking::qualification),
        Codec.INT.fieldOf("rank").forGetter(DeskBooking::rank),
        Codec.LONG.fieldOf("day").forGetter(DeskBooking::day),
        Codec.BOOL.optionalFieldOf("passed").forGetter(DeskBooking::passed)
    ).apply(instance, DeskBooking::new));

    /** The paper is in: how it did. */
    public DeskBooking handedIn(boolean result) {
        return new DeskBooking(pet, name, qualification, rank, day, Optional.of(result));
    }

    /** Whether the desk is held for the pet now: its exam is on and it has not handed in yet. */
    public boolean holdsDesk(long dayTime) {
        return passed.isEmpty() && QualificationExam.canSit(day, dayTime);
    }

    /** Whether the desk still has anything to show for it: the exam on, or the results until they come down. */
    public boolean current(long dayTime) {
        return holdsDesk(dayTime)
            || passed.isPresent() && (QualificationExam.day(dayTime) == day || QualificationExam.isResultsMorning(day, dayTime));
    }

    /** What the desk has on it for this booking now. */
    public DeskSheet sheet(long dayTime) {
        if (holdsDesk(dayTime)) {
            return DeskSheet.ANSWER;
        }
        return passed.filter(result -> QualificationExam.isResultsMorning(day, dayTime))
            .map(result -> result ? DeskSheet.PASSED : DeskSheet.FAILED)
            .orElse(DeskSheet.NONE);
    }
}
