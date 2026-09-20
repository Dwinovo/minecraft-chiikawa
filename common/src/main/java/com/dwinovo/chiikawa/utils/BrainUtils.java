package com.dwinovo.chiikawa.utils;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetActivities;
import com.dwinovo.chiikawa.entity.brain.task.tameable.AnchorLeashBehavior;
import com.dwinovo.chiikawa.entity.brain.task.tameable.FloatBehavior;
import com.dwinovo.chiikawa.entity.brain.task.tameable.FollowOwnerBehavior;
import com.dwinovo.chiikawa.entity.brain.task.tameable.PickUpItemTask;
import com.dwinovo.chiikawa.entity.brain.task.tameable.RandomWalkTask;
import com.dwinovo.chiikawa.entity.brain.task.tameable.SitBehavior;
import com.dwinovo.chiikawa.entity.brain.task.tameable.GoShoppingBehavior;
import com.dwinovo.chiikawa.entity.brain.task.tameable.TakeTaskBehavior;
import com.dwinovo.chiikawa.init.InitActivity;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.Set;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.schedule.Activity;

/** Activities shared by every pet, whatever its job. */
public final class BrainUtils {
    private BrainUtils() {
    }

    /** Always active and unrelated to any intent: float, look, move, anchor leash. */
    public static void addCoreTasks(Brain<AbstractPet> brain) {
        Pair<Integer, BehaviorControl<? super AbstractPet>> floatInWater = Pair.of(0, new FloatBehavior<>());
        Pair<Integer, BehaviorControl<? super AbstractPet>> look = Pair.of(0, new LookAtTargetSink(45, 45));
        Pair<Integer, BehaviorControl<? super AbstractPet>> walkToTarget = Pair.of(1, new MoveToTargetSink());
        Pair<Integer, BehaviorControl<? super AbstractPet>> anchorLeash = Pair.of(2, new AnchorLeashBehavior());
        PetActivities.register(brain, Activity.CORE, ImmutableList.of(floatInWater, look, walkToTarget, anchorLeash), Set.of());
    }

    /** {@code follow_owner}: walk back to the owner. */
    public static void addFollowOwnerTasks(Brain<AbstractPet> brain) {
        PetActivities.register(brain, InitActivity.FOLLOW_OWNER.get(),
            ImmutableList.of(Pair.of(2, new FollowOwnerBehavior())), Set.of());
    }

    /** {@code stay}: hold still but keep glancing around. */
    public static void addStayTasks(Brain<AbstractPet> brain) {
        Pair<Integer, BehaviorControl<? super AbstractPet>> sit = Pair.of(0, new SitBehavior<>());
        Pair<Integer, BehaviorControl<? super AbstractPet>> lookAround = Pair.of(99, new RunOne<AbstractPet>(ImmutableList.of(
            lookAtPlayer(), lookAtCreature(), doNothing()
        )));
        PetActivities.register(brain, InitActivity.STAY.get(), ImmutableList.of(sit, lookAround), Set.of());
    }

    /** {@code wander}: look around and stroll. */
    public static void addIdleTasks(Brain<AbstractPet> brain) {
        Pair<Integer, BehaviorControl<? super AbstractPet>> randomTask = Pair.of(99, new RunOne<AbstractPet>(ImmutableList.of(
            lookAtPlayer(), lookAtCreature(), Pair.of(new RandomWalkTask(), 2), doNothing()
        )));
        PetActivities.register(brain, Activity.IDLE, ImmutableList.of(randomTask), Set.of());
    }

    /** {@code pick_up_item}: walk to the remembered item and take it. */
    public static void addPickUpTasks(Brain<AbstractPet> brain) {
        PetActivities.register(brain, InitActivity.PICK_UP.get(),
            ImmutableList.of(Pair.of(3, new PickUpItemTask(0.7f))), Set.of());
    }

    /** {@code shop}: walk to the nearest shop and buy something the pet likes. */
    public static void addShopTasks(Brain<AbstractPet> brain) {
        PetActivities.register(brain, InitActivity.SHOP.get(),
            ImmutableList.of(Pair.of(2, new GoShoppingBehavior())), Set.of());
    }

    /** {@code take_task}: walk to the nearest labor board and take a slip. */
    public static void addTakeTaskTasks(Brain<AbstractPet> brain) {
        PetActivities.register(brain, InitActivity.TAKE_TASK.get(),
            ImmutableList.of(Pair.of(2, new TakeTaskBehavior())), Set.of());
    }

    private static Pair<BehaviorControl<? super AbstractPet>, Integer> lookAtPlayer() {
        return Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 5), 2);
    }

    private static Pair<BehaviorControl<? super AbstractPet>, Integer> lookAtCreature() {
        return Pair.of(SetEntityLookTarget.create(MobCategory.CREATURE, 5), 2);
    }

    private static Pair<BehaviorControl<? super AbstractPet>, Integer> doNothing() {
        return Pair.of(new DoNothing(30, 60), 1);
    }
}
