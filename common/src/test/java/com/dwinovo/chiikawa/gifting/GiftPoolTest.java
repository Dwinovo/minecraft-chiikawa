package com.dwinovo.chiikawa.gifting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.gifting.GiftPool.Candidate;
import com.dwinovo.chiikawa.testing.FixedRandom;
import com.mojang.datafixers.util.Either;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** What a pet may give out of its bag, and the drawing of one thing from it. */
class GiftPoolTest {
    /** What items say of themselves, as stand-ins for ones that do: gold is ready, iron is not, whatever the stack. */
    private static final Item READY = Items.GOLD_NUGGET;
    private static final Item UNREADY = Items.IRON_NUGGET;
    private static final Function<ItemStack, Optional<Boolean>> SAYS = stack ->
        stack.is(READY) ? Optional.of(true) : stack.is(UNREADY) ? Optional.of(false) : Optional.empty();

    private static final GiftingSettings EVENLY = new GiftingSettings(24000, List.of());
    private static final Personality NO_KEEPS = Personality.DEFAULT;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static SimpleContainer bag() {
        return new SimpleContainer(AbstractPet.FULL_BACKPACK_SIZE);
    }

    private static Personality keeping(Item... kept) {
        return new Personality(Map.of(), Map.of(), 0.0F, List.of(),
            java.util.Arrays.stream(kept).map(item -> new Personality.WeightedItem(item, 1).kept()).toList(),
            IdleHabits.DEFAULT, Map.of());
    }

    private static List<Item> items(List<Candidate> pool) {
        return pool.stream().map(candidate -> candidate.stack().getItem()).toList();
    }

    @Test
    void anEmptyBagHasNothingToGive() {
        assertTrue(GiftPool.of(bag(), NO_KEEPS, EVENLY).isEmpty());
        assertEquals(Optional.empty(), GiftPool.draw(List.of(), FixedRandom.ints()));
    }

    @Test
    void whatIsInTheBagStorageIsInThePool() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(Items.COOKIE, 3));
        bag.setItem(16, new ItemStack(Items.POPPY));

        assertEquals(List.of(Items.COOKIE, Items.POPPY), items(GiftPool.of(bag, NO_KEEPS, EVENLY, SAYS)));
    }

    @Test
    void theToolInHandAndTheBagItWearsAreNeverPresents() {
        SimpleContainer bag = bag();
        bag.setItem(AbstractPet.MAINHAND_SLOT, new ItemStack(Items.WOODEN_HOE));
        bag.setItem(AbstractPet.BAG_SLOT, new ItemStack(Items.LEATHER));

        assertTrue(GiftPool.of(bag, NO_KEEPS, EVENLY, SAYS).isEmpty());
    }

    @Test
    void theSlotsAWornBagAddsAreStorageToo() {
        SimpleContainer bag = bag();
        bag.setItem(AbstractPet.BACKPACK_SIZE, new ItemStack(Items.APPLE));

        assertEquals(List.of(Items.APPLE), items(GiftPool.of(bag, NO_KEEPS, EVENLY, SAYS)));
    }

    @Test
    void whatThePetKeepsIsNotInThePool() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(Items.COOKIE));
        bag.setItem(3, new ItemStack(Items.POPPY));

        assertEquals(List.of(Items.POPPY), items(GiftPool.of(bag, keeping(Items.COOKIE), EVENLY, SAYS)));
    }

    @Test
    void anItemThatSaysItIsNotReadyIsLeftOut() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(UNREADY));
        bag.setItem(3, new ItemStack(Items.COOKIE));

        assertEquals(List.of(Items.COOKIE), items(GiftPool.of(bag, NO_KEEPS, EVENLY, SAYS)));
    }

    @Test
    void anItemThatSaysItIsReadyIsInThePoolEvenThoughThePetKeepsIt() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(READY));
        bag.setItem(3, new ItemStack(Items.COOKIE));

        List<Item> pool = items(GiftPool.of(bag, keeping(READY, Items.COOKIE), EVENLY, SAYS));

        assertEquals(List.of(READY), pool, "readiness decides for the item that has it, and the keeping for the rest");
    }

    @Test
    void threeStacksOfCookiesAndAFlowerAreTwoCandidatesAsLikelyAsEachOther() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(Items.COOKIE, 64));
        bag.setItem(3, new ItemStack(Items.COOKIE, 64));
        bag.setItem(4, new ItemStack(Items.POPPY));
        bag.setItem(5, new ItemStack(Items.COOKIE, 10));
        List<Candidate> pool = GiftPool.of(bag, NO_KEEPS, EVENLY, SAYS);

        assertEquals(2, pool.size());
        assertEquals(2, pool.get(0).slot(), "a kind is where it is first");
        // Two of weight 1: a roll of 0 is the cookie, 1 the flower, whatever there is of either.
        assertEquals(Items.COOKIE, GiftPool.draw(pool, FixedRandom.ints(0)).orElseThrow().stack().getItem());
        assertEquals(Items.POPPY, GiftPool.draw(pool, FixedRandom.ints(1)).orElseThrow().stack().getItem());
    }

    @Test
    void everyItemThatIsItsOwnAlbumIsACandidateOfItsOwn() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(READY));
        bag.setItem(3, new ItemStack(READY));
        bag.setItem(4, new ItemStack(Items.COOKIE));

        List<Candidate> pool = GiftPool.of(bag, NO_KEEPS, EVENLY, SAYS);

        assertEquals(List.of(2, 3, 4), pool.stream().map(Candidate::slot).toList());
    }

    @Test
    void whatThePetFanciesWeighsMoreAndTheRestWeighsOne() {
        SimpleContainer bag = bag();
        bag.setItem(2, new ItemStack(Items.COOKIE));
        bag.setItem(3, new ItemStack(Items.POPPY));
        GiftingSettings fond = new GiftingSettings(24000, List.of(new GiftingSettings.Weighted(Either.right(Items.POPPY), 5)));
        List<Candidate> pool = GiftPool.of(bag, NO_KEEPS, fond, SAYS);

        assertEquals(List.of(1, 5), pool.stream().map(Candidate::weight).toList());
        // Six in all: the first is the cookie's, the other five the flower's.
        assertEquals(Items.COOKIE, GiftPool.draw(pool, FixedRandom.ints(0)).orElseThrow().stack().getItem());
        IntStream.range(1, 6).forEach(roll ->
            assertEquals(Items.POPPY, GiftPool.draw(pool, FixedRandom.ints(roll)).orElseThrow().stack().getItem()));
    }

    @Test
    void theFirstLineThatFitsSaysWhatAThingWeighs() {
        GiftingSettings settings = new GiftingSettings(0, List.of(
            new GiftingSettings.Weighted(Either.right(Items.COOKIE), 3),
            new GiftingSettings.Weighted(Either.right(Items.COOKIE), 9)));

        assertEquals(3, settings.weightOf(new ItemStack(Items.COOKIE)));
        assertEquals(GiftingSettings.UNLISTED_WEIGHT, settings.weightOf(new ItemStack(Items.POPPY)));
    }

    @Test
    void aPoolWithNoWeightHasNoDraw() {
        assertFalse(GiftPool.draw(List.of(new Candidate(2, new ItemStack(Items.COOKIE), 0)), FixedRandom.ints(0)).isPresent());
    }
}
