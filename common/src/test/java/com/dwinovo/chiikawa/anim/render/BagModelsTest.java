package com.dwinovo.chiikawa.anim.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.anim.baked.BakedBone;
import com.dwinovo.chiikawa.anim.baked.BakedCube;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.compile.ModelBaker;
import com.dwinovo.chiikawa.anim.format.BedrockGeoFile;
import com.dwinovo.chiikawa.item.BagItem;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * What the art has to give the code for a bag to show: every pet a place to hang each
 * kind of bag and a strap to hold it, and every bag a model and a texture of its own.
 * A pet is a model with animations; a bag is a model without.
 */
class BagModelsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/chiikawa");
    private static final Gson GSON = new Gson();

    @Test
    void everyPetHasABoneToHangEachKindOfBagFromAndAStrapToHoldIt() throws IOException {
        List<String> pets = modelNames(true);
        assertFalse(pets.isEmpty(), "no pet models found under " + ASSETS);
        for (String pet : pets) {
            BakedModel model = bake(pet);
            for (BagItem.Wear wear : BagItem.Wear.values()) {
                BakedBone locator = bone(model, pet, wear.locator());
                BakedBone strap = bone(model, pet, wear.strap());
                assertEquals(0, locator.cubeCount, pet + "'s " + wear.locator() + " still has a preview bag in it");
                assertTrue(strap.cubeCount > 0, pet + "'s " + wear.strap() + " has no strap in it");
                // Both on the same part of the body, so the bag and its strap move as one.
                assertEquals(locator.parentIdx, strap.parentIdx,
                    pet + "'s " + wear + " bag and strap hang from different bones");
            }
        }
    }

    @Test
    void everyBagIsOneBoneCentredOnItsOriginWithATextureOfItsOwn() throws IOException {
        List<String> bags = modelNames(false);
        assertFalse(bags.isEmpty(), "no bag models found under " + ASSETS);
        for (String bag : bags) {
            BakedModel model = bake(bag);
            assertEquals(1, model.bones.length, bag + " is not one bone");
            assertTrue(Files.exists(ASSETS.resolve("textures/entities/" + bag + ".png")), bag + " has no texture");
            // Centred, so a pet's bone says where the middle of the bag goes.
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (BakedCube cube : model.cubes) {
                minX = Math.min(minX, cube.minX);
                maxX = Math.max(maxX, cube.maxX);
                minY = Math.min(minY, cube.minY);
                maxY = Math.max(maxY, cube.maxY);
            }
            assertEquals(0.0F, (minX + maxX) / 2.0F, 0.25F, bag + " is off centre across");
            assertEquals(0.0F, (minY + maxY) / 2.0F, 0.25F, bag + " is off centre up and down");
        }
    }

    /** The geo models with an animation file (the pets) or without one (the bags). */
    private static List<String> modelNames(boolean animated) throws IOException {
        try (Stream<Path> files = Files.list(ASSETS.resolve("models/entity"))) {
            return files.map(file -> file.getFileName().toString().replace(".json", ""))
                .filter(name -> Files.exists(ASSETS.resolve("animations/" + name + ".json")) == animated)
                .sorted()
                .toList();
        }
    }

    private static BakedModel bake(String name) throws IOException {
        try (Reader reader = Files.newBufferedReader(ASSETS.resolve("models/entity/" + name + ".json"))) {
            return ModelBaker.bake(GSON.fromJson(reader, BedrockGeoFile.class));
        }
    }

    private static BakedBone bone(BakedModel model, String pet, String name) {
        Integer idx = model.boneIndex.get(name);
        assertNotNull(idx, pet + " has no " + name);
        return model.bones[idx];
    }
}
