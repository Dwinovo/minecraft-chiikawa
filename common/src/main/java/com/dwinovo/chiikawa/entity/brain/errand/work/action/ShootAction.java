package com.dwinovo.chiikawa.entity.brain.errand.work.action;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.LackKey;
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
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;

/**
 * Fights the foe the attack sensor names with a bow: footwork, and a bow drawn and loosed whenever there is an
 * arrow and the foe is in range. It fights as long as it has arrows; for the foe it keeps to and the leash it is
 * held by, see {@link StrikeAction}.
 */
public final class ShootAction implements WorkAction<Duel, ErrandTunings.Ranged> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "shoot");
    /** What an archer is short of when it has nothing to loose. */
    private static final LackKey ARROWS = LackKey.tag(ItemTags.ARROWS);

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Codec<ErrandTunings.Ranged> tuningCodec() {
        return ErrandTunings.Ranged.CODEC;
    }

    @Override
    public Set<LackKey> lacks() {
        return Set.of(ARROWS);
    }

    @Override
    public Optional<String> refuses(Optional<WorkTarget> target) {
        return WorkTarget.neededBy("shoot", target, WorkTarget.Kind.FOE);
    }

    @Override
    public Offers<Duel> offers(Work<Duel, ErrandTunings.Ranged> work, ResourceLocation errand, PetView view,
            ErrandTunings.Ranged tuning) {
        // Arrows first: an archer without any is short of them whether or not there is a foe to shoot at, and says so.
        if (!view.hasArrows()) {
            return Offers.none(Check.lack("no_arrows", ARROWS));
        }
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

    /** Between shots the pet goes on, whatever its attack cooldown, for as long as it has arrows. */
    @Override
    public Check stillWanted(Work<Duel, ErrandTunings.Ranged> work, ResourceLocation errand, Duel plan, PetView view,
            ErrandTunings.Ranged tuning) {
        return view.hasArrows() ? Check.OK : Check.fail("no_arrows");
    }

    @Override
    public Routine routine(Work<Duel, ErrandTunings.Ranged> work, Duel plan,
            ErrandSettings<ErrandTunings.Ranged> settings) {
        return Routine.of(fight(plan.foe(), settings.tuning()));
    }

    /**
     * @param foe the creature the fight is about
     * @param tuning how the pet shoots
     * @return the fight itself: round after round of footwork, a draw and a shot, for as long as the sensor names the foe and it is alive
     */
    public static Routine.Part fight(UUID foe, ErrandTunings.Ranged tuning) {
        Stance stance = new Stance(tuning.footwork(), true);
        return Routine.repeatWhile(Skirmish.whileHunting(foe),
            () -> Skirmish.shooting(foe, stance, tuning.range()),
            () -> new DrawBow(foe, stance, tuning.range(), tuning.drawTicks()),
            () -> new Release(foe, tuning.cooldownTicks()));
    }

    /** The arrows a bow shoots. */
    @Override
    public Predicate<ItemStack> materials() {
        return ProjectileWeaponItem.ARROW_ONLY;
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shared();
    }
}
