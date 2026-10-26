package com.dwinovo.chiikawa.entity.brain.sensor;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetTargeting;
import com.dwinovo.chiikawa.entity.brain.combat.Arms;
import com.dwinovo.chiikawa.entity.brain.combat.Ground;
import com.dwinovo.chiikawa.entity.brain.combat.PetCombat;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget;
import com.dwinovo.chiikawa.entity.job.JobWork;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.utils.Utils;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.phys.AABB;

/**
 * What a fighting pet has its eye on, and why.
 *
 * <p>A foe is on the list for a {@link Ground}, and which grounds the pet fights on is the owner's orders, see
 * {@link FoeCandidates}. As a vanilla wolf has it, it is <b>what hurt the pet</b>, <b>what is about its owner</b> (what hit
 * them, what is lining them up, and what they are hitting themselves) and <b>what is hostile and near</b>. A pet that walks
 * beside its owner through a fight and watches is not a pet anyone wants, and until now that is what one at heel did.
 *
 * <p>Which of them to fight is {@link PetCombat#pick}: the one that is about defence before the one that is only a hunt,
 * the nearest, but the one already being fought is kept while it is worth keeping, so a pet in a crowd finishes something
 * instead of turning towards whatever drifted closest.
 *
 * <p>This is the only thing that writes the attack target, whatever the pet's job and what it is doing: one that is gone, or
 * that the pet may not attack, or that it has no ground to fight now, is not on the list and so is dropped for the next, or
 * for none. The fights read it and never write it, and a fight is about the foe it set off for, and over when the sensor
 * names another. A pet whose job has no fight in it looks only if it has a weapon to take up to defend itself with.
 */
public class PetAttackbleEntitySensor extends Sensor<AbstractPet> {
    /**
     * How often a pet looks around. It used to be every three seconds, which is four
     * zombie hits on its owner before the pet so much as turned round.
     */
    private static final int SCAN_TICKS = 10;
    /** How far it looks, and how far it keeps looking at what it found. */
    private static final double SEARCH_RANGE = 15.0;

    public PetAttackbleEntitySensor() {
        super(SCAN_TICKS);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleType.ATTACK_TARGET,
            MemoryModuleType.HURT_BY_ENTITY
        );
    }

    @Override
    protected void doTick(ServerLevel level, AbstractPet pet) {
        // Only a pet whose job has work against a foe, or that has a weapon to take up, ever acts on attack targets, so
        // skip the scan for other pets, and leave them none. Whether a pet may fight right now is up to its errands.
        boolean fights = fights(pet);
        Set<Ground> allowed = allowed(pet, fights);
        if (allowed.isEmpty() || !(fights || Arms.of(pet).swap().isPresent())) {
            pet.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            return;
        }

        List<LivingEntity> candidates = candidates(level, pet, allowed);
        List<PetCombat.Foe> foes = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            LivingEntity candidate = candidates.get(i);
            boolean guarding = groundsOf(pet, candidate, allowed).stream().anyMatch(Ground::guarding);
            foes.add(PetCombat.of(candidate, pet.distanceTo(candidate), guarding));
        }
        OptionalInt chosen = PetCombat.pick(foes, keptTargetId(pet), hasArrows(pet));
        if (chosen.isEmpty()) {
            pet.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            return;
        }
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).getId() == chosen.getAsInt()) {
                pet.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, candidates.get(i));
                return;
            }
        }
    }

    /** @return whether the pet's job has work that is done to a foe */
    private static boolean fights(AbstractPet pet) {
        return JobWork.hasWorkTo(pet, WorkTarget.Kind.FOE);
    }

    /** @return the grounds the pet's orders let it fight on now, as the sensor and the pet's view are to agree on them */
    public static Set<Ground> allowed(AbstractPet pet) {
        return allowed(pet, fights(pet));
    }

    private static Set<Ground> allowed(AbstractPet pet, boolean fights) {
        return PetConstraints.foesOf(pet, PetOwnership.of(pet), fights);
    }

    /**
     * Everything the pet may fight right now, in no particular order: what it is already
     * fighting, what hurt it, its owner's business, and the hostiles around it, each as far as the grounds allow.
     */
    private static List<LivingEntity> candidates(ServerLevel level, AbstractPet pet, Set<Ground> allowed) {
        Set<LivingEntity> found = new LinkedHashSet<>();
        pet.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> offer(pet, found, target, allowed));
        // Whoever hurt us, unless it is our own side — the owner's sword sweep, a sibling's
        // arrow — in which case the grudge is dropped rather than carried.
        pet.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY).ifPresent(hurtBy -> {
            if (fightable(pet, hurtBy)) {
                offer(pet, found, hurtBy, allowed);
            } else {
                pet.getBrain().eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
            }
        });
        ownersBusiness(pet).forEach(foe -> offer(pet, found, foe, allowed));

        AABB around = pet.getBoundingBox().inflate(SEARCH_RANGE);
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, around, entity -> entity != pet)) {
            offer(pet, found, nearby, allowed);
        }
        return List.copyOf(found);
    }

    /**
     * What its owner is caught up in: what hit them, and what they hit. Both are what a
     * pet is for — one is defending them, the other is helping.
     */
    private static List<LivingEntity> ownersBusiness(AbstractPet pet) {
        LivingEntity owner = ownerNear(pet);
        if (owner == null) {
            return List.of();
        }
        List<LivingEntity> theirs = new ArrayList<>(2);
        LivingEntity hurtThem = owner.getLastHurtByMob();
        if (hurtThem != null) {
            theirs.add(hurtThem);
        }
        LivingEntity theyHit = owner.getLastHurtMob();
        if (theyHit != null) {
            theirs.add(theyHit);
        }
        return theirs;
    }

    /** @return the pet's owner, while they are in the pet's world and within the pet's sight of them */
    private static LivingEntity ownerNear(AbstractPet pet) {
        LivingEntity owner = pet.getOwner();
        return owner == null || owner.level() != pet.level() || owner.distanceTo(pet) > SEARCH_RANGE ? null : owner;
    }

    private static void offer(AbstractPet pet, Set<LivingEntity> found, LivingEntity candidate, Set<Ground> allowed) {
        if (fightable(pet, candidate) && !groundsOf(pet, candidate, allowed).isEmpty()) {
            found.add(candidate);
        }
    }

    /**
     * @return whether the pet may fight it, and it is there to be fought: alive, within the search range and not on the
     *         pet's own side. What the sensor lists and what the pet's view shows of what it remembers is the same
     *         question, so a foe the pet has been taken away from is no longer shown, though the sensor looks again
     *         only every half second
     */
    public static boolean fightable(AbstractPet pet, LivingEntity foe) {
        return foe.isAlive() && foe.distanceTo(pet) <= SEARCH_RANGE && PetTargeting.canTarget(pet, foe);
    }

    /**
     * @return the grounds the pet has to fight this foe now: those of the grounds its orders allow that the foe is on;
     *         none for one it has no ground for
     */
    public static Set<Ground> groundsOf(AbstractPet pet, LivingEntity foe) {
        return groundsOf(pet, foe, allowed(pet));
    }

    private static Set<Ground> groundsOf(AbstractPet pet, LivingEntity foe, Set<Ground> allowed) {
        EnumSet<Ground> on = EnumSet.noneOf(Ground.class);
        if (pet.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY).filter(foe::equals).isPresent() || after(foe, pet)) {
            on.add(Ground.SELF);
        }
        LivingEntity owner = ownerNear(pet);
        if (owner != null && (owner.getLastHurtByMob() == foe || owner.getLastHurtMob() == foe) || after(foe, pet.getOwner())) {
            on.add(Ground.OWNER);
        }
        if (foe.getType().is(InitTag.ENTITY_HOSTILE_ENTITY)) {
            on.add(Ground.NEARBY);
            on.add(Ground.HUNT);
        }
        on.retainAll(allowed);
        return on;
    }

    /** The id of what the pet is fighting, so the choice can stay with it. */
    private static int keptTargetId(AbstractPet pet) {
        Optional<LivingEntity> target = pet.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        return target.map(LivingEntity::getId).orElse(PetCombat.NO_FOE);
    }

    /** Whether this thing has picked somebody out — the pet itself, or its owner. */
    private static boolean after(LivingEntity entity, LivingEntity who) {
        return who != null && entity instanceof Mob mob && mob.getTarget() == who;
    }

    /** Whether the pet can answer a lit creeper from outside the blast. */
    private static boolean hasArrows(AbstractPet pet) {
        ItemStack held = pet.getMainHandItem();
        return held.getItem() instanceof ProjectileWeaponItem && !Utils.getArrow(pet).isEmpty();
    }
}
