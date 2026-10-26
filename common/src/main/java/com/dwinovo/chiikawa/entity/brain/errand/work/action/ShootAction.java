package com.dwinovo.chiikawa.entity.brain.errand.work.action;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.DrawBow;
import com.dwinovo.chiikawa.entity.brain.errand.step.Release;
import com.dwinovo.chiikawa.entity.brain.errand.step.Skirmish;
import com.dwinovo.chiikawa.entity.brain.errand.step.Stance;
import com.dwinovo.chiikawa.entity.brain.errand.view.AttackTarget;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Fights the foe the attack sensor names with a bow: footwork, and a bow drawn and loosed whenever there is an
 * arrow and the foe is in range. It fights as long as it has arrows; for the foe it keeps to and the leash it is
 * held by, see {@link StrikeAction}.
 */
public final class ShootAction implements WorkAction<Duel, ErrandTunings.Ranged> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "shoot");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Codec<ErrandTunings.Ranged> tuningCodec() {
        return ErrandTunings.Ranged.CODEC;
    }

    @Override
    public Optional<String> refuses(Optional<WorkTarget> target) {
        return WorkTarget.neededBy("shoot", target, WorkTarget.Kind.FOE);
    }

    @Override
    public Offers<Duel> offers(Work<Duel, ErrandTunings.Ranged> work, ResourceLocation errand, PetView view,
            ErrandTunings.Ranged tuning) {
        WorkTarget.Foe target = work.targetOf(WorkTarget.Foe.class);
        Optional<AttackTarget> foe = target.in(view);
        if (foe.isEmpty()) {
            return Offers.none(Check.fail(target.missing()));
        }
        if (!view.hasArrows()) {
            return Offers.none(Check.fail("no_arrows"));
        }
        if (view.attackCoolingDown()) {
            return Offers.none(Check.fail("attack_cooling_down"));
        }
        return Offers.of(Offer.of(new Duel(foe.get().id())));
    }

    /** Between shots the pet goes on, whatever its attack cooldown, for as long as it has arrows. */
    @Override
    public Check stillWanted(Work<Duel, ErrandTunings.Ranged> work, ResourceLocation errand, Duel plan, PetView view,
            ErrandTunings.Ranged tuning) {
        return view.hasArrows() ? Check.OK : Check.fail("no_arrows");
    }

    @Override
    public Routine routine(Work<Duel, ErrandTunings.Ranged> work, Duel plan,
            ErrandSettings<ErrandTunings.Ranged> settings) {
        ErrandTunings.Ranged tuning = settings.tuning();
        Stance stance = new Stance(tuning.footwork(), true);
        return Routine.of(Routine.repeatWhile(Skirmish.whileHunting(plan.foe()),
            () -> Skirmish.shooting(plan.foe(), stance, tuning.range()),
            () -> new DrawBow(plan.foe(), stance, tuning.range(), tuning.drawTicks()),
            () -> new Release(plan.foe(), tuning.cooldownTicks())));
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shared();
    }
}
