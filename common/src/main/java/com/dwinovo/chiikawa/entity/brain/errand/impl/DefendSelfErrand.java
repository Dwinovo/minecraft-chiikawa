package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.combat.Arms;
import com.dwinovo.chiikawa.entity.brain.combat.Ground;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.SwapToWeapon;
import com.dwinovo.chiikawa.entity.brain.errand.view.AttackTarget;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.work.action.ShootAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.action.StrikeAction;
import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Answers an attack with a weapon from the bag. A pet that is hit, or whose owner is, and holds no weapon but has one in its
 * bag that its owner lets it take up, takes it up, fights with it as a fencer or an archer does, and takes its tool back
 * when the fight is over or off. Nothing here is saved: the weapon is in the hand only for as long as the errand is, see
 * {@link SwapToWeapon}.
 *
 * <p>It is an errand of every pet and starts only for a pet without a weapon in hand, but goes on once it has one: the swap
 * changes the pet's job, and the errand is not the job's, so that does not end it. It ends as the fight does, because the
 * sensor names no foe any more, or the owner's orders change, or something of a higher priority takes the pet.
 */
public final class DefendSelfErrand implements ErrandType<DefendSelfErrand.Defence, ErrandTunings.Defend> {
    /**
     * What the pet has set out to do.
     *
     * @param foe the creature that attacks, which the fight is about
     * @param weapon the weapon it means to take up
     */
    public record Defence(UUID foe, Arms.Weapon weapon) implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return view.whereIs(foe);
        }
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.DEFEND_SELF;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.COMBAT;
    }

    @Override
    public Codec<ErrandTunings.Defend> tuningCodec() {
        return ErrandTunings.Defend.CODEC;
    }

    @Override
    public Offers<Defence> offers(PetView view, ErrandTunings.Defend tuning) {
        Optional<AttackTarget> attacker = view.targets().attackTarget()
            .filter(foe -> foe.grounds().contains(Ground.SELF) || foe.grounds().contains(Ground.OWNER));
        if (attacker.isEmpty()) {
            return Offers.none(Check.fail("no_attacker"));
        }
        Arms arms = view.arms();
        if (arms.wielding()) {
            return Offers.none(Check.fail("armed"));
        }
        return arms.swap().<Offers<Defence>>map(weapon -> Offers.of(Offer.of(new Defence(attacker.get().id(), weapon))))
            .orElseGet(() -> Offers.none(Check.fail("no_spare_weapon")));
    }

    @Override
    public Routine routine(Defence plan, ErrandSettings<ErrandTunings.Defend> settings) {
        ErrandTunings.Defend tuning = settings.tuning();
        Routine.Part fight = plan.weapon().bow() ? ShootAction.fight(plan.foe(), tuning.bow())
            : StrikeAction.fight(plan.foe(), tuning.sword());
        return Routine.of(Routine.step(() -> new SwapToWeapon(plan.weapon())), fight).releasing(SwapToWeapon::swapBack);
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shared();
    }
}
