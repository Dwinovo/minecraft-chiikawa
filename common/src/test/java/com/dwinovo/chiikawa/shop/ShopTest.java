package com.dwinovo.chiikawa.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.testing.FixedRandom;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Money, price lists, and what a pet would buy with the one from the other. */
class ShopTest {
    /** Paid in diamonds, as a pack that made money of diamonds has it, or as an exam's fee is. */
    private static final Predicate<ItemStack> DIAMONDS = stack -> stack.is(Items.DIAMOND);
    private static final ShopCatalog SHOP = new ShopCatalog(List.of(
        new ShopCatalog.Entry(Items.COOKIE, 1, 0),
        new ShopCatalog.Entry(Items.CAKE, 3, 0),
        new ShopCatalog.Entry(Items.WHEAT, 2, 1)));

    /** A pet that is carrying nothing but its money. */
    private static final ToIntFunction<Item> NOTHING = item -> 0;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void paymentCountsOnlyWhatPays() {
        SimpleContainer bag = new SimpleContainer(4);
        bag.setItem(0, new ItemStack(Items.DIAMOND, 3));
        bag.setItem(1, new ItemStack(Items.EMERALD, 9));
        bag.setItem(2, new ItemStack(Items.DIAMOND, 2));

        assertEquals(5, Payment.count(bag, DIAMONDS), "a pack that made money of diamonds");
    }

    @Test
    void payingTakesTheMoneyOutOfWhicheverPocketsItIsIn() {
        SimpleContainer bag = new SimpleContainer(3);
        bag.setItem(0, new ItemStack(Items.DIAMOND, 2));
        bag.setItem(2, new ItemStack(Items.DIAMOND, 2));

        assertTrue(Payment.take(bag, 3, DIAMONDS));
        assertEquals(1, Payment.count(bag, DIAMONDS));
    }

    @Test
    void nothingIsPaidWhenThereIsNotEnough() {
        SimpleContainer bag = new SimpleContainer(2);
        bag.setItem(0, new ItemStack(Items.DIAMOND, 2));

        assertFalse(Payment.take(bag, 3, DIAMONDS));
        assertEquals(2, Payment.count(bag, DIAMONDS), "a pet paid part of a price and got nothing for it");
    }

    @Test
    void aPriceListKnowsWhatItSellsAndWhatItTakesIn() {
        assertTrue(SHOP.sale(Items.COOKIE).isPresent());
        assertTrue(SHOP.sale(Items.DIAMOND).isEmpty());
        // Wheat is both; a cookie is only ever sold.
        assertTrue(SHOP.purchase(Items.WHEAT).isPresent());
        assertTrue(SHOP.purchase(Items.COOKIE).isEmpty());
    }

    @Test
    void aPetBuysWhatItLikesAndCanAfford() {
        Personality fussy = likes(Items.CAKE);

        assertTrue(ShopBasket.wants(fussy, SHOP, 2, NOTHING, FixedRandom.ints(0)).isEmpty(),
            "a pet with two emeralds bought a cake costing three");
        assertEquals(Optional.of(Items.CAKE),
            ShopBasket.wants(fussy, SHOP, 3, NOTHING, FixedRandom.ints(0)).map(ShopCatalog.Entry::item));
    }

    @Test
    void aPetDoesNotBuyWhatItDoesNotWant() {
        Personality fussy = likes(Items.DIAMOND);

        assertTrue(ShopBasket.wants(fussy, SHOP, 64, NOTHING, FixedRandom.ints(0)).isEmpty(),
            "a pet bought something off a list it wanted nothing from");
        assertFalse(ShopBasket.wantsAnything(fussy, SHOP, 64, NOTHING));
    }

    @Test
    void aPetWithNoLikingsBuysNothing() {
        assertFalse(ShopBasket.wantsAnything(Personality.DEFAULT, SHOP, 64, NOTHING));
    }

    @Test
    void aLikingThatNeedsSomethingIsOnlyWantedWhileThePetHasIt() {
        // Film, for a pet with a camera to put it in.
        Personality photographer = new Personality(Map.of(), Map.of(), 0.0F, List.of(),
            List.of(new Personality.WeightedItem(Items.CAKE, 1).requiring(Items.BOOK)), IdleHabits.DEFAULT, Map.of());
        ToIntFunction<Item> withABook = item -> item == Items.BOOK ? 1 : 0;

        assertFalse(ShopBasket.wantsAnything(photographer, SHOP, 64, NOTHING), "film for a pet with no camera");
        assertTrue(ShopBasket.wants(photographer, SHOP, 64, NOTHING, FixedRandom.ints(0)).isEmpty());
        assertTrue(ShopBasket.wantsAnything(photographer, SHOP, 64, withABook));
        assertEquals(Optional.of(Items.CAKE),
            ShopBasket.wants(photographer, SHOP, 64, withABook, FixedRandom.ints(0)).map(ShopCatalog.Entry::item));
    }

    @Test
    void aLikingWithALimitStopsBeingWantedOnceThePetHasEnough() {
        // A camera is only worth one.
        Personality collector = new Personality(Map.of(), Map.of(), 0.0F, List.of(),
            List.of(new Personality.WeightedItem(Items.CAKE, 1).limitedTo(2)), IdleHabits.DEFAULT, Map.of());

        assertTrue(ShopBasket.wantsAnything(collector, SHOP, 64, item -> item == Items.CAKE ? 1 : 0));
        assertFalse(ShopBasket.wantsAnything(collector, SHOP, 64, item -> item == Items.CAKE ? 2 : 0),
            "a pet that already had as many as it wants bought another");
    }

    @Test
    void aLikingThatIsNotWantedLeavesTheOthersToBeDrawn() {
        Personality picky = new Personality(Map.of(), Map.of(), 0.0F, List.of(), List.of(
            new Personality.WeightedItem(Items.CAKE, 5).requiring(Items.BOOK),
            new Personality.WeightedItem(Items.COOKIE, 1)), IdleHabits.DEFAULT, Map.of());

        // However heavily the cake weighs, it is not in the draw for a pet with no book.
        assertEquals(Optional.of(Items.COOKIE),
            ShopBasket.wants(picky, SHOP, 64, NOTHING, FixedRandom.ints(0)).map(ShopCatalog.Entry::item));
    }

    @Test
    void whatAPetBuysToKeepIsNeverAPresent() {
        Personality collector = new Personality(Map.of(), Map.of(), 0.0F, List.of(), List.of(
            new Personality.WeightedItem(Items.CAKE, 1).kept(),
            new Personality.WeightedItem(Items.COOKIE, 1)), IdleHabits.DEFAULT, Map.of());

        assertTrue(ShopBasket.isKept(collector, Items.CAKE));
        assertFalse(ShopBasket.isKept(collector, Items.COOKIE), "a cookie may be a present");
        assertFalse(ShopBasket.isKept(collector, Items.WHEAT), "something it does not like at all may be a present");
    }

    @Test
    void aPetSavingUpBuysNothingElseUntilItCanAffordWhatItIsSavingFor() {
        // A cake costs three, a cookie one, and the pet is saving for the cake.
        Personality saver = new Personality(Map.of(), Map.of(), 0.0F, List.of(), List.of(
            new Personality.WeightedItem(Items.COOKIE, 5),
            new Personality.WeightedItem(Items.CAKE, 1).savedUpFor()), IdleHabits.DEFAULT, Map.of());

        for (int money = 0; money < 3; money++) {
            assertFalse(ShopBasket.wantsAnything(saver, SHOP, money, NOTHING), money + " emeralds spent on little things");
            assertTrue(ShopBasket.wants(saver, SHOP, money, NOTHING, FixedRandom.ints(0)).isEmpty());
        }
        // With enough it goes for the cake, however heavily the cookie weighs.
        assertTrue(ShopBasket.wantsAnything(saver, SHOP, 3, NOTHING));
        for (int roll = 0; roll < 6; roll++) {
            assertEquals(Optional.of(Items.CAKE),
                ShopBasket.wants(saver, SHOP, 3, NOTHING, FixedRandom.ints(roll)).map(ShopCatalog.Entry::item));
        }
    }

    @Test
    void aPetThatHasWhatItWasSavingForSpendsAsUsual() {
        Personality saver = new Personality(Map.of(), Map.of(), 0.0F, List.of(), List.of(
            new Personality.WeightedItem(Items.COOKIE, 1),
            new Personality.WeightedItem(Items.CAKE, 1).limitedTo(1).savedUpFor()), IdleHabits.DEFAULT, Map.of());
        ToIntFunction<Item> withACake = item -> item == Items.CAKE ? 1 : 0;

        assertEquals(Optional.of(Items.COOKIE),
            ShopBasket.wants(saver, SHOP, 1, withACake, FixedRandom.ints(0)).map(ShopCatalog.Entry::item));
    }

    @Test
    void aPetNotSavingForSomethingTheShopDoesNotSellSpendsAsUsual() {
        Personality saver = new Personality(Map.of(), Map.of(), 0.0F, List.of(), List.of(
            new Personality.WeightedItem(Items.COOKIE, 1),
            new Personality.WeightedItem(Items.DIAMOND, 1).savedUpFor()), IdleHabits.DEFAULT, Map.of());

        assertTrue(ShopBasket.wantsAnything(saver, SHOP, 1, NOTHING));
    }

    @Test
    void savingUpAndKeepingAreReadFromAndWrittenToAPersonality() {
        Personality.WeightedItem liking = new Personality.WeightedItem(Items.CAKE, 2).kept().savedUpFor();

        var json = Personality.WeightedItem.CODEC.encodeStart(JsonOps.INSTANCE, liking).getOrThrow(false, org.junit.jupiter.api.Assertions::fail).getAsJsonObject();

        assertTrue(json.get("keep").getAsBoolean());
        assertTrue(json.get("save_up").getAsBoolean());
        assertEquals(liking, Personality.WeightedItem.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, org.junit.jupiter.api.Assertions::fail));
        assertFalse(Personality.WeightedItem.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            "{ \"item\": \"minecraft:cake\", \"weight\": 1 }")).getOrThrow(false, org.junit.jupiter.api.Assertions::fail).keep(), "keep is off unless asked for");
    }

    @Test
    void whatAPetCarriesIsCountedAcrossItsBackpack() {
        SimpleContainer bag = new SimpleContainer(3);
        bag.setItem(0, new ItemStack(Items.BOOK, 2));
        bag.setItem(2, new ItemStack(Items.BOOK, 3));
        bag.setItem(1, new ItemStack(Items.CAKE));

        assertEquals(5, ShopBasket.carriedIn(bag).applyAsInt(Items.BOOK));
        assertEquals(0, ShopBasket.carriedIn(bag).applyAsInt(Items.COOKIE));
    }

    @Test
    void aPriceListParsesAndLeavesOutPricesItDoesNotSet() {
        ShopCatalog parsed = ShopCatalog.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
            { "entries": [ { "item": "minecraft:cookie", "buy": 1 }, { "item": "minecraft:wheat", "sell": 1 } ] }"""))
            .getOrThrow(false, org.junit.jupiter.api.Assertions::fail);

        assertEquals(1, parsed.onSale().size());
        assertTrue(parsed.purchase(Items.WHEAT).isPresent());
    }

    @Test
    void abrokenFileIsSkippedRatherThanTakingTheRestWithIt() {
        ShopCatalogLoader.Loaded loaded = ShopCatalogLoader.load(Map.of(
            id("good"), JsonParser.parseString("""
                { "entries": [ { "item": "minecraft:cookie", "buy": 1 } ] }"""),
            id("broken"), JsonParser.parseString("""
                { "entries": "not a list" }""")));

        assertEquals(1, loaded.catalogs().size());
        assertEquals(1, loaded.errors().size());
    }

    @Test
    void aBundleCountDefaultsToOneAndIsParsedWhenGiven() {
        ShopCatalog parsed = ShopCatalog.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
            { "entries": [ { "item": "minecraft:cookie", "buy": 1 },
                           { "item": "minecraft:sugar", "buy": 1, "count": 4 } ] }"""))
            .getOrThrow(false, org.junit.jupiter.api.Assertions::fail);

        assertEquals(1, parsed.sale(Items.COOKIE).orElseThrow().count());
        assertEquals(4, parsed.sale(Items.SUGAR).orElseThrow().count());
        assertTrue(ShopCatalog.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
            { "entries": [ { "item": "minecraft:sugar", "buy": 1, "count": 0 } ] }""")).error().isPresent(),
            "a bundle of none");
    }

    private static Personality likes(Item item) {
        return new Personality(Map.of(), Map.of(), 0.0F, List.of(),
            List.of(new Personality.WeightedItem(item, 1)), IdleHabits.DEFAULT, Map.of());
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("chiikawa", path);
    }
}
