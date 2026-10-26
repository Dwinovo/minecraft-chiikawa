package com.dwinovo.chiikawa.gifting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.gifting.GiftChoice.Pick;
import com.dwinovo.chiikawa.init.InitTag;
import com.mojang.datafixers.util.Either;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** What a pet may give out of its bag for the present it has in mind: where it looks first, and what it never gives. */
class GiftChoiceTest {
    private static final ResourceLocation CHIIKAWA = ResourceLocation.fromNamespaceAndPath("chiikawa", "chiikawa");
    private static final ResourceLocation FURUHONYA = ResourceLocation.fromNamespaceAndPath("chiikawa", "furuhonya");
    /** What items say of themselves, as stand-ins for ones that do: gold is ready, iron is not, whatever the stack. */
    private static final Item READY = Items.GOLD_NUGGET;
    private static final Item UNREADY = Items.IRON_NUGGET;
    private static final Function<ItemStack, Optional<Boolean>> SAYS = stack ->
        stack.is(READY) ? Optional.of(true) : stack.is(UNREADY) ? Optional.of(false) : Optional.empty();
    private static final Predicate<ItemStack> NOTHING_NEVER = stack -> false;

    private static final GiftingSettings SETTINGS = new GiftingSettings(24000, 3, 24000, Map.of(), List.of(
        new GiftingSettings.Gift(Either.left(Items.POPPY), 3, List.of()),
        new GiftingSettings.Gift(Either.left(Items.BOOK), 4, List.of(FURUHONYA))), InitTag.NEVER_GIFTED);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static SimpleContainer bag() {
        return new SimpleContainer(AbstractPet.FULL_BACKPACK_SIZE);
    }

    private static GiftIntention thinkingOf(ItemStack keepsake) {
        return new GiftIntention(GiftOccasion.FILM_FULL, Optional.of(keepsake), 0, 24000);
    }

    private static GiftIntention thinking() {
        return new GiftIntention(GiftOccasion.GOT_PAID, Optional.empty(), 0, 24000);
    }

    private static Personality liking(Personality.WeightedItem... liked) {
        return new Personality(Map.of(), Map.of(), 0.0F, List.of(), List.of(liked), IdleHabits.DEFAULT, Map.of());
    }

    private static List<Pick> of(GiftIntention intention, SimpleContainer bag, ResourceLocation character, Personality personality,
            Predicate<ItemStack> never) {
        return GiftChoice.of(intention, bag, character, personality, SETTINGS, never, SAYS);
    }

    private static List<Item> items(List<Pick> picks) {
        return picks.stream().map(Pick::item).toList();
    }

    @Test
    void anEmptyBagHasNothingToGive() {
        assertTrue(of(thinking(), bag(), CHIIKAWA, liking(new Personality.WeightedItem(Items.COOKIE, 1)), NOTHING_NEVER).isEmpty());
    }

    @Test
    void whatThePetSetAsideIsGivenBeforeAnythingItLikes() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(Items.COOKIE, 5));
        bag.setItem(5, new ItemStack(Items.APPLE));

        List<Pick> picks = of(thinkingOf(new ItemStack(Items.APPLE)), bag, CHIIKAWA,
            liking(new Personality.WeightedItem(Items.COOKIE, 1)), NOTHING_NEVER);

        assertEquals(List.of(new Pick(5, Items.APPLE, 1)), picks, "the keepsake and only it, though there are cookies it likes");
    }

    @Test
    void aKeepsakeIsTheThingItselfAndNotAThingLikeIt() {
        SimpleContainer bag = bag();
        ItemStack named = new ItemStack(Items.APPLE);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("the one it kept"));
        bag.setItem(2, new ItemStack(Items.APPLE));
        bag.setItem(3, new ItemStack(Items.COOKIE, 2));

        List<Pick> picks = of(thinkingOf(named), bag, CHIIKAWA, liking(new Personality.WeightedItem(Items.COOKIE, 1)), NOTHING_NEVER);

        assertEquals(List.of(Items.COOKIE), items(picks), "an apple with another name is not the apple it set aside");
    }

    @Test
    void aKeepsakeThePetNoLongerHasLeavesItToGiveWhatItLikes() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(Items.COOKIE, 2));

        List<Pick> picks = of(thinkingOf(new ItemStack(Items.APPLE)), bag, CHIIKAWA,
            liking(new Personality.WeightedItem(Items.COOKIE, 3)), NOTHING_NEVER);

        assertEquals(List.of(new Pick(2, Items.COOKIE, 3)), picks);
    }

    @Test
    void aPetGivesOneOfWhatItLikesAndKeepsTheRest() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(Items.COOKIE, 1));
        bag.setItem(3, new ItemStack(Items.CAKE, 1));
        bag.setItem(4, new ItemStack(Items.CAKE, 1));
        Personality likes = liking(new Personality.WeightedItem(Items.COOKIE, 1), new Personality.WeightedItem(Items.CAKE, 2));

        List<Pick> picks = of(thinking(), bag, CHIIKAWA, likes, NOTHING_NEVER);

        assertEquals(List.of(new Pick(3, Items.CAKE, 2)), picks,
            "one cookie is the cookie it keeps; two cakes are one to give, a kind once and at the slot it is first in");
    }

    @Test
    void whatThePetLikesAndKeepsIsNeverGiven() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(Items.COOKIE, 4));

        assertTrue(of(thinking(), bag, CHIIKAWA, liking(new Personality.WeightedItem(Items.COOKIE, 1).kept()), NOTHING_NEVER).isEmpty());
    }

    @Test
    void whatThePetLikesIsGivenOnlyIfItIsNotWhatItWorksWithOrMoneyOrOnTheNeverList() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(Items.WHEAT_SEEDS, 8));
        bag.setItem(3, new ItemStack(Items.EMERALD, 8));
        bag.setItem(4, new ItemStack(Items.DIAMOND, 8));
        Personality likes = liking(new Personality.WeightedItem(Items.WHEAT_SEEDS, 1), new Personality.WeightedItem(Items.EMERALD, 1),
            new Personality.WeightedItem(Items.DIAMOND, 1));
        Predicate<ItemStack> never = stack -> stack.is(Items.WHEAT_SEEDS) || stack.is(Items.EMERALD) || stack.is(Items.DIAMOND);

        assertTrue(of(thinking(), bag, CHIIKAWA, likes, never).isEmpty(), "seeds, money and the never list, all of them it likes");
        assertEquals(List.of(Items.WHEAT_SEEDS, Items.DIAMOND), items(of(thinking(), bag, CHIIKAWA, likes,
            stack -> stack.is(Items.EMERALD))), "and only what is never given is left out");
    }

    @Test
    void aKeepsakeThatIsNeverGivenIsNotGiven() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(Items.WHEAT_SEEDS));

        assertTrue(of(thinkingOf(new ItemStack(Items.WHEAT_SEEDS)), bag, CHIIKAWA, liking(), stack -> stack.is(Items.WHEAT_SEEDS)).isEmpty());
    }

    @Test
    void theGiftsOfAKindAreGivenOnlyByThatKindAndOthersAreGivenByEveryone() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(Items.BOOK));
        bag.setItem(3, new ItemStack(Items.POPPY));
        bag.setItem(4, new ItemStack(Items.STICK));

        assertEquals(List.of(new Pick(2, Items.BOOK, 4), new Pick(3, Items.POPPY, 3)),
            of(thinking(), bag, FURUHONYA, liking(), NOTHING_NEVER));
        assertEquals(List.of(new Pick(3, Items.POPPY, 3)), of(thinking(), bag, CHIIKAWA, liking(), NOTHING_NEVER),
            "a book is not a gift of a pet that is not known for them, and a stick is nobody's");
    }

    @Test
    void whatThePetLikesIsAskedBeforeTheGiftsOfItsKind() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(Items.COOKIE, 2));
        bag.setItem(3, new ItemStack(Items.BOOK));

        assertEquals(List.of(Items.COOKIE), items(of(thinking(), bag, FURUHONYA,
            liking(new Personality.WeightedItem(Items.COOKIE, 1)), NOTHING_NEVER)));
    }

    @Test
    void theToolInHandAndTheBagItWearsAreNeverPresents() {
        SimpleContainer bag = bag();
        bag.setItem(AbstractPet.MAINHAND_SLOT, new ItemStack(Items.BOOK, 2));
        bag.setItem(AbstractPet.BAG_SLOT, new ItemStack(Items.POPPY, 2));

        assertTrue(of(thinking(), bag, FURUHONYA, liking(), NOTHING_NEVER).isEmpty());
    }

    @Test
    void theSlotsAWornBagAddsAreStorageToo() {
        SimpleContainer bag = bag();
        bag.setItem(AbstractPet.BACKPACK_SIZE, new ItemStack(Items.POPPY));

        assertEquals(List.of(Items.POPPY), items(of(thinking(), bag, CHIIKAWA, liking(), NOTHING_NEVER)));
    }

    @Test
    void anItemThatSaysItIsNotReadyIsLeftOutAndOneThatSaysItIsReadyIsGivenThoughThePetKeepsIt() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(UNREADY));
        bag.setItem(3, new ItemStack(READY));
        bag.setItem(4, new ItemStack(READY));
        Personality likes = liking(new Personality.WeightedItem(UNREADY, 1), new Personality.WeightedItem(READY, 1).kept());
        GiftingSettings both = new GiftingSettings(24000, 3, 24000, Map.of(), List.of(
            new GiftingSettings.Gift(Either.left(UNREADY), 1, List.of()), new GiftingSettings.Gift(Either.left(READY), 1, List.of())),
            InitTag.NEVER_GIFTED);

        List<Pick> picks = GiftChoice.of(thinking(), bag, CHIIKAWA, likes, both, NOTHING_NEVER, SAYS);

        assertEquals(List.of(new Pick(3, READY, 1), new Pick(4, READY, 1)), picks,
            "readiness decides for the item that has it, and each such stack is its own present");
    }
}
