package com.dwinovo.chiikawa.gifting;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.item.GiftReadiness;
import com.dwinovo.chiikawa.shop.ShopBasket;
import com.dwinovo.chiikawa.shop.Wallet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What a pet may give its owner out of its bag for the present it has in mind. Pure: the same bag and the same
 * mind give the same answer, and the pet's random picks among what it gives, see {@link Pick}.
 *
 * <p>Three sources, and the first that has anything is the only one asked:
 * <ol>
 *   <li>what the pet set aside for its owner when it thought of the present, if it still has it;</li>
 *   <li>what its kind likes, of which it has more than it keeps for itself: it gives one and keeps the rest;</li>
 *   <li>the gifts the settings name for its kind, anything in the bag that is one.</li>
 * </ol>
 * A pet with none of these has nothing to give and gives nothing: the present it had in mind fades, and no
 * other thing is given in its place.
 *
 * <p>Pooled are the things in the bag's storage slots, not the tool in its hand nor the bag it wears. Never given:
 * what the pet works with, what it keeps for itself, money and what the settings say never: but a thing that says
 * for itself whether it is ready to be given ({@link GiftReadiness}) is asked instead of what the pet keeps, which
 * is how a full roll of film is a present though film is something a pet keeps.
 *
 * <p>The draw is among kinds of thing, not slots: three stacks of cookies and one flower make two candidates, each
 * as likely as the other, however many cookies there are. A thing that is not alike from stack to stack, as every
 * full roll of film is its own album, is a candidate for each of them.
 */
public final class GiftChoice {
    private GiftChoice() {
    }

    /**
     * One thing the pet may give.
     *
     * @param slot where the pet has it, the first slot it is in
     * @param item what it is
     * @param weight how much the pet fancies giving it
     */
    public record Pick(int slot, Item item, int weight) {
    }

    /**
     * @param intention the present in the pet's mind
     * @param bag the pet's bag, slot by slot
     * @param character the kind of pet, by its entity type
     * @param personality what the pet's kind keeps for itself and likes
     * @param settings how pets give presents
     * @param never what the pet never gives, see {@link #never}
     * @return what the pet may give, none if it has nothing
     */
    public static List<Pick> of(GiftIntention intention, Container bag, ResourceLocation character, Personality personality,
            GiftingSettings settings, Predicate<ItemStack> never) {
        return of(intention, bag, character, personality, settings, never, GiftChoice::saidByTheItem);
    }

    /**
     * @param settings how pets give presents
     * @param workItems what the pet works with
     * @return what no pet gives, whatever else is said of it: what it works with, money and what the settings say never
     */
    public static Predicate<ItemStack> never(GiftingSettings settings, Predicate<ItemStack> workItems) {
        return stack -> workItems.test(stack) || Wallet.isMoney(stack) || stack.is(settings.never());
    }

    /**
     * As {@link #of(GiftIntention, Container, ResourceLocation, Personality, GiftingSettings, Predicate)}, with something
     * other than the items themselves to say which of them are ready to be given.
     *
     * @param readiness what an item says of a stack of it, when it says anything
     */
    static List<Pick> of(GiftIntention intention, Container bag, ResourceLocation character, Personality personality,
            GiftingSettings settings, Predicate<ItemStack> never, Function<ItemStack, Optional<Boolean>> readiness) {
        List<Pick> keepsake = keepsake(intention, bag, never);
        if (!keepsake.isEmpty()) {
            return keepsake;
        }
        Predicate<ItemStack> givable = stack -> !never.test(stack)
            && readiness.apply(stack).orElseGet(() -> !ShopBasket.isKept(personality, stack.getItem()));
        List<Pick> liked = candidates(bag, givable, stack -> personality.likes().stream()
            .filter(liking -> !liking.keep() && liking.item() == stack.getItem())
            .mapToInt(liking -> liking.weight().asInt()).findFirst(), readiness, 2);
        if (!liked.isEmpty()) {
            return liked;
        }
        return candidates(bag, givable, stack -> settings.giftOf(stack, character)
            .map(gift -> OptionalInt.of(gift.weight())).orElse(OptionalInt.empty()), readiness, 1);
    }

    /** What the pet set aside, while it is in the bag still and is not something it never gives. */
    private static List<Pick> keepsake(GiftIntention intention, Container bag, Predicate<ItemStack> never) {
        if (intention.keepsake().isEmpty() || never.test(intention.keepsake().get())) {
            return List.of();
        }
        for (int slot = AbstractPet.BAG_SLOT + 1; slot < bag.getContainerSize(); slot++) {
            ItemStack stack = bag.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, intention.keepsake().get())) {
                return List.of(new Pick(slot, stack.getItem(), 1));
            }
        }
        return List.of();
    }

    /**
     * The kinds of thing in the bag that may be given and that the weigher has a weight for, each at the slot it is
     * first in.
     *
     * @param weigh how much the pet fancies giving a stack, nothing for a thing that is no candidate
     * @param least how many of a kind the pet has to have, in all its slots, for it to be a candidate: more than it gives
     */
    private static List<Pick> candidates(Container bag, Predicate<ItemStack> givable,
            Function<ItemStack, OptionalInt> weigh, Function<ItemStack, Optional<Boolean>> readiness, int least) {
        List<Pick> picks = new ArrayList<>();
        Set<Item> seen = new HashSet<>();
        for (int slot = AbstractPet.BAG_SLOT + 1; slot < bag.getContainerSize(); slot++) {
            ItemStack stack = bag.getItem(slot);
            if (stack.isEmpty() || !givable.test(stack) || weigh.apply(stack).isEmpty()) {
                continue;
            }
            // Alike from stack to stack unless the item says each one is its own.
            boolean own = readiness.apply(stack).isPresent();
            if ((own || !seen.contains(stack.getItem())) && count(bag, stack.getItem()) >= least) {
                picks.add(new Pick(slot, stack.getItem(), weigh.apply(stack).getAsInt()));
            }
            seen.add(stack.getItem());
        }
        return picks;
    }

    private static int count(Container bag, Item item) {
        int count = 0;
        for (int slot = AbstractPet.BAG_SLOT + 1; slot < bag.getContainerSize(); slot++) {
            ItemStack stack = bag.getItem(slot);
            count += stack.is(item) ? stack.getCount() : 0;
        }
        return count;
    }

    /** What the item itself says of a stack of it, if it says anything: whether it is ready to be given. */
    private static Optional<Boolean> saidByTheItem(ItemStack stack) {
        return stack.getItem() instanceof GiftReadiness readiness
            ? Optional.of(readiness.readyToGive(stack)) : Optional.empty();
    }
}
