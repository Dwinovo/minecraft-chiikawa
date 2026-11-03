package com.dwinovo.chiikawa.anim.pose;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedBone;
import com.dwinovo.chiikawa.anim.baked.BakedBoneChannel;
import com.dwinovo.chiikawa.anim.baked.BakedCube;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.baked.LoopMode;
import com.dwinovo.chiikawa.anim.compile.AnimationBaker;
import com.dwinovo.chiikawa.anim.controller.BlendMode;
import com.dwinovo.chiikawa.anim.controller.ControllerSnapshot;
import com.dwinovo.chiikawa.anim.molang.MolangContext;
import com.dwinovo.chiikawa.anim.runtime.AnimationChannel;
import com.dwinovo.chiikawa.anim.runtime.PoseSampler;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What the entity looks like is the controllers in order, then the interceptors in stage order. */
class PoseComposerTest {
    private static final int ROT_X = PoseSampler.OFFSET_ROT;

    private static BakedModel oneBone() {
        BakedBone[] bones = {new BakedBone("Root", -1, 0, 0, 0, 0, 0, 0, false, 0, 0, new int[0])};
        return new BakedModel(bones, new BakedCube[0], new int[] {0}, Map.of("Root", 0), 64, 64);
    }

    /** An animation that holds the root's X rotation at {@code radians}. */
    private static BakedAnimation holding(String name, float radians) {
        BakedBoneChannel channel = new BakedBoneChannel(0, BakedBoneChannel.TYPE_ROTATION, true,
            new float[0], new float[] {radians, 0f, 0f}, new byte[0], null);
        return new BakedAnimation(name, 1f, LoopMode.LOOP, new BakedBoneChannel[] {channel});
    }

    private static ControllerSnapshot playing(String name, BlendMode mode, BakedAnimation animation) {
        return new ControllerSnapshot(name, mode, new AnimationChannel(animation, 0L, true), null, 0L, 0f, false);
    }

    private static PoseFrame frame(ControllerSnapshot... snapshots) {
        PoseFrame frame = new PoseFrame();
        frame.controllerSnapshots = snapshots;
        return frame;
    }

    @Test
    void aLaterControllerOverridesAnEarlierOneAndAnAdditiveOneAddsToIt() {
        PoseComposer composer = new PoseComposer();

        float[] overridden = composer.compose(oneBone(), frame(
            playing("main", BlendMode.OVERRIDE, holding("a", 0.2f)),
            playing("action", BlendMode.OVERRIDE, holding("b", 0.5f))));
        float[] added = composer.compose(oneBone(), frame(
            playing("main", BlendMode.OVERRIDE, holding("a", 0.2f)),
            playing("breath", BlendMode.ADDITIVE, holding("b", 0.5f))));

        assertEquals(0.5f, overridden[ROT_X], 1e-6f);
        assertEquals(0.7f, added[ROT_X], 1e-6f);
    }

    @Test
    void aControllerGoingSilentFadesBackToWhatEarlierOnesWrote() {
        PoseComposer composer = new PoseComposer();
        BakedAnimation main = holding("a", 0.2f);
        BakedAnimation action = holding("b", 0.6f);
        ControllerSnapshot stopping = new ControllerSnapshot("action", BlendMode.OVERRIDE,
            new AnimationChannel(action, 0L, true), null, 0L, 1f, true);
        PoseFrame halfway = frame(playing("main", BlendMode.OVERRIDE, main), stopping);
        halfway.animationTimeNs = 500_000_000L;

        float[] pose = composer.compose(oneBone(), halfway);

        assertEquals(0.4f, pose[ROT_X], 1e-6f, "half way from the action's 0.6 back to main's 0.2");
    }

    /** A model with the two bones the layering tests tell apart: an eyelid, and everything else. */
    private static BakedModel eyelidAndRest() {
        BakedBone[] bones = {
            new BakedBone("Eyelid", -1, 0, 0, 0, 0, 0, 0, false, 0, 0, new int[0]),
            new BakedBone("Rest", -1, 0, 0, 0, 0, 0, 0, false, 0, 0, new int[0])};
        return new BakedModel(bones, new BakedCube[0], new int[] {0, 1}, Map.of("Eyelid", 0, "Rest", 1), 64, 64);
    }

    /** An animation that holds one channel of one bone constant and keys nothing else. */
    private static BakedAnimation holding(String name, int bone, byte type, float x, float y, float z) {
        BakedBoneChannel channel = new BakedBoneChannel(bone, type, true,
            new float[0], new float[] {x, y, z}, new byte[0], null);
        return new BakedAnimation(name, 1f, LoopMode.LOOP, new BakedBoneChannel[] {channel});
    }

    /** What the eyelid is open as: the blink's, and the sleep's, the way the pets' own animations key it. */
    private static BakedAnimation eyelidScaleY(String name, float y) {
        return holding(name, 0, BakedBoneChannel.TYPE_SCALE, 1f, y, 1f);
    }

    private static final int EYELID_SCALE_Y = PoseSampler.OFFSET_SCALE + 1;

    @Test
    void aChannelTheUpperLayerDoesNotKeyShowsWhatTheLowerLayerWrote() {
        PoseComposer composer = new PoseComposer();
        BakedAnimation blink = eyelidScaleY("blink", 0.5f);
        // The base loop moves the rest of the body and says nothing about the eyelid.
        BakedAnimation idle = holding("idle", 1, BakedBoneChannel.TYPE_POSITION, 0f, 3f, 0f);

        float[] pose = composer.compose(eyelidAndRest(), frame(
            playing("blink", BlendMode.OVERRIDE, blink),
            playing("main", BlendMode.OVERRIDE, idle)));

        assertEquals(0.5f, pose[EYELID_SCALE_Y], 1e-6f, "the blink shows through");
        assertEquals(3f, pose[PoseSampler.FLOATS_PER_BONE + PoseSampler.OFFSET_POS + 1], 1e-6f, "and the base loop is drawn");
    }

    @Test
    void aChannelTheUpperLayerKeysIsTheUpperLayersEvenWhereTheLowerOneAlsoKeysIt() {
        PoseComposer composer = new PoseComposer();

        float[] pose = composer.compose(eyelidAndRest(), frame(
            playing("blink", BlendMode.OVERRIDE, eyelidScaleY("blink", 0.5f)),
            playing("main", BlendMode.OVERRIDE, eyelidScaleY("sleep", 0.07f))));

        assertEquals(0.07f, pose[EYELID_SCALE_Y], 1e-6f);
    }

    @Test
    void aBaseLoopCrossfadingOutOfAShutEyeEndsAtTheBlinkNotAtIdentityOrTheSleep() {
        PoseComposer composer = new PoseComposer();
        ControllerSnapshot blink = playing("blink", BlendMode.OVERRIDE, eyelidScaleY("blink", 0.5f));
        BakedAnimation sleep = eyelidScaleY("sleep", 0.07f);
        BakedAnimation idle = holding("idle", 1, BakedBoneChannel.TYPE_POSITION, 0f, 3f, 0f);
        long fadeStart = 1_000_000_000L;
        ControllerSnapshot main = new ControllerSnapshot("main", BlendMode.OVERRIDE,
            new AnimationChannel(idle, 0L, true), new AnimationChannel(sleep, 0L, true), fadeStart, 1f, false);

        PoseFrame begun = frame(blink, main);
        begun.animationTimeNs = fadeStart;
        PoseFrame halfway = frame(blink, main);
        halfway.animationTimeNs = fadeStart + 500_000_000L;
        PoseFrame done = frame(blink, main);
        done.animationTimeNs = fadeStart + 1_000_000_000L;

        assertEquals(0.07f, composer.compose(eyelidAndRest(), begun)[EYELID_SCALE_Y], 1e-6f);
        assertEquals(0.285f, composer.compose(eyelidAndRest(), halfway)[EYELID_SCALE_Y], 1e-6f,
            "half way from the sleep's 0.07 to the blink's 0.5, the idle having nothing to say of it");
        assertEquals(0.5f, composer.compose(eyelidAndRest(), done)[EYELID_SCALE_Y], 1e-6f);
    }

    @Test
    void anIdentityTheUpperLayerKeysIsPrunedAndSoLetsTheLowerLayerShow() {
        PoseComposer composer = new PoseComposer();
        BakedModel model = eyelidAndRest();
        BakedAnimation open = AnimationBaker.bake(JsonParser.parseString(
            "{\"animations\": {\"open\": {\"animation_length\": 1, \"bones\": {\"Eyelid\": {\"scale\": 1}}}}}")
            .getAsJsonObject(), model).get("open");

        assertEquals(0, open.channels.length, "an eyelid held at exactly the identity is no channel at all");
        float[] pose = composer.compose(model, frame(
            playing("blink", BlendMode.OVERRIDE, eyelidScaleY("blink", 0.5f)),
            playing("main", BlendMode.OVERRIDE, open)));
        assertEquals(0.5f, pose[EYELID_SCALE_Y], 1e-6f);
    }

    @Test
    void interceptorsRunAfterTheControllersAndByStageAndSeeWhatTheInputsFilled() {
        PoseComposer composer = new PoseComposer();
        List<String> order = new ArrayList<>();
        double[] speedSeen = new double[1];
        composer.addInputProvider((frame, ctx) -> {
            order.add("input");
            ctx.vars[MolangContext.SLOT_GROUND_SPEED] = frame.walkSpeed;
        });
        // Registered first, but its stage is later.
        composer.addInterceptor(BoneInterceptor.Stage.PHYSICS_SECONDARY, (model, frame, ctx, pose) -> {
            order.add("physics");
            pose[ROT_X] += 1f;
        });
        composer.addInterceptor(BoneInterceptor.Stage.LOOK_AT, (model, frame, ctx, pose) -> {
            order.add("look");
            speedSeen[0] = ctx.vars[MolangContext.SLOT_GROUND_SPEED];
            pose[ROT_X] = pose[ROT_X] * 10f;
        });
        PoseFrame frame = frame(playing("main", BlendMode.OVERRIDE, holding("a", 0.2f)));
        frame.walkSpeed = 0.75f;

        float[] pose = composer.compose(oneBone(), frame);

        assertEquals(List.of("input", "look", "physics"), order);
        assertEquals(0.75, speedSeen[0], 1e-6);
        assertEquals(3f, pose[ROT_X], 1e-6f, "the look-at saw the controllers' 0.2, physics saw the look-at's result");
    }

    @Test
    void everyCallHandsBackABufferOfItsOwn() {
        PoseComposer composer = new PoseComposer();
        PoseFrame frame = frame();

        float[] first = composer.compose(oneBone(), frame);
        first[ROT_X] = 9f;
        float[] second = composer.compose(oneBone(), frame);

        assertEquals(0f, second[ROT_X]);
    }
}
