package com.dwinovo.chiikawa.utils;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetActivities;
import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.entity.brain.task.idle.GlanceBehavior;
import com.dwinovo.chiikawa.entity.brain.task.idle.RestBehavior;
import com.dwinovo.chiikawa.entity.brain.task.idle.WeightedChoice;
import com.dwinovo.chiikawa.entity.brain.task.social.CooperateBehavior;
import com.dwinovo.chiikawa.entity.brain.task.social.SocializeBehavior;
import com.dwinovo.chiikawa.entity.brain.task.tameable.AnchorLeashBehavior;
import com.dwinovo.chiikawa.entity.brain.task.tameable.FloatBehavior;
import com.dwinovo.chiikawa.entity.brain.task.tameable.FollowOwnerBehavior;
import com.dwinovo.chiikawa.entity.brain.task.tameable.PickUpItemTask;
import com.dwinovo.chiikawa.entity.brain.task.tameable.RandomWalkTask;
import com.dwinovo.chiikawa.entity.brain.task.tameable.SitBehavior;
import com.dwinovo.chiikawa.entity.brain.task.tameable.GiveGiftBehavior;
import com.dwinovo.chiikawa.entity.brain.task.tameable.GoShoppingBehavior;
import com.dwinovo.chiikawa.entity.brain.task.tameable.TakeTaskBehavior;
import com.dwinovo.chiikawa.init.InitActivity;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Set;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
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
        Pair<Integer, BehaviorControl<? super AbstractPet>> lookAround = Pair.of(99, new WeightedChoice<>(List.of(
            lookAtPlayer(), lookAtCreature(), rest()
        )));
        PetActivities.register(brain, InitActivity.STAY.get(), ImmutableList.of(sit, lookAround), Set.of());
    }

    /** {@code wander}: look around, stroll, and now and then say something. */
    public static void addIdleTasks(Brain<AbstractPet> brain) {
        Pair<Integer, BehaviorControl<? super AbstractPet>> randomTask = Pair.of(99, new WeightedChoice<>(List.of(
            lookAtPlayer(), lookAtCreature(), stroll(), rest()
        )));
        Pair<Integer, BehaviorControl<? super AbstractPet>> chatter = Pair.of(99, BehaviorBuilder.create(instance ->
            instance.point((level, pet, gameTime) -> PetSpeech.say(pet, VoiceMoment.IDLE).isPresent())));
        PetActivities.register(brain, Activity.IDLE, ImmutableList.of(randomTask, chatter), Set.of());
    }

    /** {@code pick_up_item}: walk to the remembered item and take it. */
    public static void addPickUpTasks(Brain<AbstractPet> brain) {
        PetActivities.register(brain, InitActivity.PICK_UP.get(),
            ImmutableList.of(Pair.of(3, new PickUpItemTask(0.7f))), Set.of());
    }

    /** {@code gift_owner}: carry what the pet bought for its owner over to them. */
    public static void addGiftTasks(Brain<AbstractPet> brain) {
        PetActivities.register(brain, InitActivity.GIFT_OWNER.get(),
            ImmutableList.of(Pair.of(2, new GiveGiftBehavior())), Set.of());
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

    /**
     * {@code socialize}: go over to another pet and play a scene with it; {@code cooperate}:
     * play along while another pet comes over.
     */
    public static void addSocialTasks(Brain<AbstractPet> brain) {
        PetActivities.register(brain, InitActivity.SOCIALIZE.get(),
            ImmutableList.of(Pair.of(2, new SocializeBehavior())), Set.of());
        PetActivities.register(brain, InitActivity.COOPERATE.get(),
            ImmutableList.of(Pair.of(2, new CooperateBehavior())), Set.of());
    }

    // What a pet does with nothing to do, each as often and for as long as its personality's
    // idle habits say.

    private static WeightedChoice.Option<AbstractPet> lookAtPlayer() {
        return new WeightedChoice.Option<>(new GlanceBehavior(entity -> entity.getType() == EntityTypes.PLAYER,
            IdleHabits::lookAtPlayer), pet -> habits(pet).lookAtPlayer().weight());
    }

    private static WeightedChoice.Option<AbstractPet> lookAtCreature() {
        return new WeightedChoice.Option<>(new GlanceBehavior(entity -> entity.getType().getCategory() == MobCategory.CREATURE,
            IdleHabits::lookAtCreature), pet -> habits(pet).lookAtCreature().weight());
    }

    private static WeightedChoice.Option<AbstractPet> stroll() {
        return new WeightedChoice.Option<>(new RandomWalkTask(), pet -> habits(pet).stroll());
    }

    private static WeightedChoice.Option<AbstractPet> rest() {
        return new WeightedChoice.Option<>(new RestBehavior(), pet -> habits(pet).rest().weight());
    }

    private static IdleHabits habits(AbstractPet pet) {
        return PetPersonalities.of(pet.getType()).idle();
    }
}
