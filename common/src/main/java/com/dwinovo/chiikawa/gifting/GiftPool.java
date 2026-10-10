package com.dwinovo.chiikawa.gifting;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.item.GiftReadiness;
import com.dwinovo.chiikawa.shop.ShopBasket;
import com.dwinovo.chiikawa.shop.Wallet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What a pet may give its owner out of its bag, and the drawing of one thing from it.
 *
 * <p>Pooled are the things in the bag's storage slots, not the tool in its hand nor the bag it
 * wears, and not money or what the pet keeps for itself: but a thing that says for itself whether it
 * is ready to be given ({@link GiftReadiness}) is asked first, which is how a full roll of film is a
 * present though film is something a pet keeps.
 *
 * <p>The draw is among kinds of thing, not slots: three stacks of cookies and one flower make two
 * candidates, each as likely as the other, however many cookies there are. A thing that is not alike
 * from stack to stack, as every full roll of film is its own album, is a candidate for each of them.
 */
public final class GiftPool {
    private GiftPool() {
    }

    /**
     * One thing the pet may give.
     *
     * @param slot where the pet has it, the first slot it is in
     * @param stack what is in that slot
     * @param weight how much the pet fancies giving it
     */
    public record Candidate(int slot, ItemStack stack, int weight) {
    }

    /** The pet's own pool; nothing when no settings say how pets give presents. */
    public static List<Candidate> of(AbstractPet pet) {
        return GiftingSettings.current()
            .map(settings -> of(pet.getBackpack(), PetPersonalities.of(pet.getType()), settings))
            .orElse(List.of());
    }

    /**
     * @param backpack the pet's bag, slot by slot
     * @param personality what the pet keeps for itself
     * @param settings how much it fancies giving each thing
     */
    public static List<Candidate> of(Container backpack, Personality personality, GiftingSettings settings) {
        return of(backpack, personality, settings, GiftPool::saidByTheItem);
    }

    /**
     * As {@link #of(Container, Personality, GiftingSettings)}, with something other than the items
     * themselves to say which of them are ready to be given.
     *
     * @param readiness what an item says of a stack of it, when it says anything
     */
    static List<Candidate> of(Container backpack, Personality personality, GiftingSettings settings,
            Function<ItemStack, Optional<Boolean>> readiness) {
        List<Candidate> pool = new ArrayList<>();
        Set<Item> seen = new HashSet<>();
        for (int slot = AbstractPet.BAG_SLOT + 1; slot < backpack.getContainerSize(); slot++) {
            ItemStack stack = backpack.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Optional<Boolean> said = readiness.apply(stack);
            if (!said.orElseGet(() -> !Wallet.isMoney(stack) && !ShopBasket.isKept(personality, stack.getItem()))) {
                continue;
            }
            // Alike from stack to stack unless the item says each one is its own.
            if (said.isPresent() || seen.add(stack.getItem())) {
                pool.add(new Candidate(slot, stack, settings.weightOf(stack)));
            }
        }
        return pool;
    }

    /** What the item itself says of a stack of it, if it says anything: whether it is ready to be given. */
    private static Optional<Boolean> saidByTheItem(ItemStack stack) {
        return stack.getItem() instanceof GiftReadiness readiness
            ? Optional.of(readiness.readyToGive(stack)) : Optional.empty();
    }

    /** One of the pool by weight, nothing when it is empty. */
    public static Optional<Candidate> draw(List<Candidate> pool, RandomSource random) {
        int total = pool.stream().mapToInt(Candidate::weight).sum();
        if (total <= 0) {
            return Optional.empty();
        }
        int roll = random.nextInt(total);
        for (Candidate candidate : pool) {
            roll -= candidate.weight();
            if (roll < 0) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
