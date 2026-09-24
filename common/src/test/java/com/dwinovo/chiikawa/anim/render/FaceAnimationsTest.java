package com.dwinovo.chiikawa.anim.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * A face that moves: the blink every pet with one loops, the open mouth, and the faces it
 * pulls. What these move has to be there in the pet's model — a bone an animation names and
 * the model does not have is a part of the face that silently stays still.
 */
class FaceAnimationsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/chiikawa");
    private static final List<String> FACE = List.of("blink", "open_mouth1", "open_mouth2");
    private static final List<String> EXPRESSIONS = List.of("happy", "hurt", "confused", "scratch_head", "revive");
    private static final List<String> PETS = List.of("chiikawa", "furuhonya", "hachiware", "kurimanju", "momonga",
        "rakko", "shisa", "usagi");

    @Test
    void aFaceMovesOnlyWhatThePetsModelHas() throws IOException {
        for (String pet : pets()) {
            Set<String> bones = modelBones(pet);
            JsonObject animations = read(ASSETS.resolve("animations/" + pet + ".json")).getAsJsonObject("animations");
            for (String face : Stream.concat(FACE.stream(), EXPRESSIONS.stream()).toList()) {
                if (!animations.has(face)) {
                    continue;
                }
                for (String bone : animations.getAsJsonObject(face).getAsJsonObject("bones").keySet()) {
                    assertTrue(bones.contains(bone), pet + "'s " + face + " moves " + bone + ", which its model has not got");
                }
            }
        }
    }

    @Test
    void facesBuiltFromBonesBlinkAndTalk() throws IOException {
        for (String pet : List.of("chiikawa", "furuhonya", "kurimanju", "momonga", "rakko", "shisa", "usagi")) {
            JsonObject animations = read(ASSETS.resolve("animations/" + pet + ".json")).getAsJsonObject("animations");
            for (String face : FACE) {
                assertTrue(animations.has(face), pet + " has no " + face);
            }
            assertTrue(modelBones(pet).containsAll(List.of("LeftEyelid", "RightEyelid", "Mouth", "Mouth3")),
                pet + "'s eyes and mouth are painted on again");
        }
    }

    /**
     * Every pet has a face for each reaction the game shows (design 0.1.1, section 4): pleased
     * and hurt of its own, and puzzled, as {@code confused} or the older {@code scratch_head}.
     * Being brought back falls back to pleased where a pet has no face of its own for it.
     */
    @Test
    void everyPetHasAFaceForEveryReaction() throws IOException {
        for (String pet : PETS) {
            JsonObject animations = read(ASSETS.resolve("animations/" + pet + ".json")).getAsJsonObject("animations");
            assertTrue(animations.has("happy"), pet + " has no happy face");
            assertTrue(animations.has("hurt"), pet + " has no hurt face");
            assertTrue(animations.has("confused") || animations.has("scratch_head"), pet + " has no puzzled face");
        }
    }

    private static List<String> pets() throws IOException {
        try (Stream<Path> files = Files.list(ASSETS.resolve("animations"))) {
            return files.map(file -> file.getFileName().toString().replace(".json", "")).sorted().toList();
        }
    }

    private static Set<String> modelBones(String pet) throws IOException {
        Set<String> names = new HashSet<>();
        read(ASSETS.resolve("models/entity/" + pet + ".json")).getAsJsonArray("minecraft:geometry").get(0)
            .getAsJsonObject().getAsJsonArray("bones")
            .forEach(bone -> names.add(bone.getAsJsonObject().get("name").getAsString()));
        return names;
    }

    private static JsonObject read(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
