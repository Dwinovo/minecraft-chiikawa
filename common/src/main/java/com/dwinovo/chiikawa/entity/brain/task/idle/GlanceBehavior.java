package com.dwinovo.chiikawa.entity.brain.task.idle;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

/**
 * Looks at whoever of a kind is nearest, for as long as the pet's habit says. Picks its
 * target as vanilla's {@code SetEntityLookTarget} does, from who the pet can see, and
 * points the pet's look at them through the same {@code LOOK_TARGET} memory, so the head
 * turns the way it does for anything else the pet looks at. Unlike vanilla's, it keeps
 * the look up for the habit's time rather than the look sink's own, and lets it go when
 * they walk off.
 */
public final class GlanceBehavior extends Behavior<AbstractPet> {
    private final Predicate<LivingEntity> kind;
    private final Function<IdleHabits, IdleHabits.Glance> habit;
    private LivingEntity target;
    private float range;
    private long until;

    /**
     * @param kind who this glance is for
     * @param habit which of the pet's habits says how near and how long
     */
    public GlanceBehavior(Predicate<LivingEntity> kind, Function<IdleHabits, IdleHabits.Glance> habit) {
        super(Map.of(
            MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT), IdleHabits.LONGEST_TICKS);
        this.kind = kind;
        this.habit = habit;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet pet) {
        range = habit.apply(PetPersonalities.of(pet.getType()).idle()).range();
        target = pet.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            .flatMap(seen -> seen.findClosest(entity -> kind.test(entity) && inRange(pet, entity)))
            .orElse(null);
        return target != null;
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        until = gameTime + habit.apply(PetPersonalities.of(pet.getType()).idle()).sampleTicks(pet.getRandom());
        lookAtTarget(pet);
    }

    /** The look sink lets a look go after a while of its own; this puts it back until the habit's time is up. */
    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        if (!pet.getBrain().hasMemoryValue(MemoryModuleType.LOOK_TARGET)) {
            lookAtTarget(pet);
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return gameTime < until && target.isAlive() && target.level() == pet.level() && inRange(pet, target);
    }

    @Override
    protected void stop(ServerLevel level, AbstractPet pet, long gameTime) {
        LivingEntity lookedAt = target;
        pet.getBrain().getMemory(MemoryModuleType.LOOK_TARGET)
            .filter(look -> look instanceof EntityTracker tracker && tracker.getEntity() == lookedAt)
            .ifPresent(look -> pet.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET));
        target = null;
    }

    private void lookAtTarget(AbstractPet pet) {
        pet.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
    }

    private boolean inRange(AbstractPet pet, LivingEntity entity) {
        return pet.distanceToSqr(entity) <= range * range;
    }
}
