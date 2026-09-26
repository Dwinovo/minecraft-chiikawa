package com.dwinovo.chiikawa.anim.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedBone;
import com.dwinovo.chiikawa.anim.baked.BakedCube;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.compile.AnimationBaker;
import com.dwinovo.chiikawa.anim.compile.ModelBaker;
import com.dwinovo.chiikawa.anim.format.BedrockGeoFile;
import com.dwinovo.chiikawa.anim.molang.MolangContext;
import com.dwinovo.chiikawa.anim.render.layer.BoneTransformWalker;
import com.dwinovo.chiikawa.anim.runtime.AnimationChannel;
import com.dwinovo.chiikawa.anim.runtime.PoseSampler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * Where a pet ends up when it takes a pose, worked out the way the renderer draws it: the
 * animation sampled by the engine, every bone and cube turned as {@link ModelRenderer}
 * turns it. The pets were remade on one build after their animations were made, and a
 * pose made for the old model can put the new one through the floor; Blockbench's floor
 * is the model's y = 0, and so is the game's.
 */
class PetPosesTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/chiikawa");
    private static final Gson GSON = new Gson();
    private static final String SIT = "sit";
    /** A quarter of a pixel either way: closer than anyone can see against the ground. */
    private static final float GROUND_SLACK = 0.25F;

    /** A sitting pet sits on the ground: not sunk into it, and not hovering above it. */
    @Test
    void everyPetSitsOnTheGround() throws IOException {
        List<String> pets = pets();
        assertFalse(pets.isEmpty(), "no pets found under " + ASSETS);
        List<String> wrong = new ArrayList<>();
        for (String pet : pets) {
            float lowest = lowestPoint(pet, SIT);
            if (Math.abs(lowest) > GROUND_SLACK) {
                wrong.add(pet + " sits with its lowest point at " + lowest + " px");
            }
        }
        assertTrue(wrong.isEmpty(), String.join("; ", wrong));
    }

    /** The lowest point of any cube of the pet, in pixels above the ground, at the start of an animation. */
    private static float lowestPoint(String pet, String animation) throws IOException {
        BakedModel model = model(pet);
        BakedAnimation baked = AnimationBaker.bake(animations(pet), model).get(animation);
        assertNotNull(baked, pet + " has no " + animation + " animation");

        float[] pose = new float[model.bones.length * PoseSampler.FLOATS_PER_BONE];
        PoseSampler.resetIdentity(pose, model.bones.length);
        PoseSampler.sample(new AnimationChannel(baked, 0L, true), 0L, new MolangContext(), pose);

        BoneTransformWalker walker = new BoneTransformWalker();
        Quaternionf turn = new Quaternionf();
        Vector3f corner = new Vector3f();
        float lowest = Float.MAX_VALUE;
        for (int b = 0; b < model.bones.length; b++) {
            BakedBone bone = model.bones[b];
            if (bone.cubeCount == 0) {
                continue;
            }
            PoseStack stack = new PoseStack();
            // The walker stops at the bone's pivot; the renderer goes on back from it.
            walker.transformToBone(model, pose, b, stack);
            stack.translate(-bone.pivotX, -bone.pivotY, -bone.pivotZ);
            for (int c = bone.cubeStart; c < bone.cubeStart + bone.cubeCount; c++) {
                BakedCube cube = model.cubes[c];
                stack.pushPose();
                if (cube.hasRotation) {
                    stack.last().rotateAround(BedrockRotation.of(turn, cube.rotX, cube.rotY, cube.rotZ),
                        cube.pivotX, cube.pivotY, cube.pivotZ);
                }
                Matrix4f matrix = stack.last().pose();
                for (int i = 0; i < 8; i++) {
                    matrix.transformPosition(
                        (i & 1) != 0 ? cube.maxX : cube.minX,
                        (i & 2) != 0 ? cube.maxY : cube.minY,
                        (i & 4) != 0 ? cube.maxZ : cube.minZ, corner);
                    lowest = Math.min(lowest, corner.y);
                }
                stack.popPose();
            }
        }
        return lowest;
    }

    /** The pets: every geo model with an animation file beside it. */
    private static List<String> pets() throws IOException {
        try (Stream<Path> files = Files.list(ASSETS.resolve("models/entity"))) {
            return files.map(file -> file.getFileName().toString().replace(".json", ""))
                .filter(name -> Files.exists(ASSETS.resolve("animations/" + name + ".json")))
                .sorted()
                .toList();
        }
    }

    private static BakedModel model(String pet) throws IOException {
        try (Reader reader = Files.newBufferedReader(ASSETS.resolve("models/entity/" + pet + ".json"))) {
            return ModelBaker.bake(GSON.fromJson(reader, BedrockGeoFile.class));
        }
    }

    private static JsonObject animations(String pet) throws IOException {
        try (Reader reader = Files.newBufferedReader(ASSETS.resolve("animations/" + pet + ".json"))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
