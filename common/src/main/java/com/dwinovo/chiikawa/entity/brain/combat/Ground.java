package com.dwinovo.chiikawa.entity.brain.combat;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * Why a pet fights a foe: the grounds that a vanilla wolf's target goals give, one for each. What a fight is
 * about decides which errand takes it, see {@link com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget.Foe}, and so
 * how much it matters: standing up for itself and its owner outranks clearing out a patch it has been let loose in.
 */
public enum Ground implements StringRepresentable {
    /** It hurt the pet, or has picked the pet out, as the wolf's {@code HurtByTargetGoal} has it. */
    SELF,
    /** It is about the pet's owner: it hit them, they are hitting it or it has picked them out, as the wolf's owner goals have it. */
    OWNER,
    /** It is hostile and near, and the pet is at its owner's side or in the wild, where it deals with what comes by. */
    NEARBY,
    /** It is hostile and near, and the pet is let loose in its area, where it hunts. */
    HUNT;

    /** By lower-case name, as an errand file names a ground. */
    public static final Codec<Ground> CODEC = StringRepresentable.fromEnum(Ground::values);

    /** @return whether a fight on this ground is one of defence, which comes before the hunt */
    public boolean guarding() {
        return this != HUNT;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
