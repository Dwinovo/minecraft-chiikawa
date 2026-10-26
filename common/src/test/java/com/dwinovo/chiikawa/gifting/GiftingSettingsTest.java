package com.dwinovo.chiikawa.gifting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GiftingSettingsTest {
    private static final ResourceLocation CHIIKAWA = ResourceLocation.fromNamespaceAndPath("chiikawa", "chiikawa");
    private static final ResourceLocation RAKKO = ResourceLocation.fromNamespaceAndPath("chiikawa", "rakko");
    private static final String GOOD = """
        { "cooldown_ticks": 24000, "owner_daily_limit": 3, "fade_ticks": 12000,
          "occasions": { "was_treated": { "chance": 0.3, "delay_ticks": 1200 }, "film_full": { "chance": 1.0 } },
          "gifts": [ { "item": "minecraft:book", "weight": 4, "characters": ["chiikawa:furuhonya"] },
                     { "items": "#chiikawa:gift_flowers", "weight": 3 } ],
          "never": "#chiikawa:never_gifted" }""";

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static GiftingSettings good() {
        return GiftingSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(GOOD)).getOrThrow();
    }

    @Test
    void settingsParseTheGatesTheOccasionsAndTheGifts() {
        GiftingSettings settings = good();

        assertEquals(24000, settings.cooldownTicks());
        assertEquals(3, settings.ownerDailyLimit());
        assertEquals(12000, settings.fadeTicks());
        assertEquals(new GiftingSettings.OccasionSettings(0.3F, 1200), settings.occasion(GiftOccasion.WAS_TREATED).orElseThrow());
        assertEquals(new GiftingSettings.OccasionSettings(1.0F, 0), settings.occasion(GiftOccasion.FILM_FULL).orElseThrow(),
            "an occasion with no delay has none");
        assertTrue(settings.occasion(GiftOccasion.GOT_PAID).isEmpty(), "an occasion left out never puts a present in mind");
        assertEquals(2, settings.gifts().size());
        assertTrue(settings.gifts().get(0).match().left().isPresent());
        assertTrue(settings.gifts().get(1).match().right().isPresent());
        assertEquals(ResourceLocation.fromNamespaceAndPath("chiikawa", "never_gifted"), settings.never().location());
    }

    @Test
    void settingsEncodeToWhatTheyParseFrom() {
        GiftingSettings settings = good();

        JsonElement json = GiftingSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow();

        assertEquals(settings, GiftingSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertEquals("#chiikawa:gift_flowers", json.getAsJsonObject().getAsJsonArray("gifts").get(1)
            .getAsJsonObject().get("items").getAsString());
        assertEquals("minecraft:book", json.getAsJsonObject().getAsJsonArray("gifts").get(0)
            .getAsJsonObject().get("item").getAsString());
    }

    @Test
    void settingsThatCannotBeRightAreRejected() {
        assertTrue(GiftingSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("24000", "-1"))).isError(), "a cooldown before the beginning of time");
        assertTrue(GiftingSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("\"weight\": 4", "\"weight\": 0"))).isError(), "a present weighing nothing");
        assertTrue(GiftingSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("minecraft:book", "minecraft:no_such_thing"))).isError());
        assertTrue(GiftingSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("\"chance\": 0.3", "\"chance\": 1.5"))).isError(), "a chance of more than certain");
        assertTrue(GiftingSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("\"fade_ticks\": 12000", "\"fade_ticks\": 0"))).isError(), "a present forgotten as it is thought of");
        assertTrue(GiftingSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("\"occasions\"", "\"times\""))).isError(), "no occasions at all");
        assertTrue(GiftingSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("\"film_full\"", "\"boredom\""))).isError(), "an occasion nothing tells a pet of");
    }

    @Test
    void aGiftIsForEveryoneOrForTheKindsItNames() {
        GiftingSettings settings = good();

        assertTrue(settings.giftOf(new ItemStack(Items.POPPY), CHIIKAWA).isEmpty(), "poppies are no gift of this file");
        assertEquals(Optional.empty(), settings.giftOf(new ItemStack(Items.BOOK), RAKKO), "a book is Furuhonya's to give");
        assertEquals(4, settings.giftOf(new ItemStack(Items.BOOK), ResourceLocation.fromNamespaceAndPath("chiikawa", "furuhonya"))
            .orElseThrow().weight());
        assertTrue(settings.gifts().get(1).isFor(RAKKO), "flowers are everyone's to give");
        assertFalse(settings.gifts().get(0).isFor(RAKKO));
    }

    @Test
    void theLoaderTakesTheOneFileAndNothingElse() {
        ResourceLocation other = ResourceLocation.fromNamespaceAndPath("chiikawa", "other");
        JsonElement good = JsonParser.parseString(GOOD);

        assertTrue(GiftingSettingsLoader.load(Map.of(GiftingSettingsLoader.FILE, good)).isPresent());
        assertEquals(Optional.empty(), GiftingSettingsLoader.load(Map.of(other, good)), "a file of another name");
        assertEquals(Optional.empty(), GiftingSettingsLoader.load(Map.of()));
    }

    @Test
    void aBrokenFileLeavesNoSettingsRatherThanGuessedOnes() {
        assertEquals(Optional.empty(), GiftingSettingsLoader.load(Map.of(GiftingSettingsLoader.FILE,
            JsonParser.parseString("{ \"cooldown_ticks\": \"soon\" }"))));
    }

    @Test
    void aFileOfTheOldShapeDoesNotLoad() {
        // 0.1.3 packs that override the file by hand have `weights` and none of the rest: said so, not guessed.
        assertEquals(Optional.empty(), GiftingSettingsLoader.load(Map.of(GiftingSettingsLoader.FILE,
            JsonParser.parseString("{ \"cooldown_ticks\": 6000, \"weights\": [] }"))));
    }
}
