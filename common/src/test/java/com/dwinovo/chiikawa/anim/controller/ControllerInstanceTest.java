package com.dwinovo.chiikawa.anim.controller;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedBoneChannel;
import com.dwinovo.chiikawa.anim.baked.LoopMode;
import com.dwinovo.chiikawa.anim.molang.MolangContext;
import com.dwinovo.chiikawa.anim.runtime.PoseSampler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pinpoint behaviour tests for the GeckoLib-aligned controller pipeline.
 *
 * <p>Covered:
 * <ul>
 *   <li>{@link ControllerInstance#playOnce} overrides the state handler until
 *       the triggered animation finishes.</li>
 *   <li>{@code HOLD_ON_LAST_FRAME} triggers persist past their duration.</li>
 *   <li>{@link BlendMode#ADDITIVE} composes per channel-type as expected:
 *       rotation/position add, scale multiplies.</li>
 *   <li>State handler returning {@code null} means no contribution that frame.</li>
 * </ul>
 */
class ControllerInstanceTest {

    private static BakedAnimation playOnceAnim(String name, float duration) {
        return new BakedAnimation(name, duration, LoopMode.PLAY_ONCE, new BakedBoneChannel[0]);
    }

    private static BakedAnimation loopAnim(String name) {
        return new BakedAnimation(name, 1f, LoopMode.LOOP, new BakedBoneChannel[0]);
    }

    private static BakedAnimation holdAnim(String name, float duration) {
        return new BakedAnimation(name, duration, LoopMode.HOLD_ON_LAST_FRAME, new BakedBoneChannel[0]);
    }

    @Test
    void triggerOverridesHandlerUntilFinished() {
        BakedAnimation idle = loopAnim("idle");
        BakedAnimation harvest = playOnceAnim("harvest", 1f);
        ControllerConfig config = new ControllerConfig("main", BlendMode.OVERRIDE, 0f,
                (state, ctx) -> idle);
        ControllerInstance c = new ControllerInstance(config);

        long t0 = 1_000_000_000L;
        c.tick(null, null, t0);
        assertSame(idle, c.snapshot().current().animation(), "before trigger: handler chooses idle");

        c.playOnce(harvest, t0);
        c.tick(null, null, t0);
        assertSame(harvest, c.snapshot().current().animation(), "while triggered: harvest wins");

        // 0.5s into harvest — still triggered.
        c.tick(null, null, t0 + 500_000_000L);
        assertSame(harvest, c.snapshot().current().animation(), "mid-trigger: still harvest");

        // Past harvest's 1s duration — handler resumes.
        c.tick(null, null, t0 + 1_500_000_000L);
        assertSame(idle, c.snapshot().current().animation(), "post-trigger: handler resumes");
    }

    @Test
    void holdOnLastFrameDoesNotAutoFinish() {
        BakedAnimation hold = holdAnim("freeze", 1f);
        ControllerConfig config = new ControllerConfig("react", BlendMode.OVERRIDE, 0f,
                ControllerHandler::neverPlay);
        ControllerInstance c = new ControllerInstance(config);

        long t0 = 1_000_000_000L;
        c.playOnce(hold, t0);
        c.tick(null, null, t0 + 5_000_000_000L); // way past duration
        assertSame(hold, c.snapshot().current().animation(),
                "HOLD_ON_LAST_FRAME ignores the duration timeout");
    }

    @Test
    void clearTriggerReturnsControlToHandler() {
        BakedAnimation idle = loopAnim("idle");
        BakedAnimation hold = holdAnim("scratch", 0.5f);
        ControllerConfig config = new ControllerConfig("main", BlendMode.OVERRIDE, 0f,
                (state, ctx) -> idle);
        ControllerInstance c = new ControllerInstance(config);

        c.playOnce(hold, 0L);
        c.tick(null, null, 0L);
        assertSame(hold, c.snapshot().current().animation());

        c.clearTrigger();
        c.tick(null, null, 1_000_000_000L);
        assertSame(idle, c.snapshot().current().animation(), "after clearTrigger: handler resumes");
    }

    @Test
    void handlerReturningNullProducesIdleSnapshot() {
        ControllerConfig config = new ControllerConfig("decoration", BlendMode.OVERRIDE, 0f,
                ControllerHandler::neverPlay);
        ControllerInstance c = new ControllerInstance(config);

        c.tick(null, null, 0L);
        assertTrue(c.snapshot().isIdle(), "no animation chosen → idle snapshot");
        assertNull(c.snapshot().current(), "current channel must be null when idle");
    }

    @Test
    void withinControllerFadeUsesNowNsNotPhaseSeed() {
        // Regression: fadeStartNs used to be set to the new animation's startNs,
        // which for looping animations is the (very old) phase seed. That made
        // the fade alpha jump to 1.0 on the first frame, hiding the crossfade
        // entirely (idle → sit was visually a hard cut).
        BakedAnimation idle = loopAnim("idle");
        BakedAnimation sit  = loopAnim("sit");
        boolean[] selectSit = {false};
        ControllerConfig config = new ControllerConfig("main", BlendMode.OVERRIDE, 0.16f,
                (state, ctx) -> selectSit[0] ? sit : idle);
        ControllerInstance c = new ControllerInstance(config);

        // Latch a phase seed several seconds in the past, like a real entity would.
        long phaseSeed = 1_000_000_000L;
        c.setPhaseSeed(phaseSeed);

        long t0 = phaseSeed + 5_000_000_000L; // 5s "later"
        c.tick(null, null, t0);
        assertSame(idle, c.snapshot().current().animation());
        assertNull(c.snapshot().previous(), "no previous before switch");

        // Trigger the switch to sit.
        selectSit[0] = true;
        c.tick(null, null, t0);
        ControllerSnapshot s = c.snapshot();
        assertSame(sit,  s.current().animation(),  "current is now sit");
        assertSame(idle, s.previous().animation(), "previous is idle (fading out)");
        assertEquals(t0, s.fadeStartNs(),
                "fadeStartNs MUST be the wall-clock now, not the new animation's startNs");
        assertEquals(0.16f, s.fadeDurationSec(), 1e-6f);
    }

    @Test
    void handlerReturningNullEntersStopFadeWhenTransitionConfigured() {
        BakedAnimation harvest = playOnceAnim("harvest", 1f);
        // Action-style controller: handler never plays, fades out over 0.15s.
        ControllerConfig config = new ControllerConfig("action", BlendMode.OVERRIDE, 0.15f,
                ControllerHandler::neverPlay);
        ControllerInstance c = new ControllerInstance(config);

        long t0 = 1_000_000_000L;
        c.playOnce(harvest, t0);
        c.tick(null, null, t0);
        assertSame(harvest, c.snapshot().current().animation(), "trigger active");

        // Trigger ends: the controller should enter stop-fade, not snap to idle.
        long postEnd = t0 + 1_500_000_000L;
        c.tick(null, null, postEnd);
        ControllerSnapshot s = c.snapshot();
        assertTrue(s.isFadingOut(), "after trigger ends, controller is fading out");
        assertSame(harvest, s.current().animation(),
                "current still set to (now-stale) trigger animation so it can be sampled during fade");
        assertEquals(0.15f, s.fadeDurationSec(), 1e-6f);

        // After fade duration elapses, controller goes truly silent.
        c.tick(null, null, postEnd + 200_000_000L);
        assertTrue(c.snapshot().isIdle(), "post-fade: controller is silent");
        assertFalse(c.snapshot().isFadingOut());
    }

    @Test
    void handlerReturningNullSnapsWhenNoTransitionConfigured() {
        // transitionSec=0 means stop is a clean cut, no fade-out state.
        BakedAnimation harvest = playOnceAnim("harvest", 1f);
        ControllerConfig config = new ControllerConfig("instant", BlendMode.OVERRIDE, 0f,
                ControllerHandler::neverPlay);
        ControllerInstance c = new ControllerInstance(config);

        c.playOnce(harvest, 0L);
        c.tick(null, null, 0L);
        c.tick(null, null, 2_000_000_000L); // past harvest duration
        assertTrue(c.snapshot().isIdle(), "no transition = clean cut to idle");
        assertFalse(c.snapshot().isFadingOut());
    }

    @Test
    void handlerReturningAnimationCancelsStopFade() {
        BakedAnimation idle = loopAnim("idle");
        BakedAnimation harvest = playOnceAnim("harvest", 1f);
        boolean[] handlerActive = {false};
        ControllerConfig config = new ControllerConfig("hybrid", BlendMode.OVERRIDE, 0.15f,
                (state, ctx) -> handlerActive[0] ? idle : null);
        ControllerInstance c = new ControllerInstance(config);

        long t0 = 1_000_000_000L;
        c.playOnce(harvest, t0);
        c.tick(null, null, t0);
        c.tick(null, null, t0 + 1_500_000_000L); // trigger ends → stop-fade
        assertTrue(c.snapshot().isFadingOut());

        // Mid stop-fade, the handler decides to play idle. Stop-fade must cancel.
        handlerActive[0] = true;
        c.tick(null, null, t0 + 1_550_000_000L);
        assertFalse(c.snapshot().isFadingOut(), "handler producing animation cancels stop-fade");
        assertSame(idle, c.snapshot().current().animation());
    }

    @Test
    void additiveBlendComposesPerChannelType() {
        // 2 bones × FLOATS_PER_BONE; bone 0 carries the OVERRIDE base, bone 1 stays identity.
        int boneCount = 2;
        float[] poseBuf = new float[boneCount * PoseSampler.FLOATS_PER_BONE];
        PoseSampler.resetIdentity(poseBuf, boneCount);

        // Pre-populate bone 0 with OVERRIDE values: rot=(0.1, 0.2, 0.3), pos=(1,2,3), scale=(2,2,2)
        int b0 = 0;
        poseBuf[b0 + PoseSampler.OFFSET_ROT]     = 0.1f;
        poseBuf[b0 + PoseSampler.OFFSET_ROT + 1] = 0.2f;
        poseBuf[b0 + PoseSampler.OFFSET_ROT + 2] = 0.3f;
        poseBuf[b0 + PoseSampler.OFFSET_POS]     = 1f;
        poseBuf[b0 + PoseSampler.OFFSET_POS + 1] = 2f;
        poseBuf[b0 + PoseSampler.OFFSET_POS + 2] = 3f;
        poseBuf[b0 + PoseSampler.OFFSET_SCALE]     = 2f;
        poseBuf[b0 + PoseSampler.OFFSET_SCALE + 1] = 2f;
        poseBuf[b0 + PoseSampler.OFFSET_SCALE + 2] = 2f;

        // Build an additive animation that contributes (0.5,0,0) rot, (10,0,0) pos, (3,1,1) scale on bone 0.
        BakedBoneChannel rotCh = new BakedBoneChannel(0, BakedBoneChannel.TYPE_ROTATION, true,
                new float[0], new float[]{0.5f, 0f, 0f}, new byte[0], null);
        BakedBoneChannel posCh = new BakedBoneChannel(0, BakedBoneChannel.TYPE_POSITION, true,
                new float[0], new float[]{10f, 0f, 0f}, new byte[0], null);
        BakedBoneChannel scaleCh = new BakedBoneChannel(0, BakedBoneChannel.TYPE_SCALE, true,
                new float[0], new float[]{3f, 1f, 1f}, new byte[0], null);
        BakedAnimation additive = new BakedAnimation("additive", 1f, LoopMode.LOOP,
                new BakedBoneChannel[]{rotCh, posCh, scaleCh});

        com.dwinovo.chiikawa.anim.runtime.AnimationChannel ch =
                new com.dwinovo.chiikawa.anim.runtime.AnimationChannel(additive, 0L, true);
        PoseSampler.sample(ch, BlendMode.ADDITIVE, 0L, new MolangContext(), poseBuf);

        // Rotation: 0.1 + 0.5 = 0.6
        assertEquals(0.6f, poseBuf[b0 + PoseSampler.OFFSET_ROT], 1e-6f, "additive rotation = +=");
        assertEquals(0.2f, poseBuf[b0 + PoseSampler.OFFSET_ROT + 1], 1e-6f);
        // Position: 1 + 10 = 11
        assertEquals(11f, poseBuf[b0 + PoseSampler.OFFSET_POS], 1e-6f, "additive position = +=");
        // Scale: 2 * 3 = 6 (multiplicative — naive += would yield 5)
        assertEquals(6f, poseBuf[b0 + PoseSampler.OFFSET_SCALE], 1e-6f, "additive scale = *=");
        assertEquals(2f, poseBuf[b0 + PoseSampler.OFFSET_SCALE + 1], 1e-6f);

        // Bone 1 was identity → still identity (animation didn't keyframe it).
        int b1 = PoseSampler.FLOATS_PER_BONE;
        assertEquals(0f, poseBuf[b1 + PoseSampler.OFFSET_ROT]);
        assertEquals(0f, poseBuf[b1 + PoseSampler.OFFSET_POS]);
        assertEquals(1f, poseBuf[b1 + PoseSampler.OFFSET_SCALE]);
    }
}
