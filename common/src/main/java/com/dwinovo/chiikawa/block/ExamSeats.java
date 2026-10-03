package com.dwinovo.chiikawa.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;

/**
 * The seats in front of a labor board that pets sit exams in: two rows facing the board,
 * each held for the pet on its way to it, as a slip is. A pet that finds them all taken
 * waits for one to come free.
 */
public final class ExamSeats {
    /** Seats along a row: two to either side of the middle, which stays clear for pets taking slips. */
    private static final int[] ACROSS = {-2, -1, 1, 2};
    /** How far out from the board's face each row stands. */
    private static final int[] ROWS = {2, 3};
    public static final int SEATS = ACROSS.length * ROWS.length;
    /** How long a seat is held for a pet on its way to it, as a slip is held. */
    public static final int HOLD_TICKS = 1200;

    public static final Codec<ExamSeats> CODEC = Seat.CODEC.listOf().xmap(ExamSeats::new, seats -> seats.held);

    private final List<Seat> held;

    public ExamSeats() {
        this(List.of());
    }

    private ExamSeats(List<Seat> held) {
        this.held = new ArrayList<>(held);
    }

    /**
     * Where the seat at {@code place} is: the front row first, left to right as the pets
     * face the board.
     *
     * @param facing the way the board faces, out over its seats
     */
    public static BlockPos position(BlockPos board, Direction facing, int place) {
        int at = Math.floorMod(place, SEATS);
        return board.relative(facing, ROWS[at / ACROSS.length])
            .relative(facing.getClockWise(), ACROSS[at % ACROSS.length]);
    }

    /**
     * The seat {@code pet} has, or a free one held for it now.
     *
     * @return the seat's place, 0 to {@link #SEATS} - 1; empty when every seat is taken
     */
    public OptionalInt take(UUID pet, long gameTime) {
        held.removeIf(seat -> seat.until() < gameTime);
        for (Seat seat : held) {
            if (seat.pet().equals(pet)) {
                return OptionalInt.of(seat.place());
            }
        }
        for (int place = 0; place < SEATS; place++) {
            int wanted = place;
            if (held.stream().noneMatch(seat -> seat.place() == wanted)) {
                held.add(new Seat(pet, place, gameTime + HOLD_TICKS));
                return OptionalInt.of(place);
            }
        }
        return OptionalInt.empty();
    }

    /** Whether {@code pet} could have a seat now. */
    public boolean hasSeatFor(UUID pet, long gameTime) {
        List<Seat> current = held.stream().filter(seat -> seat.until() >= gameTime).toList();
        return current.stream().anyMatch(seat -> seat.pet().equals(pet)) || current.size() < SEATS;
    }

    /** Whether anyone holds a seat now. */
    public boolean anyTaken(long gameTime) {
        return held.stream().anyMatch(seat -> seat.until() >= gameTime);
    }

    /** Keeps {@code pet}'s seat held while it sits the exam. */
    public void keep(UUID pet, long gameTime) {
        held.replaceAll(seat -> seat.pet().equals(pet) ? new Seat(pet, seat.place(), gameTime + HOLD_TICKS) : seat);
    }

    /** {@code pet} gets up and goes. */
    public void leave(UUID pet) {
        held.removeIf(seat -> seat.pet().equals(pet));
    }

    /**
     * A seat held for a pet.
     *
     * @param place 0 to {@link #SEATS} - 1; see {@link #position}
     * @param until game time the seat is held until
     */
    record Seat(UUID pet, int place, long until) {
        static final Codec<Seat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("pet").forGetter(Seat::pet),
            Codec.intRange(0, SEATS - 1).fieldOf("place").forGetter(Seat::place),
            Codec.LONG.fieldOf("until").forGetter(Seat::until)
        ).apply(instance, Seat::new));
    }
}
