package com.dwinovo.chiikawa.social;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.entity.brain.intent.RunningIntent;
import com.dwinovo.chiikawa.init.InitMemory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;

/**
 * Who may play which scene with whom. Asked by the sensor when a pet looks around, and
 * again by the pet as it sets off, since the partner may have got busy in between.
 */
public final class SocialRules {
    private SocialRules() {
    }

    /**
     * A scene the pet could start now with a pet it can see, each scene weighed by its
     * chance. Scenes are thought of in id order; among the partners a scene allows, the
     * pet goes for the most preferred kind it can find, the nearest one of those.
     *
     * @param gameTime the current game time
     * @return the scene, not yet under way
     */
    public static Optional<InteractionPlan> find(AbstractPet pet, long gameTime) {
        Holder<EntityType<?>> type = typeOf(pet);
        for (Map.Entry<Identifier, PetInteraction> entry : PetInteractions.all().entrySet()) {
            PetInteraction interaction = entry.getValue();
            Optional<PetInteraction.Side> self = interaction.initiatorSide(type);
            if (self.isEmpty() || !hasToHandOver(pet, interaction) || pet.getRandom().nextFloat() >= interaction.chance()) {
                continue;
            }
            Optional<Partner> partner = bestPartner(pet, entry.getKey(), interaction, gameTime);
            if (partner.isPresent()) {
                return Optional.of(new InteractionPlan(entry.getKey(), interaction, self.get(), partner.get().pet(),
                    interaction.partners().get(partner.get().rank()), false));
            }
        }
        return Optional.empty();
    }

    /**
     * Whether {@code partner} would play this scene with {@code pet} now: near enough, of a
     * kind the scene is played with, not played by these two lately, and doing what the
     * scene asks of it. An idle partner must be free — permitted to play along by its own
     * owner, not already waited for by another pet, and doing nothing but pottering about.
     *
     * @return the partner's place among the scene's partners, empty if it would not
     */
    public static OptionalInt accepts(Identifier id, PetInteraction interaction, AbstractPet pet,
            AbstractPet partner, long gameTime) {
        if (partner == pet || !partner.isAlive() || partner.level() != pet.level()
                || pet.distanceToSqr(partner) > interaction.noticeDistance() * interaction.noticeDistance()) {
            return OptionalInt.empty();
        }
        OptionalInt rank = interaction.partnerRank(typeOf(partner));
        if (rank.isEmpty()
                || cooldowns(pet).coolingDown(id, partner.getUUID(), gameTime)
                || interaction.partnerFinished()
                    .filter(slip -> !slip.metBy(partner.getBrain().getMemory(InitMemory.LAST_FINISHED_SLIP.get()), gameTime))
                    .isPresent()) {
            return OptionalInt.empty();
        }
        boolean doingIt = switch (interaction.partnerState()) {
            case IDLE -> isFree(partner);
            case PLAYING_MUSIC -> partner.getActivity() == PetActivity.PLAY_GUITAR;
        };
        return doingIt ? rank : OptionalInt.empty();
    }

    /**
     * Whether the pet carries what it hands over in this scene, if it hands anything over.
     * Looked for among its things, never the tool in its hand or the bag on its back.
     */
    public static boolean hasToHandOver(AbstractPet pet, PetInteraction interaction) {
        return interaction.handsOver().isEmpty() || handOverSlot(pet, interaction).isPresent();
    }

    /**
     * Takes one of what the pet hands over in this scene out of its backpack.
     *
     * @return what it took, empty when it has none left
     */
    public static ItemStack takeHandOver(AbstractPet pet, PetInteraction interaction) {
        OptionalInt slot = handOverSlot(pet, interaction);
        return slot.isPresent() ? pet.getBackpack().removeItem(slot.getAsInt(), 1) : ItemStack.EMPTY;
    }

    /**
     * Notes on both pets that they have played this scene together, so neither starts it
     * with the other again before the scene's cooldown is over.
     */
    public static void rememberPlayed(Identifier id, PetInteraction interaction, AbstractPet one, AbstractPet other,
            long gameTime) {
        long end = gameTime + interaction.cooldownTicks();
        note(one, id, other.getUUID(), end, gameTime);
        note(other, id, one.getUUID(), end, gameTime);
    }

    private static void note(AbstractPet pet, Identifier id, UUID other, long end, long gameTime) {
        pet.getBrain().setMemory(InitMemory.SOCIAL_COOLDOWNS.get(), cooldowns(pet).with(id, other, end, gameTime));
    }

    private static SocialCooldowns cooldowns(AbstractPet pet) {
        return pet.getBrain().getMemory(InitMemory.SOCIAL_COOLDOWNS.get()).orElse(SocialCooldowns.NONE);
    }

    private static Optional<Partner> bestPartner(AbstractPet pet, Identifier id, PetInteraction interaction,
            long gameTime) {
        Partner best = null;
        // Nearest first, so the first of each kind found is the nearest of that kind.
        for (LivingEntity seen : pet.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                .map(visible -> visible.findAll(entity -> entity instanceof AbstractPet))
                .orElse(List.of())) {
            AbstractPet partner = (AbstractPet) seen;
            OptionalInt rank = accepts(id, interaction, pet, partner, gameTime);
            if (rank.isPresent() && (best == null || rank.getAsInt() < best.rank())) {
                best = new Partner(partner, rank.getAsInt());
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Free to be asked: its owner lets it play, nobody is on the way to it, and it is only
     * pottering about. A pet at work is never pulled off it for a scene.
     */
    private static boolean isFree(AbstractPet partner) {
        return PetConstraints.allows(partner, PetOwnership.of(partner), IntentCategory.SOCIAL)
            && !partner.getBrain().hasMemoryValue(InitMemory.INTERACTION_RESERVATION.get())
            && partner.getBrain().getMemory(InitMemory.CURRENT_INTENT.get())
                .map(RunningIntent::id)
                .map(PetIntents::get)
                .map(PetIntent::category)
                .filter(category -> category == IntentCategory.WANDER)
                .isPresent();
    }

    private static OptionalInt handOverSlot(AbstractPet pet, PetInteraction interaction) {
        SimpleContainer backpack = pet.getBackpack();
        for (int slot = AbstractPet.BAG_SLOT + 1; slot < backpack.getContainerSize(); slot++) {
            if (interaction.handsOver(backpack.getItem(slot))) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    private static Holder<EntityType<?>> typeOf(AbstractPet pet) {
        return pet.getType().builtInRegistryHolder();
    }

    /** A pet a scene could be played with, and where it stands among the scene's partners. */
    private record Partner(AbstractPet pet, int rank) {
    }
}
