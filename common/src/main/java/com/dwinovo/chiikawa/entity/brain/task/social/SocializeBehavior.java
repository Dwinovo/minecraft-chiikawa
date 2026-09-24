package com.dwinovo.chiikawa.entity.brain.task.social;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.entity.brain.intent.RunningIntent;
import com.dwinovo.chiikawa.entity.interact.PetInteractHandler;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.social.InteractionPlan;
import com.dwinovo.chiikawa.social.InteractionReservation;
import com.dwinovo.chiikawa.social.PartPlayer;
import com.dwinovo.chiikawa.social.PetInteraction;
import com.dwinovo.chiikawa.social.SocialRules;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumables;
import org.jetbrains.annotations.Nullable;

/**
 * Plays a scene the pet has thought of: asks the partner, walks over, and the two play
 * their parts for as long as the scene lasts.
 *
 * <p>An idle partner is reserved as the pet sets off, the way a slip on a labor board is:
 * it stays put and waits, and no other pet can have it in the meantime. The reservation
 * runs out after the scene's {@code reservation_ticks}, and so does the pet's patience —
 * a pet that cannot get there, or finds the partner gone off, gives up. However it ends,
 * the two remember having played it, so they do not try it again straight away.
 *
 * <p>What is handed over in a scene comes out of the pet's backpack at the moment they
 * meet, checked there and then; without one on it the pet gives up instead.
 */
public class SocializeBehavior extends Behavior<AbstractPet> {
    private static final float SPEED = 0.6F;
    /**
     * How much further than the scene's approach distance the two may stand and still
     * begin: the walk stops as soon as it is within the approach distance, and a partner
     * shifting its feet should not undo the arrival.
     */
    private static final int ARRIVE_MARGIN = 1;

    private @Nullable InteractionPlan plan;
    /** The pet's part, once the two have met. */
    private @Nullable PartPlayer part;
    private boolean over;
    /** When the pet gives up getting there, then when the scene ends. */
    private long deadline;

    public SocializeBehavior() {
        super(ImmutableMap.of(
            InitMemory.INTERACTION_PLAN.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
            MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
        ));
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        Brain<AbstractPet> brain = pet.getBrain();
        InteractionPlan idea = brain.getMemory(InitMemory.INTERACTION_PLAN.get()).orElseThrow();
        part = null;
        over = false;
        // The partner may have got busy since the pet thought of it, or the pet eaten what
        // it meant to share.
        if (SocialRules.accepts(idea.id(), idea.interaction(), pet, idea.partner(), gameTime).isEmpty()
                || !SocialRules.hasToHandOver(pet, idea.interaction())) {
            plan = null;
            over = true;
            return;
        }
        plan = idea.engage();
        brain.setMemory(InitMemory.INTERACTION_PLAN.get(), plan);
        PetInteraction interaction = plan.interaction();
        deadline = gameTime + interaction.reservationTicks();
        if (interaction.partnerState() == PetInteraction.PartnerState.IDLE) {
            AbstractPet partner = plan.partner();
            partner.getBrain().setMemoryWithExpiry(InitMemory.INTERACTION_RESERVATION.get(),
                new InteractionReservation(pet, plan.partnerSide(), false), interaction.reservationTicks());
            IntentSelector.requestReevaluate(partner);
        }
        BehaviorUtils.setWalkAndLookTargetMemories(pet, plan.partner(), SPEED, interaction.approachDistance());
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return !over && plan != null && partnerStillThere(pet, plan)
            && (part != null || SocialRules.hasToHandOver(pet, plan.interaction()));
    }

    /** The scene's own clock: {@code reservation_ticks} to get there, then {@code duration_ticks}. */
    @Override
    protected boolean timedOut(long gameTime) {
        return gameTime > deadline;
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        InteractionPlan plan = this.plan;
        if (plan == null) {
            return;
        }
        Brain<AbstractPet> brain = pet.getBrain();
        AbstractPet partner = plan.partner();
        BehaviorUtils.lookAtEntity(pet, partner);
        if (part != null) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            part.tick(pet, gameTime);
            return;
        }
        if (pet.blockPosition().distManhattan(partner.blockPosition())
                <= plan.interaction().approachDistance() + ARRIVE_MARGIN) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            if (partnerReady(plan)) {
                begin(pet, plan, gameTime);
            }
        } else if (!brain.hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
            // The walk ended short of the partner: it cannot be reached from here.
            over = true;
        }
    }

    @Override
    protected void stop(ServerLevel level, AbstractPet pet, long gameTime) {
        InteractionPlan plan = this.plan;
        if (plan != null) {
            AbstractPet partner = plan.partner();
            partner.getBrain().getMemory(InitMemory.INTERACTION_RESERVATION.get())
                .filter(reservation -> reservation.heldBy(pet))
                .ifPresent(reservation -> {
                    partner.getBrain().eraseMemory(InitMemory.INTERACTION_RESERVATION.get());
                    IntentSelector.requestReevaluate(partner);
                });
            SocialRules.rememberPlayed(plan.id(), plan.interaction(), pet, partner, gameTime);
        }
        if (part != null) {
            part.end(pet);
            part = null;
        }
        pet.getBrain().eraseMemory(InitMemory.INTERACTION_PLAN.get());
        pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.plan = null;
    }

    /**
     * They meet: whatever is handed over changes hands, both begin their parts, and the
     * scene's clock starts.
     */
    private void begin(AbstractPet pet, InteractionPlan plan, long gameTime) {
        PetInteraction interaction = plan.interaction();
        AbstractPet partner = plan.partner();
        if (interaction.handsOver().isPresent()) {
            ItemStack treat = SocialRules.takeHandOver(pet, interaction);
            if (treat.isEmpty()) {
                over = true;
                return;
            }
            // Eaten there and then, and worth what a snack from its owner is worth.
            partner.heal(PetInteractHandler.FEED_HEAL);
            partner.playSound(treat.getOrDefault(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD).sound().value(), 1.0F, 1.0F);
        }
        if (interaction.partnerEagerTicks() > 0) {
            partner.feedDish(interaction.partnerEagerTicks());
        }
        part = PartPlayer.begin(pet, plan.self(), gameTime);
        deadline = gameTime + interaction.durationTicks();
        if (interaction.partnerState() == PetInteraction.PartnerState.IDLE) {
            partner.getBrain().getMemory(InitMemory.INTERACTION_RESERVATION.get()).ifPresent(reservation ->
                partner.getBrain().setMemoryWithExpiry(InitMemory.INTERACTION_RESERVATION.get(), reservation.perform(),
                    interaction.durationTicks()));
        }
    }

    /**
     * Whether the partner is still there for it: alive in the same level and, for an idle
     * partner, still waiting for this pet; for a musician, still playing.
     */
    private static boolean partnerStillThere(AbstractPet pet, InteractionPlan plan) {
        AbstractPet partner = plan.partner();
        if (!partner.isAlive() || partner.level() != pet.level()) {
            return false;
        }
        return switch (plan.interaction().partnerState()) {
            case IDLE -> partner.getBrain().getMemory(InitMemory.INTERACTION_RESERVATION.get())
                .filter(reservation -> reservation.heldBy(pet))
                .isPresent();
            case PLAYING_MUSIC -> partner.getActivity() == PetActivity.PLAY_GUITAR;
        };
    }

    /** An idle partner has to have stopped and turned to play along; a musician just carries on. */
    private static boolean partnerReady(InteractionPlan plan) {
        return plan.interaction().partnerState() != PetInteraction.PartnerState.IDLE
            || plan.partner().getBrain().getMemory(InitMemory.CURRENT_INTENT.get())
                .map(RunningIntent::id)
                .filter(PetIntents.COOPERATE::equals)
                .isPresent();
    }
}
