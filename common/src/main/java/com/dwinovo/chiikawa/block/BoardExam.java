package com.dwinovo.chiikawa.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

/**
 * The exam room in front of a labor board: the seats pets sit an exam in, and who sat
 * which exam there and how they did, for the board to post the morning after. Kept by the
 * board and saved with it.
 */
public final class BoardExam {
    /** Seats in front of a board; a pet that finds them all taken goes to another board or waits for the next exam. */
    public static final int SEATS = 4;
    /** How long a seat is held for a pet on its way to it, as a slip is held. */
    public static final int SEAT_HOLD_TICKS = 1200;

    public static final Codec<BoardExam> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Seat.CODEC.listOf().optionalFieldOf("seats", List.of()).forGetter(exam -> exam.seats),
        Sitting.CODEC.listOf().optionalFieldOf("sittings", List.of()).forGetter(exam -> exam.sittings)
    ).apply(instance, BoardExam::new));

    private final List<Seat> seats;
    private final List<Sitting> sittings;

    public BoardExam() {
        this(List.of(), List.of());
    }

    private BoardExam(List<Seat> seats, List<Sitting> sittings) {
        this.seats = new ArrayList<>(seats);
        this.sittings = new ArrayList<>(sittings);
    }

    /**
     * The seat {@code pet} has, or a free one held for it now.
     *
     * @return the seat's place, 0 to {@link #SEATS} - 1; empty when every seat is taken
     */
    public OptionalInt seat(UUID pet, long gameTime) {
        seats.removeIf(seat -> seat.until() < gameTime);
        for (Seat seat : seats) {
            if (seat.pet().equals(pet)) {
                return OptionalInt.of(seat.place());
            }
        }
        for (int place = 0; place < SEATS; place++) {
            int wanted = place;
            if (seats.stream().noneMatch(seat -> seat.place() == wanted)) {
                seats.add(new Seat(pet, place, gameTime + SEAT_HOLD_TICKS));
                return OptionalInt.of(place);
            }
        }
        return OptionalInt.empty();
    }

    /** Whether a pet could have a seat now. */
    public boolean hasSeatFor(UUID pet, long gameTime) {
        return seats.stream().filter(seat -> seat.until() >= gameTime)
            .anyMatch(seat -> seat.pet().equals(pet))
            || seats.stream().filter(seat -> seat.until() >= gameTime).count() < SEATS;
    }

    /** Keeps {@code pet}'s seat held while it sits the exam. */
    public void keep(UUID pet, long gameTime) {
        seats.replaceAll(seat -> seat.pet().equals(pet) ? new Seat(pet, seat.place(), gameTime + SEAT_HOLD_TICKS) : seat);
    }

    /** {@code pet} gets up and goes. */
    public void leave(UUID pet) {
        seats.removeIf(seat -> seat.pet().equals(pet));
    }

    /** Someone handed a paper in. The board posts it the morning after. */
    public void record(Sitting sitting) {
        sittings.removeIf(earlier -> earlier.pet().equals(sitting.pet())
            && earlier.qualification().equals(sitting.qualification()));
        sittings.add(sitting);
    }

    /**
     * What the board has posted today: the exams sat before today, and not before the
     * last exam day's; what is sat today goes up tomorrow.
     */
    public List<Sitting> posted(long today) {
        long latest = sittings.stream().mapToLong(Sitting::day).filter(day -> day < today).max().orElse(-1L);
        return sittings.stream().filter(sitting -> sitting.day() == latest).toList();
    }

    /** Forgets what was sat on days before {@code day}, once a new exam day comes round. */
    public void forgetBefore(long day) {
        sittings.removeIf(sitting -> sitting.day() < day);
    }

    /**
     * A seat held for a pet.
     *
     * @param place 0 to {@link #SEATS} - 1, left to right across the front of the board
     * @param until game time the seat is held until
     */
    record Seat(UUID pet, int place, long until) {
        static final Codec<Seat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("pet").forGetter(Seat::pet),
            Codec.intRange(0, SEATS - 1).fieldOf("place").forGetter(Seat::place),
            Codec.LONG.fieldOf("until").forGetter(Seat::until)
        ).apply(instance, Seat::new));
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
