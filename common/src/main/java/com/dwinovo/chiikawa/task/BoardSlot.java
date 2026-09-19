package com.dwinovo.chiikawa.task;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * One slip on a labor board and who has it.
 *
 * @param slip the slip
 * @param reservation the pet on its way to take it; honoured until it expires
 * @param claimed whether a pet has taken it
 */
public record BoardSlot(PetTask slip, Optional<Reservation> reservation, boolean claimed) {
    public static final Codec<BoardSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        PetTask.CODEC.fieldOf("slip").forGetter(BoardSlot::slip),
        Reservation.CODEC.optionalFieldOf("reservation").forGetter(BoardSlot::reservation),
        Codec.BOOL.optionalFieldOf("claimed", false).forGetter(BoardSlot::claimed)
    ).apply(instance, BoardSlot::new));

    public static BoardSlot open(PetTask slip) {
        return new BoardSlot(slip, Optional.empty(), false);
    }

    /**
     * @param pet the pet on its way
     * @param until game time the reservation lapses
     */
    public record Reservation(UUID pet, long until) {
        public static final Codec<Reservation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("pet").forGetter(Reservation::pet),
            Codec.LONG.fieldOf("until").forGetter(Reservation::until)
        ).apply(instance, Reservation::new));
    }

    /** Whether {@code pet} may take this slip: it is not taken and nobody else holds it. */
    public boolean openTo(UUID pet, long gameTime) {
        return !claimed && reservation.filter(held -> !held.pet().equals(pet) && held.until() > gameTime).isEmpty();
    }

    /** Whether {@code pet} holds a live reservation on this slip. */
    public boolean reservedBy(UUID pet, long gameTime) {
        return !claimed && reservation.filter(held -> held.pet().equals(pet) && held.until() > gameTime).isPresent();
    }

    public BoardSlot reserve(UUID pet, long until) {
        return new BoardSlot(slip, Optional.of(new Reservation(pet, until)), false);
    }

    public BoardSlot release() {
        return new BoardSlot(slip, Optional.empty(), claimed);
    }

    public BoardSlot claim() {
        return new BoardSlot(slip, Optional.empty(), true);
    }
}
