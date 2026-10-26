package com.dwinovo.chiikawa.entity;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * What the owner has told a pet it may and may not do, from its screen: the kinds of errand it is not to go on, how far it
 * takes part in a fight, and whether it swaps to a weapon from its bag to fight back. The owner draws the boundary and the
 * pet keeps its own days inside it, so these hold whatever the pet would fancy, see
 * {@link com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints#allows}.
 *
 * <p>Saved with the pet as {@code Rules} and synced to its owner's screen; a key a save lacks is the default, which is how a
 * pet always was: everything on, {@link CombatStance#CLEAR}, swapping. Written only by the owner's own hand, see
 * {@link AbstractPet#setRules}.
 *
 * @param off the kinds of errand the pet is not to do
 * @param stance how far it takes part in a fight
 * @param swapWeapons whether it takes a weapon from its bag to fight back, and its tool again afterwards
 */
public record PetRules(EnumSet<Toggle> off, CombatStance stance, boolean swapWeapons) {
    /** A kind of errand the owner may switch off, which is one {@link ErrandCategory} of them. */
    public enum Toggle implements StringRepresentable {
        SHOP(ErrandCategory.SHOP),
        GIFT(ErrandCategory.GIFT),
        PHOTO(ErrandCategory.PHOTO),
        PICK_UP(ErrandCategory.PICK_UP),
        SOCIAL(ErrandCategory.SOCIAL);

        /** By lower-case name, as a save holds it. */
        public static final Codec<Toggle> CODEC = StringRepresentable.fromEnum(Toggle::values);

        private final ErrandCategory category;

        Toggle(ErrandCategory category) {
            this.category = category;
        }

        /** @return the errands this switches off */
        public ErrandCategory category() {
            return category;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** How a pet is until its owner says otherwise. */
    public static final PetRules DEFAULT = new PetRules(EnumSet.noneOf(Toggle.class), CombatStance.CLEAR, true);

    /** As saved, and as sent: a key that is missing is the default's. */
    public static final Codec<PetRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Toggle.CODEC.listOf().optionalFieldOf("Off", List.of()).forGetter(rules -> List.copyOf(rules.off)),
        CombatStance.CODEC.optionalFieldOf("Stance", DEFAULT.stance).forGetter(PetRules::stance),
        Codec.BOOL.optionalFieldOf("SwapWeapons", DEFAULT.swapWeapons).forGetter(PetRules::swapWeapons)
    ).apply(instance, (off, stance, swap) -> {
        EnumSet<Toggle> switched = EnumSet.noneOf(Toggle.class);
        switched.addAll(off);
        return new PetRules(switched, stance, swap);
    }));

    /** As sent from the screen to the server. */
    public static final StreamCodec<ByteBuf, PetRules> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public PetRules {
        off = EnumSet.copyOf(off);
    }

    /** @return the kinds of errand the pet is not to do, which is the caller's to keep */
    @Override
    public EnumSet<Toggle> off() {
        return EnumSet.copyOf(off);
    }

    /** @return whether no switch forbids errands of this category; most categories have none */
    public boolean permits(ErrandCategory category) {
        return off.stream().noneMatch(toggle -> toggle.category() == category);
    }

    /** @return these rules with one kind of errand switched off or on */
    public PetRules with(Toggle toggle, boolean allowed) {
        EnumSet<Toggle> switched = off();
        if (allowed) {
            switched.remove(toggle);
        } else {
            switched.add(toggle);
        }
        return new PetRules(switched, stance, swapWeapons);
    }

    /** @return these rules with another stance */
    public PetRules withStance(CombatStance stance) {
        return new PetRules(off, stance, swapWeapons);
    }

    /** @return these rules with swapping weapons on or off */
    public PetRules withSwap(boolean swapWeapons) {
        return new PetRules(off, stance, swapWeapons);
    }
}
