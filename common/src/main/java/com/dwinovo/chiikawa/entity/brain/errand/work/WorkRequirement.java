package com.dwinovo.chiikawa.entity.brain.errand.work;

import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.util.StringRepresentable;

/**
 * An extra start condition a work is built with, checked before its action: {@code "requires": ["at_night"]}.
 * Checked again while the errand goes on, since it is a condition of doing the work and not only of setting off.
 */
public enum WorkRequirement implements StringRepresentable {
    /**
     * A wild pet does this on its own; an owned pet only while it carries a slip that counts the work, since on its
     * own it would strip the grass and mushrooms its owner keeps around the base. The work must have a counter.
     */
    WILD_OR_SLIP,
    /** Only done at night. */
    AT_NIGHT;

    public static final Codec<WorkRequirement> CODEC = StringRepresentable.fromEnum(WorkRequirement::values);

    /**
     * @param work the work this is a requirement of
     * @param view the pet
     * @return whether it holds, and if not, why
     */
    public Check check(Work<?, ?> work, PetView view) {
        return switch (this) {
            case WILD_OR_SLIP -> view.ownership() instanceof PetOwnership.Wild || work.countedBy(view)
                ? Check.OK : Check.fail("needs_slip");
            case AT_NIGHT -> view.phase() == DayPhase.NIGHT ? Check.OK : Check.fail("not_night");
        };
    }

    /**
     * @param work the work this is a requirement of
     * @return what is wrong with it on that work, or nothing
     */
    Optional<String> problemWith(Work<?, ?> work) {
        return this == WILD_OR_SLIP && work.counter().isEmpty()
            ? Optional.of("requires wild_or_slip, which needs a counter") : Optional.empty();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
