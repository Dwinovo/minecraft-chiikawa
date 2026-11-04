package com.dwinovo.chiikawa.entity;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;

/**
 * What the owner has told a pet it may do, from its screen: whether it picks up what lies on the ground. The rest of what a
 * pet does is the owner's to hold back by other means already, the bag it is given, the weapon in its hand or bag, the camera
 * and the pocket money, so nothing else is a rule. The owner draws the boundary and the pet keeps its own days inside it, so
 * these hold whatever the pet would fancy, see {@link com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints#allows}.
 *
 * <p>Saved with the pet as {@code Rules} and synced to its owner's screen; a key a save lacks is the default, which is how a
 * pet always was: picking up. Written only by the owner's own hand, see {@link AbstractPet#setRules}.
 *
 * @param pickUp whether the pet picks up what lies on the ground near it, while it is free
 */
public record PetRules(boolean pickUp) {
    /** How a pet is until its owner says otherwise. */
    public static final PetRules DEFAULT = new PetRules(true);

    /** As saved, and as sent: a key that is missing is the default's. */
    public static final Codec<PetRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ExtraCodecs.strictOptionalField(Codec.BOOL, "PickUp", DEFAULT.pickUp).forGetter(PetRules::pickUp)
    ).apply(instance, PetRules::new));

    /** The most the server reads of what the screen sends, in bytes. */
    private static final long MOST_SENT = 2097152L;

    /** @return the rules as the screen sent them to the server, as the codec reads them */
    public static PetRules read(FriendlyByteBuf buffer) {
        return buffer.readWithCodec(NbtOps.INSTANCE, CODEC, NbtAccounter.create(MOST_SENT));
    }

    /** Sends these rules from the screen to the server. */
    public void write(FriendlyByteBuf buffer) {
        buffer.writeWithCodec(NbtOps.INSTANCE, CODEC, this);
    }

    /** @return whether no rule forbids errands of this category; most categories have none */
    public boolean permits(ErrandCategory category) {
        return pickUp || category != ErrandCategory.PICK_UP;
    }

    /** @return these rules with picking up on or off */
    public PetRules withPickUp(boolean pickUp) {
        return new PetRules(pickUp);
    }
}
