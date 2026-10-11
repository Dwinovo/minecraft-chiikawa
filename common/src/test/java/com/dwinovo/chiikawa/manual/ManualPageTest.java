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
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ManualPageTest {
    @Test
    void aPackWritesAChapterAsATitleATextALinkAndStageDirections() {
        ManualPage page = parse("""
            { "order": 5, "title": "t", "text": "x", "link": "play/work", "scene": { "actors": [
                { "pet": "chiikawa:usagi", "x": 0.3, "facing": 20, "hold": "minecraft:wooden_hoe",
                  "action": "harvest", "every": 24, "say": "s" },
                { "prop": "chiikawa:labor_board", "x": 0.7, "offset": [0, 4, 11] },
                { "item": "#chiikawa:currency", "y": 0.4, "bob": true } ] } }""");

        assertEquals("x", page.text());
        assertEquals("play/work", page.link());
        ManualPage.Actor usagi = page.scene().actors().get(0);
        assertEquals(PetAction.HARVEST, usagi.action().orElseThrow());
        assertEquals(24, usagi.motion().every());
        assertEquals("s", usagi.motion().say().orElseThrow());
        assertEquals(new Vec3(0, 4, 11), page.scene().actors().get(1).offset());
        assertEquals(Vec3.ZERO, usagi.offset(), "an actor stands on its spot when nothing says otherwise");
        ManualPage.Actor money = page.scene().actors().get(2);
        assertTrue(money.item().orElseThrow().tag(), "money is named by its tag");
        assertTrue(money.motion().bob());
        assertEquals(0.5F, money.x(), "and stands in the middle when nothing says otherwise");
    }

    @Test
    void aChapterNeedsATextAndALink() {
        assertFalse(ManualPage.CODEC.parse(JsonOps.INSTANCE, json("""
            { "title": "t", "link": "play/work", "scene": {} }""")).isSuccess(), "no text");
        assertFalse(ManualPage.CODEC.parse(JsonOps.INSTANCE, json("""
            { "title": "t", "text": "x", "scene": {} }""")).isSuccess(), "no link");
        assertTrue(ManualPage.CODEC.parse(JsonOps.INSTANCE, json("""
            { "title": "t", "text": "x", "link": "play/work", "scene": {} }""")).isSuccess(), "an empty scene is a scene");
    }

    @Test
    void anActorIsExactlyOneThing() {
        assertFalse(ManualPage.CODEC.parse(JsonOps.INSTANCE, json("""
            { "title": "t", "text": "x", "link": "l", "scene": { "actors": [
                { "pet": "chiikawa:usagi", "prop": "chiikawa:shop" } ] } }""")).isSuccess());
        assertFalse(ManualPage.CODEC.parse(JsonOps.INSTANCE, json("""
            { "title": "t", "text": "x", "link": "l", "scene": { "actors": [ { "x": 0.2 } ] } }""")).isSuccess());
    }

    @Test
    void aMoveNobodyKnowsIsRefused() {
        assertFalse(ManualPage.CODEC.parse(JsonOps.INSTANCE, json("""
            { "title": "t", "text": "x", "link": "l", "scene": { "actors": [
                { "pet": "chiikawa:usagi", "action": "moonwalk" } ] } }""")).isSuccess());
    }

    @Test
    void chaptersComeInOrderAndABrokenOneIsLeftOut() {
        ManualLoader.Loaded loaded = ManualLoader.load(Map.of(
            id("later"), json("{ \"order\": 20, \"title\": \"later\", \"text\": \"x\", \"link\": \"l\", \"scene\": {} }"),
            id("first"), json("{ \"order\": 10, \"title\": \"first\", \"text\": \"x\", \"link\": \"l\", \"scene\": {} }"),
            id("broken"), json("{ \"title\": \"broken\", \"scene\": {} }")));

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
