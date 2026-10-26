package com.dwinovo.chiikawa.entity.brain.errand.work.action;

import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;

/**
 * What the pet has set out to fight, in melee or with a bow.
 *
 * @param foe the creature's id
 */
public record Duel(UUID foe) implements Plan {
    @Override
    public Optional<GlobalPos> destination(PetView view) {
        return view.whereIs(foe);
    }
}
