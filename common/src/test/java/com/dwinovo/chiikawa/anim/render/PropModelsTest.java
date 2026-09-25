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
import com.dwinovo.chiikawa.client.render.LaborBoardRenderer;
import com.dwinovo.chiikawa.data.LaborBoardLevelData;
import com.dwinovo.chiikawa.item.BagItem;
import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * What the art has to give the code. Every pet needs a place to hang each kind of bag and a
 * strap to hold it; every prop — a model without animations — needs a texture, and needs to
 * be one the code draws: a bag, hung from its centre, or a block — the labor board, with a
 * plate for every slip the mod's own levels put up, or the shop, standing inside its block.
 * A pack that gives boards more slips than that has them hang without a plate each. The
 * handbook is a prop too, only ever held, and so are the pets' weapons, each standing up
 * with its texture fit for the block atlas its bits are drawn from when it breaks.
 */
class PropModelsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/chiikawa");
    private static final Gson GSON = new Gson();
    private static final List<String> BAGS = List.of("backpack", "bear_pouch", "whale_pouch", "star_pouch");
    private static final String LABOR_BOARD = "labor_board";
    private static final String SHOP = "shop";
    private static final List<String> BLOCKS = List.of(LABOR_BOARD, SHOP);
    /** Props that are only ever an item in the hand or on a shelf. */
    private static final List<String> ITEMS = List.of("handbook");
    /** Held as vanilla holds a sword, laid corner to corner from a model standing up. */
    private static final List<String> WEAPONS = List.of("chiikawa_weapon", "hachiware_weapon", "usagi_weapon", "rakko_sword");
    /** Vanilla bakes the block atlas four mip levels deep, which it keeps only if every sprite's sides divide by this. */
    private static final int MIP_STEP = 16;
    private static final float HALF_BLOCK = 8.0F;
    private static final float EPSILON = 1.0E-3F;

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
    void everyPropIsOneTheCodeDrawsAndHasATexture() throws IOException {
        List<String> props = modelNames(false);
        for (String prop : props) {
            assertTrue(BAGS.contains(prop) || BLOCKS.contains(prop) || ITEMS.contains(prop) || WEAPONS.contains(prop),
                prop + " is a model nothing draws");
            assertTrue(Files.exists(ASSETS.resolve("textures/entities/" + prop + ".png")), prop + " has no texture");
        }
        assertTrue(props.containsAll(BAGS) && props.containsAll(BLOCKS) && props.containsAll(ITEMS)
                && props.containsAll(WEAPONS),
            "a prop the code draws has no model: " + props);
    }

    @Test
    void everyBagIsOneBoneHungFromItsCentre() throws IOException {
        for (String bag : BAGS) {
            BakedModel model = bake(bag);
            assertEquals(1, model.bones.length, bag + " is not one bone");
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

    /**
     * A weapon is modelled standing up, the end it is held by at the bottom, so that leaning
     * it corner to corner lays it as a sword's sprite lies. Its texture goes on the block
     * atlas, and a sprite whose sides do not divide by sixteen takes every block's distant
     * mipmaps down with it.
     */
    @Test
    void everyWeaponStandsUpAndFitsTheBlockAtlas() throws IOException {
        for (String weapon : modelNames(false).stream().filter(WEAPONS::contains).toList()) {
            BakedModel model = bake(weapon);
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            for (BakedCube cube : model.cubes) {
                minX = Math.min(minX, cube.minX);
                maxX = Math.max(maxX, cube.maxX);
                minY = Math.min(minY, cube.minY);
                maxY = Math.max(maxY, cube.maxY);
                minZ = Math.min(minZ, cube.minZ);
                maxZ = Math.max(maxZ, cube.maxZ);
            }
            assertTrue(maxY - minY > maxX - minX && maxY - minY > maxZ - minZ, weapon + " does not stand up");
            BufferedImage texture = ImageIO.read(ASSETS.resolve("textures/entities/" + weapon + ".png").toFile());
            assertTrue(texture.getWidth() % MIP_STEP == 0 && texture.getHeight() % MIP_STEP == 0,
                weapon + "'s texture is " + texture.getWidth() + "x" + texture.getHeight()
                    + ", which lowers the block atlas's mip levels");
        }
    }

    @Test
    void theLaborBoardHasAPlateForEverySlipItsOwnLevelsPutUp() throws IOException {
        BakedModel model = bake(LABOR_BOARD);
        int most = LaborBoardLevelData.LEVELS.slipsAt(LaborBoardLevelData.LEVELS.top());
        for (int place = 0; place < most; place++) {
            assertTrue(bone(model, LABOR_BOARD, LaborBoardRenderer.PLATE_BONE + place).cubeCount > 0,
                "plate " + place + " has nothing on it");
        }
        assertEquals(null, model.boneIndex.get(LaborBoardRenderer.PLATE_BONE + most),
            "the board has a plate no board ever puts up");
    }

    /**
     * The shop stays inside its block, awning and all, so nothing it draws pokes into the
     * block above it or beside it, and its outline covers all of it.
     */
    @Test
    void theShopStandsInsideItsBlock() throws IOException {
        BakedModel model = bake(SHOP);
        for (BakedCube cube : model.cubes) {
            Matrix4f turn = new Matrix4f()
                .translate(cube.pivotX, cube.pivotY, cube.pivotZ)
                .rotate(BedrockRotation.of(new Quaternionf(), cube.rotX, cube.rotY, cube.rotZ))
                .translate(-cube.pivotX, -cube.pivotY, -cube.pivotZ);
            for (int corner = 0; corner < 8; corner++) {
                Vector3f at = turn.transformPosition(new Vector3f(
                    (corner & 1) != 0 ? cube.maxX : cube.minX,
                    (corner & 2) != 0 ? cube.maxY : cube.minY,
                    (corner & 4) != 0 ? cube.maxZ : cube.minZ));
                assertTrue(Math.abs(at.x) <= HALF_BLOCK + EPSILON && Math.abs(at.z) <= HALF_BLOCK + EPSILON
                    && at.y >= -EPSILON && at.y <= 2 * HALF_BLOCK + EPSILON, "the shop reaches out of its block at " + at);
            }
        }
    }

    /** The geo models with an animation file (the pets) or without one (the props). */
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

    private static BakedBone bone(BakedModel model, String owner, String name) {
        Integer idx = model.boneIndex.get(name);
        assertNotNull(idx, owner + " has no " + name);
        return model.bones[idx];
    }
}
