package com.dwinovo.chiikawa.entity.brain.task.combat;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.combat.PetCombat;
import com.dwinovo.chiikawa.entity.brain.combat.PetCombat.Band;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.phys.Vec3;

/**
 * Where a pet stands in a fight — the movement half of it, with the swinging left to
 * whatever else the activity is running. Both jobs use it: what changes between a sword
 * and a bow is only the band it keeps to.
 *
 * <p>Three things can happen on a tick: too close, so back off; too far, so close in;
 * or neither, so stand still and let the other half work. The band is wide enough that
 * standing still is the usual answer, which is what keeps a pet from jittering on a line.
 *
 * <p>Whether the pet has broken off is remembered here, one flag per pet, because it is
 * the only thing about a fight that has to outlive a tick: the health it broke off at is
 * lower than the health it will come back at, and something has to know which side of
 * that it is on.
 */
public class CombatMoveBehavior extends Behavior<AbstractPet> {
    private static final Map<MemoryModuleType<?>, MemoryStatus> REQUIRED_MEMORIES = ImmutableMap.of(
        MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT,
        MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
        MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
    );
    /** Walking in. */
    private static final float CLOSE_IN_SPEED = 1.0F;
    /** Walking out, which is worth doing in more of a hurry than walking in. */
    private static final float BACK_OFF_SPEED = 1.2F;
    /** How far away to aim when backing off. */
    private static final double BACK_OFF_STEP = 10.0;
    /**
     * How much farther from the trouble the owner has to be before running to them counts
     * as getting away from it. Without the margin a pet fighting at its owner's side
     * "retreats" the two steps to where they are standing, which is not a retreat.
     */
    private static final double SAFER_BY = 3.0;

    private final boolean shooting;
    private boolean brokenOff;

    /**
     * @param shooting whether this is the archer's fight, which is held at a distance
     */
    public CombatMoveBehavior(boolean shooting) {
        // Runs for as long as its activity does; the combat intent ends it.
        super(REQUIRED_MEMORIES, Integer.MAX_VALUE);
        this.shooting = shooting;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return pet.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        tick(level, pet, gameTime);
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        LivingEntity foe = pet.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (foe == null) {
            return;
        }
        brokenOff = PetCombat.breakingOff(pet.getHealth() / pet.getMaxHealth(), brokenOff);
        Band band = PetCombat.band(brokenOff, shooting, PetCombat.of(foe, 0.0),
            PetCombat.meleeFar(pet.getBbWidth(), foe.getBbWidth()));
        double distance = pet.distanceTo(foe);

        // Looking at what it is fighting costs nothing and is true in all three cases.
        pet.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(foe, true));
        if (band.tooClose(distance)) {
            backOff(pet, foe);
        } else if (band.tooFar(distance)) {
            BehaviorUtils.setWalkAndLookTargetMemories(pet, foe, CLOSE_IN_SPEED, 0);
        } else {
            // In the band: stand and fight. Erasing the walk target is what stops the feet.
            pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }

    @Override
    protected void stop(ServerLevel level, AbstractPet pet, long gameTime) {
        pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    /**
     * Straight away from the foe, and towards the owner when the owner is somewhere
     * safer: a hurt pet running to the person with the food is a better answer than a
     * hurt pet running into the dark.
     *
     * <p>Where to run is worked out rather than rolled for. A random spot has to be
     * committed to or the pet changes its mind twenty times a second on the spot where it
     * is being hit; "directly away from the thing" needs no commitment, because it is the
     * same answer next tick.
     */
    private void backOff(AbstractPet pet, LivingEntity foe) {
        LivingEntity owner = brokenOff ? pet.getOwner() : null;
        if (owner != null && owner.level() == pet.level()
                && owner.distanceTo(foe) > pet.distanceTo(foe) + SAFER_BY) {
            BehaviorUtils.setWalkAndLookTargetMemories(pet, owner, BACK_OFF_SPEED, 2);
            return;
        }
        Vec3 away = pet.position()
            .add(pet.position().subtract(foe.position()).normalize().scale(BACK_OFF_STEP));
        pet.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(away, BACK_OFF_SPEED, 0));
    }
}
