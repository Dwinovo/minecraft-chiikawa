package com.dwinovo.chiikawa.entity.brain;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.mixin.BrainAccessor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

/**
 * The single entry point for adding activities to a pet brain.
 *
 * <p>Activities get no memory requirements: whether an activity may run is decided
 * entirely by its intent, so the selector can always enter the activity of the
 * intent it picked. Only the selector switches activities.
 */
public final class PetActivities {
    private PetActivities() {
    }

    /**
     * The name to give an activity: {@code chiikawa:<path>}, as it is registered. Vanilla
     * counts two activities as one when their names match, and Forge's registry refuses an
     * object equal to one it already holds, so a bare name another mod also uses - Fowl
     * Play's {@code pick_up} - stopped the game from starting with both installed. (Each
     * loader makes the activity itself: only the loaders can reach its constructor.)
     *
     * @param path the activity's registry path
     */
    public static String name(String path) {
        return Constants.MOD_ID + ":" + path;
    }

    /**
     * @param brain the pet brain, built once in {@code AbstractPet.makeBrain}
     * @param activity the activity
     * @param behaviors prioritized behaviors of the activity
     * @param erasedOnStop memories vanilla erases when the brain leaves the activity
     */
    public static void register(
        Brain<AbstractPet> brain,
        Activity activity,
        ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super AbstractPet>>> behaviors,
        Set<MemoryModuleType<?>> erasedOnStop
    ) {
        brain.addActivity(activity, behaviors, ImmutableSet.of(), erasedOnStop);
    }

    /**
     * @return every behavior {@code brain} registered under {@code activity}
     */
    @SuppressWarnings("unchecked")
    public static List<BehaviorControl<? super AbstractPet>> behaviorsOf(Brain<AbstractPet> brain, Activity activity) {
        List<BehaviorControl<? super AbstractPet>> behaviors = new ArrayList<>();
        for (Map<Activity, Set<BehaviorControl<?>>> byActivity
                : ((BrainAccessor) brain).chiikawa$getAvailableBehaviorsByPriority().values()) {
            Set<BehaviorControl<?>> set = byActivity.get(activity);
            if (set != null) {
                for (BehaviorControl<?> behavior : set) {
                    behaviors.add((BehaviorControl<? super AbstractPet>) behavior);
                }
            }
        }
        return behaviors;
    }
}
