package com.dwinovo.chiikawa.task;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ExtraCodecs;

/**
 * One slip on a labor board and who has it.
 *
 * @param slip the slip
 * @param reservation the pet on its way to take it; honoured until it expires
 * @param claim who took it, if anyone
 */
public record BoardSlot(PetTask slip, Optional<Reservation> reservation, Optional<Claim> claim) {
    public static final Codec<BoardSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        PetTask.CODEC.fieldOf("slip").forGetter(BoardSlot::slip),
        ExtraCodecs.strictOptionalField(Reservation.CODEC, "reservation").forGetter(BoardSlot::reservation),
        ExtraCodecs.strictOptionalField(Claim.CODEC, "claim").forGetter(BoardSlot::claim)
    ).apply(instance, BoardSlot::new));

    public static BoardSlot open(PetTask slip) {
        return new BoardSlot(slip, Optional.empty(), Optional.empty());
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

    /**
     * Who took a slip, as the board shows it.
     *
     * @param pet the pet's name
     * @param owner its owner's name, empty for a wild pet
     */
    public record Claim(String pet, String owner) {
        public static final Codec<Claim> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("pet").forGetter(Claim::pet),
            ExtraCodecs.strictOptionalField(Codec.STRING, "owner", "").forGetter(Claim::owner)
        ).apply(instance, Claim::new));

        /** @return {@code "<pet> (<owner>)"}, or just the pet's name when it has no owner */
        public String describe() {
            return owner.isEmpty() ? pet : pet + " (" + owner + ")";
        }
    }

    public boolean claimed() {
        return claim.isPresent();
    }

    /** Whether {@code pet} may take this slip: it is not taken and nobody else holds it. */
    public boolean openTo(UUID pet, long gameTime) {
        return !claimed() && reservation.filter(held -> !held.pet().equals(pet) && held.until() > gameTime).isEmpty();
    }

    /** Whether {@code pet} holds a live reservation on this slip. */
    public boolean reservedBy(UUID pet, long gameTime) {
        return !claimed() && reservation.filter(held -> held.pet().equals(pet) && held.until() > gameTime).isPresent();
    }

    public BoardSlot reserve(UUID pet, long until) {
        return new BoardSlot(slip, Optional.of(new Reservation(pet, until)), Optional.empty());
    }

    public BoardSlot release() {
        return new BoardSlot(slip, Optional.empty(), claim);
    }

    public BoardSlot claim(Claim by) {
        return new BoardSlot(slip, Optional.empty(), Optional.of(by));
    }
}
