package com.dwinovo.chiikawa.entity.brain.sensor;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetTargeting;
import com.dwinovo.chiikawa.entity.brain.combat.PetCombat;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.utils.Utils;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
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
 * What a fighting pet has its eye on.
 *
 * <p>Three things put something on the list: it is hostile and nearby, it hurt the pet, or
 * <b>it is about its owner</b> — what hit them, what is lining them up, and what they are
 * hitting themselves. A pet that walks beside its owner through a fight and watches is not
 * a pet anyone wants, and until now that is what one at heel did.
 *
 * <p>Which of them to fight is {@link PetCombat#pick}: the nearest, but the one already
 * being fought is kept while it is worth keeping, so a pet in a crowd finishes something
 * instead of turning towards whatever drifted closest.
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
        // Only fencers and archers ever act on attack targets, so skip the scan for
        // other jobs. Whether a pet may fight right now is up to its intents.
        int jobId = pet.getPetJobId();
        if (jobId != InitRegistry.FENCER_ID && jobId != InitRegistry.ARCHER_ID) {
            return;
        }

        List<LivingEntity> candidates = candidates(level, pet);
        List<PetCombat.Foe> foes = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            LivingEntity candidate = candidates.get(i);
            foes.add(PetCombat.of(candidate, pet.distanceTo(candidate)));
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

    /**
     * Everything the pet may fight right now, in no particular order: what it is already
     * fighting, what hurt it, its owner's business, and the hostiles around it.
     */
    private static List<LivingEntity> candidates(ServerLevel level, AbstractPet pet) {
        Set<LivingEntity> found = new LinkedHashSet<>();
        pet.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> offer(pet, found, target));
        // Whoever hurt us, unless it is our own side — the owner's sword sweep, a sibling's
        // arrow — in which case the grudge is dropped rather than carried.
        pet.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY).ifPresent(hurtBy -> {
            if (!offer(pet, found, hurtBy)) {
                pet.getBrain().eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
            }
        });
        ownersBusiness(pet).forEach(foe -> offer(pet, found, foe));

        AABB around = pet.getBoundingBox().inflate(SEARCH_RANGE);
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, around, entity -> entity != pet)) {
            if (hostile(nearby) || after(nearby, pet) || after(nearby, pet.getOwner())) {
                offer(pet, found, nearby);
            }
        }
        return List.copyOf(found);
    }

    /**
     * What its owner is caught up in: what hit them, and what they hit. Both are what a
     * pet is for — one is defending them, the other is helping.
     */
    private static List<LivingEntity> ownersBusiness(AbstractPet pet) {
        LivingEntity owner = pet.getOwner();
        if (owner == null || owner.level() != pet.level() || owner.distanceTo(pet) > SEARCH_RANGE) {
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

    /** @return whether the candidate was worth adding */
    private static boolean offer(AbstractPet pet, Set<LivingEntity> found, LivingEntity candidate) {
        if (!candidate.isAlive() || candidate.distanceTo(pet) > SEARCH_RANGE
                || !PetTargeting.canTarget(pet, candidate)) {
            return false;
        }
        found.add(candidate);
        return true;
    }

    /** The id of what the pet is fighting, so the choice can stay with it. */
    private static int keptTargetId(AbstractPet pet) {
        Optional<LivingEntity> target = pet.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        return target.map(LivingEntity::getId).orElse(PetCombat.NO_FOE);
    }

    private static boolean hostile(LivingEntity entity) {
        return entity.getType().is(InitTag.ENTITY_HOSTILE_ENTITY);
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
