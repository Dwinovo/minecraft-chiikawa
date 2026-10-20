package com.dwinovo.chiikawa.entity.brain.errand.impl;

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
import com.dwinovo.chiikawa.entity.brain.errand.step.DrawBow;
import com.dwinovo.chiikawa.entity.brain.errand.step.Release;
import com.dwinovo.chiikawa.entity.brain.errand.step.Skirmish;
import com.dwinovo.chiikawa.entity.brain.errand.step.Stance;
import com.dwinovo.chiikawa.entity.brain.errand.step.Swing;
import com.dwinovo.chiikawa.entity.brain.errand.view.AttackTarget;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Fights the foe the attack sensor names, in melee or with a bow: the two are one errand type made twice,
 * differing in the number of their tuning and the steps they go round.
 *
 * <p>The foe is where it goes: a fight starts only on a foe within reach and off attack cooldown, and keeps going
 * between swings or shots while the foe stays within the leash. Past the leash the fight ends and the leash walks the
 * pet back; since a new fight needs the foe back within reach, the pet does not flip between chasing and returning.
 * It keeps to the foe it set off for: when the sensor names another, the fight is over and the next decision begins
 * one with the new foe, and a pet that has put its foe down is done with nothing to rest from.
 *
 * <p>An archer fights as long as it has arrows. Backing away from a creeper is a fight too, since the foe is
 * what tells the pet to keep its distance.
 *
 * @param <T> the shape of its tuning
 */
public final class CombatErrand<T> implements ErrandType<CombatErrand.Duel, T> {
    /**
     * What the pet has set out to fight.
     *
     * @param foe the creature's id
     */
    public record Duel(UUID foe) implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return view.whereIs(foe);
        }
    }

    private final ResourceLocation id;
    private final Codec<T> tuning;
    private final boolean needsArrows;
    private final BiFunction<Duel, ErrandSettings<T>, Routine> routine;

    private CombatErrand(ResourceLocation id, Codec<T> tuning, boolean needsArrows,
            BiFunction<Duel, ErrandSettings<T>, Routine> routine) {
        this.id = id;
        this.tuning = tuning;
        this.needsArrows = needsArrows;
        this.routine = routine;
    }

    /** @return fighting with a sword: footwork, and a swing whenever it can land */
    public static CombatErrand<ErrandTunings.Footwork> fencing() {
        return new CombatErrand<>(ErrandIds.MELEE, ErrandTunings.Footwork.CODEC, false, (duel, settings) -> {
            Stance stance = new Stance(settings.tuning(), false);
            return Routine.of(Routine.repeatWhile(Skirmish.whileHunting(duel.foe()),
                () -> Skirmish.meleeing(duel.foe(), stance), () -> new Swing(duel.foe())));
        });
    }

    /** @return fighting with a bow: footwork, and a bow drawn and loosed whenever there is an arrow and the foe is in range */
    public static CombatErrand<ErrandTunings.Ranged> shooting() {
        return new CombatErrand<>(ErrandIds.RANGED, ErrandTunings.Ranged.CODEC, true, (duel, settings) -> {
            ErrandTunings.Ranged tuning = settings.tuning();
            Stance stance = new Stance(tuning.footwork(), true);
            return Routine.of(Routine.repeatWhile(Skirmish.whileHunting(duel.foe()),
                () -> Skirmish.shooting(duel.foe(), stance, tuning.range()),
                () -> new DrawBow(duel.foe(), stance, tuning.range(), tuning.drawTicks()),
                () -> new Release(duel.foe(), tuning.cooldownTicks())));
        });
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.COMBAT;
    }

    @Override
    public Codec<T> tuningCodec() {
        return tuning;
    }

    @Override
    public Offers<Duel> offers(PetView view, T tuning) {
        Optional<AttackTarget> foe = view.targets().attackTarget();
        if (foe.isEmpty()) {
            return Offers.none(Check.fail("no_attack_target"));
        }
        if (needsArrows && !view.hasArrows()) {
            return Offers.none(Check.fail("no_arrows"));
        }
        if (view.attackCoolingDown()) {
            return Offers.none(Check.fail("attack_cooling_down"));
        }
        return Offers.of(Offer.of(new Duel(foe.get().id())));
    }

    /** Between swings the pet goes on, whatever its attack cooldown, for as long as an archer has arrows. */
    @Override
    public Check stillWanted(Duel plan, PetView view, T tuning) {
        return needsArrows && !view.hasArrows() ? Check.fail("no_arrows") : Check.OK;
    }

    @Override
    public Routine routine(Duel plan, ErrandSettings<T> settings) {
        return routine.apply(plan, settings);
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shared();
    }
}
