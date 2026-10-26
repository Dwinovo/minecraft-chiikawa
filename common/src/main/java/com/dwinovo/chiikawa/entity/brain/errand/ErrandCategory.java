package com.dwinovo.chiikawa.entity.brain.errand;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * What kind of thing an errand does. The owner's directive permits or forbids
 * whole categories; see {@link com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints#allows}.
 */
public enum ErrandCategory implements StringRepresentable {
    FOLLOW_OWNER,
    STAY,
    WANDER,
    WORK,
    /** Gathering what grows in the wild, such as weeds and mushrooms. */
    FORAGE,
    COMBAT,
    PICK_UP,
    /** Walking to a labor board to take a slip; on the way while following. */
    TAKE_TASK,
    /** Walking to a shop to spend what the pet has earned. */
    SHOP,
    /** Taking the owner something the pet bought for them. */
    GIFT,
    /**
     * Playing a little scene with another pet, either side of it. Only where a pet may
     * wander, so a following pet only does it while its owner is close by.
     */
    SOCIAL,
    /**
     * Sitting a licence exam at a labor board, and going back to see the results. Where a
     * slip is taken: on the way while following, anywhere while free.
     */
    EXAM,
    /** A wild pet going over to see who whistled. Wild pets only: an owner's pet takes the order itself. */
    CURIOUS,
    /** Taking a photo of the owner, a friend or the view, where the pet may walk. */
    PHOTO;

    /** The category as a work file names it. */
    public static final Codec<ErrandCategory> CODEC = StringRepresentable.fromEnum(ErrandCategory::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
