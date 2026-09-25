package com.dwinovo.chiikawa.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.testing.FixedRandom;
import com.dwinovo.chiikawa.testing.ItemComponents;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Money, price lists, and what a pet would buy with the one from the other. */
class ShopTest {
    /** Money as a pack that made money of diamonds has it: the wallet takes whatever it is told money is. */
    private static final Predicate<ItemStack> DIAMONDS = stack -> stack.is(Items.DIAMOND);
    private static final ShopCatalog SHOP = new ShopCatalog(List.of(
        new ShopCatalog.Entry(Items.COOKIE, 1, 0),
        new ShopCatalog.Entry(Items.CAKE, 3, 0),
        new ShopCatalog.Entry(Items.WHEAT, 2, 1)));

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ItemComponents.bind();
    }

    @Test
    void aWalletCountsOnlyWhatIsMoney() {
        SimpleContainer bag = new SimpleContainer(4);
        bag.setItem(0, new ItemStack(Items.DIAMOND, 3));
        bag.setItem(1, new ItemStack(Items.EMERALD, 9));
        bag.setItem(2, new ItemStack(Items.DIAMOND, 2));

        assertEquals(5, Wallet.count(bag, DIAMONDS), "a pack that made money of diamonds");
    }

    @Test
    void payingTakesTheMoneyOutOfWhicheverPocketsItIsIn() {
        SimpleContainer bag = new SimpleContainer(3);
        bag.setItem(0, new ItemStack(Items.DIAMOND, 2));
        bag.setItem(2, new ItemStack(Items.DIAMOND, 2));

        assertTrue(Wallet.pay(bag, 3, DIAMONDS));
        assertEquals(1, Wallet.count(bag, DIAMONDS));
    }

    @Test
    void nothingIsPaidWhenThereIsNotEnough() {
        SimpleContainer bag = new SimpleContainer(2);
        bag.setItem(0, new ItemStack(Items.DIAMOND, 2));

        assertFalse(Wallet.pay(bag, 3, DIAMONDS));
        assertEquals(2, Wallet.count(bag, DIAMONDS), "a pet paid part of a price and got nothing for it");
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

        assertTrue(ShopBasket.wants(fussy, SHOP, 2, FixedRandom.ints(0)).isEmpty(),
            "a pet with two emeralds bought a cake costing three");
        assertEquals(Optional.of(Items.CAKE),
            ShopBasket.wants(fussy, SHOP, 3, FixedRandom.ints(0)).map(ShopCatalog.Entry::item));
    }

    @Test
    void aPetDoesNotBuyWhatItDoesNotWant() {
        Personality fussy = likes(Items.DIAMOND);

        assertTrue(ShopBasket.wants(fussy, SHOP, 64, FixedRandom.ints(0)).isEmpty(),
            "a pet bought something off a list it wanted nothing from");
        assertFalse(ShopBasket.wantsAnything(fussy, SHOP, 64));
    }

    @Test
    void aPetWithNoLikingsBuysNothing() {
        assertFalse(ShopBasket.wantsAnything(Personality.DEFAULT, SHOP, 64));
    }

    @Test
    void aPriceListParsesAndLeavesOutPricesItDoesNotSet() {
        ShopCatalog parsed = ShopCatalog.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
            { "entries": [ { "item": "minecraft:cookie", "buy": 1 }, { "item": "minecraft:wheat", "sell": 1 } ] }"""))
            .getOrThrow();

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

    private static Personality likes(Item item) {
        return new Personality(Map.of(), Map.of(), 0.0F, List.of(),
            List.of(new Personality.WeightedItem(item, 1)), IdleHabits.DEFAULT);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("chiikawa", path);
    }
}
