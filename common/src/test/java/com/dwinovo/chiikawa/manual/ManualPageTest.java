package com.dwinovo.chiikawa.manual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ManualPageTest {
    @Test
    void aPackWritesAPageAsStageDirections() {
        ManualPage page = parse("""
            { "order": 5, "title": "t", "panels": [ { "caption": "c", "actors": [
                { "pet": "chiikawa:usagi", "x": 0.3, "facing": 20, "hold": "minecraft:wooden_hoe",
                  "action": "harvest", "every": 24, "say": "s" },
                { "prop": "chiikawa:labor_board", "x": 0.7 },
                { "item": "#chiikawa:currency", "y": 0.4, "bob": true } ] } ] }""");

        ManualPage.Actor usagi = page.panels().get(0).actors().get(0);
        assertEquals(PetAction.HARVEST, usagi.action().orElseThrow());
        assertEquals(24, usagi.motion().every());
        assertEquals("s", usagi.motion().say().orElseThrow());
        ManualPage.Actor money = page.panels().get(0).actors().get(2);
        assertTrue(money.item().orElseThrow().tag(), "money is named by its tag");
        assertTrue(money.motion().bob());
        assertEquals(0.5F, money.x(), "and stands in the middle when nothing says otherwise");
    }

    @Test
    void anActorIsExactlyOneThing() {
        assertFalse(ManualPage.CODEC.parse(JsonOps.INSTANCE, json("""
            { "title": "t", "panels": [ { "caption": "c", "actors": [
                { "pet": "chiikawa:usagi", "prop": "chiikawa:shop" } ] } ] }""")).result().isPresent());
        assertFalse(ManualPage.CODEC.parse(JsonOps.INSTANCE, json("""
            { "title": "t", "panels": [ { "caption": "c", "actors": [ { "x": 0.2 } ] } ] }""")).result().isPresent());
    }

    @Test
    void aMoveNobodyKnowsAndAFifthPanelAreRefused() {
        assertFalse(ManualPage.CODEC.parse(JsonOps.INSTANCE, json("""
            { "title": "t", "panels": [ { "caption": "c", "actors": [
                { "pet": "chiikawa:usagi", "action": "moonwalk" } ] } ] }""")).result().isPresent());
        assertFalse(ManualPage.CODEC.parse(JsonOps.INSTANCE, json("""
            { "title": "t", "panels": [ { "caption": "1" }, { "caption": "2" }, { "caption": "3" },
                { "caption": "4" }, { "caption": "5" } ] }""")).result().isPresent(),
            "a page is a four-panel strip");
    }

    @Test
    void pagesComeInOrderAndABrokenOneIsLeftOut() {
        ManualLoader.Loaded loaded = ManualLoader.load(Map.of(
            id("later"), json("{ \"order\": 20, \"title\": \"later\", \"panels\": [ { \"caption\": \"c\" } ] }"),
            id("first"), json("{ \"order\": 10, \"title\": \"first\", \"panels\": [ { \"caption\": \"c\" } ] }"),
            id("broken"), json("{ \"title\": \"broken\", \"panels\": [] }")));

        assertEquals(List.of("first", "later"), loaded.pages().stream().map(ManualPage::title).toList());
        assertEquals(1, loaded.errors().size());
    }

    private static ManualPage parse(String text) {
        return ManualPage.CODEC.parse(JsonOps.INSTANCE, json(text)).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);
    }

    private static JsonElement json(String text) {
        return JsonParser.parseString(text);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("chiikawa", path);
    }
}
