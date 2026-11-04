package com.dwinovo.chiikawa.entity;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What the owner has told a pet it may do, from its screen: whether it picks up what lies on the ground, and how far it takes
 * part in a fight. The rest of what a pet does is the owner's to hold back by other means already, the bag it is given, the
 * camera and the pocket money, so nothing else is a rule. The owner draws the boundary and the pet keeps its own days inside
 * it, so these hold whatever the pet would fancy, see {@link com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints#allows}.
 *
 * <p>Saved with the pet as {@code Rules} and synced to its owner's screen; a key a save lacks is the default, which is how a
 * pet always was: picking up, {@link CombatStance#CLEAR}. Written only by the owner's own hand, see
 * {@link AbstractPet#setRules}.
 *
 * @param pickUp whether the pet picks up what lies on the ground near it, while it is free
 * @param stance how far it takes part in a fight
 */
public record PetRules(boolean pickUp, CombatStance stance) {
    /** How a pet is until its owner says otherwise. */
    public static final PetRules DEFAULT = new PetRules(true, CombatStance.CLEAR);

    /** As saved, and as sent: a key that is missing is the default's. */
    public static final Codec<PetRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("PickUp", DEFAULT.pickUp).forGetter(PetRules::pickUp),
        CombatStance.CODEC.optionalFieldOf("Stance", DEFAULT.stance).forGetter(PetRules::stance)
    ).apply(instance, PetRules::new));

    /** As sent from the screen to the server. */
    public static final StreamCodec<ByteBuf, PetRules> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /** @return whether no rule forbids errands of this category; most categories have none */
    public boolean permits(ErrandCategory category) {
        return pickUp || category != ErrandCategory.PICK_UP;
    }

    /** @return these rules with picking up on or off */
    public PetRules withPickUp(boolean pickUp) {
        return new PetRules(pickUp, stance);
    }

    /** @return these rules with another stance */
    public PetRules withStance(CombatStance stance) {
        return new PetRules(pickUp, stance);
    }
}
