package com.dwinovo.chiikawa.entity.brain.task.idle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.ShufflingList;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Runs one of its behaviours at a time, picked by weight, as vanilla's {@code RunOne} does:
 * the behaviours are shuffled by weight and the first that can start does. The difference
 * is where the weights come from — each is asked of the entity every time a pick is made,
 * so a weight that lives in a data pack takes effect on the next pick after a reload,
 * rather than being fixed when the entity's brain was made.
 *
 * <p>An option weighed at 0 is never picked.
 */
public final class WeightedChoice<E extends LivingEntity> implements BehaviorControl<E> {
    private final List<Option<E>> options;
    private BehaviorControl<? super E> running;

    public WeightedChoice(List<Option<E>> options) {
        this.options = List.copyOf(options);
    }

    @Override
    public Behavior.Status getStatus() {
        return running == null ? Behavior.Status.STOPPED : Behavior.Status.RUNNING;
    }

    /** Every memory any of its behaviours needs, as vanilla's {@code GateBehavior} gathers them. */
    @Override
    public Set<MemoryModuleType<?>> getRequiredMemories() {
        Set<MemoryModuleType<?>> memories = new HashSet<>();
        for (Option<E> option : options) {
            memories.addAll(option.behavior().getRequiredMemories());
        }
        return memories;
    }

    @Override
    public boolean tryStart(ServerLevel level, E entity, long gameTime) {
        ShufflingList<BehaviorControl<? super E>> order = new ShufflingList<>();
        for (Option<E> option : options) {
            int weight = option.weight().applyAsInt(entity);
            if (weight > 0) {
                order.add(option.behavior(), weight);
            }
        }
        for (BehaviorControl<? super E> behavior : (Iterable<BehaviorControl<? super E>>) order.shuffle().stream()::iterator) {
            if (behavior.getStatus() == Behavior.Status.STOPPED && behavior.tryStart(level, entity, gameTime)) {
                running = behavior;
                return true;
            }
        }
        return false;
    }

    @Override
    public void tickOrStop(ServerLevel level, E entity, long gameTime) {
        if (running == null) {
            return;
        }
        if (running.getStatus() == Behavior.Status.RUNNING) {
            running.tickOrStop(level, entity, gameTime);
        }
        if (running.getStatus() == Behavior.Status.STOPPED) {
            running = null;
        }
    }

    @Override
    public void doStop(ServerLevel level, E entity, long gameTime) {
        if (running != null && running.getStatus() == Behavior.Status.RUNNING) {
            running.doStop(level, entity, gameTime);
        }
        running = null;
    }

    @Override
    public String debugString() {
        return getClass().getSimpleName() + (running == null ? "" : ": " + running.debugString());
    }

    /**
     * One behaviour to pick from.
     *
     * @param weight how likely it is to be picked, asked of the entity at each pick
     */
    public record Option<E extends LivingEntity>(BehaviorControl<? super E> behavior, ToIntFunction<? super E> weight) {
    }
}
