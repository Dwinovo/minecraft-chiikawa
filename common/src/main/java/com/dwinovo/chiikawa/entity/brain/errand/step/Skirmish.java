package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * The footwork of a fight, for as long as there is nothing to strike with yet: keeps the pet to its band round the
 * foe, see {@link Stance}, and is done the tick the pet can strike, which the steps that follow it do, or when the
 * attack sensor no longer names the foe the fight is about, which the routine takes for the end of the fight. Always
 * interruptible.
 *
 * <p>The foe is fixed when the fight starts. When the sensor names another, this fight is over and the next decision
 * starts one with the new foe, so a plan never has to be followed to a foe it was not made for.
 */
public final class Skirmish implements Step {
    private final UUID foe;
    private final Stance stance;
    private final BiPredicate<AbstractPet, LivingEntity> ready;

    private Skirmish(UUID foe, Stance stance, BiPredicate<AbstractPet, LivingEntity> ready) {
        this.foe = foe;
        this.stance = stance;
        this.ready = ready;
    }

    /**
     * @param foe the creature the fight is about
     * @param stance where the pet stands
     * @return the footwork of a fight with a sword, done when the pet can swing
     */
    public static Skirmish meleeing(UUID foe, Stance stance) {
        return new Skirmish(foe, stance, Swing::canSwing);
    }

    /**
     * @param foe the creature the fight is about
     * @param stance where the pet stands
     * @param range how far away a shot is still worth taking
     * @return the footwork of a fight with a bow, done when the pet can draw
     */
    public static Skirmish shooting(UUID foe, Stance stance, double range) {
        return new Skirmish(foe, stance, (pet, target) -> DrawBow.canDraw(pet, target, range));
    }

    /** @return whether the fight about that foe goes on: the sensor still names it, and it is still there */
    public static Predicate<Handle> whileHunting(UUID foe) {
        return handle -> hunted(LiveHandle.of(handle), foe).isPresent();
    }

    /** @return the foe, while the attack sensor still names it and it is alive */
    static Optional<LivingEntity> hunted(LiveHandle live, UUID foe) {
        return live.pet().getBrain().getMemory(MemoryModuleType.ATTACK_TARGET)
            .filter(target -> target.isAlive() && target.getUUID().equals(foe));
    }

    @Override
    public void begin(Handle handle) {
        // So that the feet are set off in the tick the fight starts, before the sinks run.
        hunted(LiveHandle.of(handle), foe).ifPresent(target -> stance.hold(LiveHandle.of(handle), target));
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        Optional<LivingEntity> target = hunted(live, foe);
        if (target.isEmpty()) {
            return StepStatus.DONE;
        }
        stance.hold(live, target.get());
        return ready.test(live.pet(), target.get()) ? StepStatus.DONE : StepStatus.RUNNING;
    }

    @Override
    public boolean holdsGaze() {
        return true;
    }
}
