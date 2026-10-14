package com.dwinovo.chiikawa.anim.compile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** Only this mod's own geo and animation files are read, whatever else the packs hold. */
class BedrockResourceLoaderTest {
    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    @Test
    void ourOwnJsonIsRead() {
        assertTrue(BedrockResourceLoader.isOurs(id("chiikawa", "models/entity/usagi.json")));
        assertTrue(BedrockResourceLoader.isOurs(id("chiikawa", "animations/usagi.json")));
    }

    @Test
    void otherModsModelsAreLeftAlone() {
        assertFalse(BedrockResourceLoader.isOurs(id("create", "models/entity/belt.json")));
        assertFalse(BedrockResourceLoader.isOurs(id("minecraft", "models/entity/connectiblechains/copper_chain.json")),
            "a mod that keeps its models under the vanilla namespace");
    }

    @Test
    void whatIsNotJsonIsLeftAlone() {
        assertFalse(BedrockResourceLoader.isOurs(id("chiikawa", "models/entity/usagi.png")));
    }
}
