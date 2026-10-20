package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.GlobalPos;

/**
 * The plan of an errand whose behavior picks its own way and only needs saying where it heads: the
 * crop it works on, the shop, the owner. Worked out from the pet as it is each time it is asked, as
 * the behavior goes by what the pet remembers and not by a choice made once.
 *
 * @param where where the errand takes the pet, if anywhere in particular
 */
public record Heading(Function<PetView, Optional<GlobalPos>> where) implements Plan {
    /** For an errand that goes nowhere in particular: staying, wandering, playing where it stands. */
    public static final Heading NOWHERE = new Heading(view -> Optional.empty());

    @Override
    public Optional<GlobalPos> destination(PetView view) {
        return where.apply(view);
    }
}
