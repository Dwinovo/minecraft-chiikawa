package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.combat.PetCombat;
import com.dwinovo.chiikawa.entity.brain.combat.PetCombat.Band;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Where a pet stands in a fight, the movement half of it: the swinging and the shooting are the steps that use
 * it. Both ways of fighting use it, and what changes between a sword and a bow is only the band it keeps to.
 *
 * <p>Three things can happen on a tick: too close, so back off; too far, so close in; or neither, so stand still
 * and let the other half work. The band is wide enough that standing still is the usual answer, which is what
 * keeps a pet from jittering on a line.
 *
 * <p>Whether the pet has broken off is remembered here, for as long as the fight lasts, because it is the only
 * thing about a fight that has to outlive a tick: the health it broke off at is lower than the health it will
 * come back at, and something has to know which side of that it is on. One for each fight, made with its routine.
 */
public final class Stance {
    private final ErrandTunings.Footwork footwork;
    private final boolean shooting;
    private boolean brokenOff;

    /**
     * @param footwork the speeds and distances of the pet's feet
     * @param shooting whether this is the archer's fight, which is held at a distance
     */
    public Stance(ErrandTunings.Footwork footwork, boolean shooting) {
        this.footwork = footwork;
        this.shooting = shooting;
    }

    /** One tick of footwork: keeps the pet to its band round the foe, looking at what it is fighting. */
    void hold(LiveHandle live, LivingEntity foe) {
        AbstractPet pet = live.pet();
        brokenOff = PetCombat.breakingOff(pet.getHealth() / pet.getMaxHealth(), brokenOff);
        Band band = PetCombat.band(brokenOff, shooting, PetCombat.of(foe, 0.0),
            PetCombat.meleeFar(pet.getBbWidth(), foe.getBbWidth()));
        double distance = pet.distanceTo(foe);

        // Looking at what it is fighting costs nothing and is true in all three cases.
        live.lookAt(foe);
        if (band.tooClose(distance)) {
            backOff(live, foe);
        } else if (band.tooFar(distance)) {
            live.walkTo(foe, footwork.closeInSpeed(), 0);
        } else {
            // In the band: stand and fight. Giving up the walk is what stops the feet.
            live.stopWalking();
        }
    }

    /**
     * Straight away from the foe, and towards the owner when the owner is somewhere safer: a hurt pet running to the
     * person with the food is a better answer than a hurt pet running into the dark.
     *
     * <p>Where to run is worked out rather than rolled for. A random spot has to be committed to or the pet changes
     * its mind twenty times a second on the spot where it is being hit; "directly away from the thing" needs no
     * commitment, because it is the same answer next tick.
     */
    private void backOff(LiveHandle live, LivingEntity foe) {
        AbstractPet pet = live.pet();
        LivingEntity owner = brokenOff ? pet.getOwner() : null;
        // Without the margin a pet fighting at its owner's side "retreats" the two steps to where they are standing,
        // which is not a retreat.
        if (owner != null && owner.level() == pet.level() && owner.distanceTo(foe) > pet.distanceTo(foe) + footwork.saferBy()) {
            live.walkTo(owner, footwork.backOffSpeed(), footwork.ownerArriveDistance());
            live.lookAt(owner);
            return;
        }
        Vec3 away = pet.position().add(pet.position().subtract(foe.position()).normalize().scale(footwork.backOffStep()));
        live.walkTo(away, footwork.backOffSpeed(), 0);
    }
}
