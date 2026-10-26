package com.dwinovo.chiikawa.entity.brain.errand.work.action;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.Skirmish;
import com.dwinovo.chiikawa.entity.brain.errand.step.Stance;
import com.dwinovo.chiikawa.entity.brain.errand.step.Swing;
import com.dwinovo.chiikawa.entity.brain.errand.view.AttackTarget;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget;
import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/**
 * Fights the foe the attack sensor names with a sword: footwork, and a swing whenever it can land.
 *
 * <p>The foe is where it goes: a fight starts only on a foe within reach and off attack cooldown, and keeps going
 * between swings while the foe stays within the leash. Past the leash the fight ends and the leash walks the
 * pet back; since a new fight needs the foe back within reach, the pet does not flip between chasing and returning.
 * It keeps to the foe it set off for: when the sensor names another, the fight is over and the next decision begins
 * one with the new foe, and a pet that has put its foe down is done with nothing to rest from.
 *
 * <p>Backing away from a creeper is a fight too, since the foe is what tells the pet to keep its distance.
 */
public final class StrikeAction implements WorkAction<Duel, ErrandTunings.Footwork> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "strike");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Codec<ErrandTunings.Footwork> tuningCodec() {
        return ErrandTunings.Footwork.CODEC;
    }

    @Override
    public Optional<String> refuses(Optional<WorkTarget> target) {
        return WorkTarget.neededBy("strike", target, WorkTarget.Kind.FOE);
    }

    @Override
    public Offers<Duel> offers(Work<Duel, ErrandTunings.Footwork> work, ResourceLocation errand, PetView view,
            ErrandTunings.Footwork tuning) {
        WorkTarget.Foe target = work.targetOf(WorkTarget.Foe.class);
        Optional<AttackTarget> foe = target.in(view);
        if (foe.isEmpty()) {
            return Offers.none(Check.fail(target.missing()));
        }
        if (view.attackCoolingDown()) {
            return Offers.none(Check.fail("attack_cooling_down"));
        }
        return Offers.of(Offer.of(new Duel(foe.get().id())));
    }

    @Override
    public Routine routine(Work<Duel, ErrandTunings.Footwork> work, Duel plan,
            ErrandSettings<ErrandTunings.Footwork> settings) {
        return Routine.of(fight(plan.foe(), settings.tuning()));
    }

    /**
     * @param foe the creature the fight is about
     * @param footwork where the pet stands
     * @return the fight itself: round after round of footwork and a swing, for as long as the sensor names the foe and it is alive
     */
    public static Routine.Part fight(UUID foe, ErrandTunings.Footwork footwork) {
        Stance stance = new Stance(footwork, false);
        return Routine.repeatWhile(Skirmish.whileHunting(foe), () -> Skirmish.meleeing(foe, stance), () -> new Swing(foe));
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shared();
    }
}
