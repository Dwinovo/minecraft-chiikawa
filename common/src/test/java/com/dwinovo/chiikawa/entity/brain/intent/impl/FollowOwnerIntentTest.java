package com.dwinovo.chiikawa.entity.brain.intent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.AnchorDistances;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.intent.DayPhase;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.PerceivedTargets;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class FollowOwnerIntentTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos OWNER = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor FOLLOWING = new PetAnchor(OWNER, AnchorDistances.FOLLOW_REACH,
        AnchorDistances.FOLLOW_LEASH, true, true);

    private final FollowOwnerIntent intent = new FollowOwnerIntent();

    @Test
    void scoreRisesFromHalfAtFollowStartToOneAtReach() {
        assertEquals(0.5F, intent.score(petAt(7, FOLLOWING)), 1.0E-6F);
        assertEquals(0.6F, intent.score(petAt(8, FOLLOWING)), 1.0E-6F);
        assertEquals(0.8F, intent.score(petAt(10, FOLLOWING)), 1.0E-6F);
        assertEquals(1.0F, intent.score(petAt(12, FOLLOWING)), 1.0E-6F);
    }

    @Test
    void scoreIsClampedOutsideTheFollowRange() {
        assertEquals(0.5F, intent.score(petAt(3, FOLLOWING)), 1.0E-6F);
        assertEquals(1.0F, intent.score(petAt(19, FOLLOWING)), 1.0E-6F);
    }

    @Test
    void closeWalkingOwnerOutscoresNearbyWorkButFarOwnerOutscoresEverything() {
        // Picking up an item (0.4) never beats following; at the reach edge following
        // beats even a fight (0.8).
        assertTrue(intent.score(petAt(7, FOLLOWING)) > 0.4F);
        assertTrue(intent.score(petAt(12, FOLLOWING)) > 0.8F);
    }

    @Test
    void startsAtFollowStartAndContinuesUntilArrived() {
        assertFalse(intent.canRun(petAt(6, FOLLOWING)).ok());
        assertTrue(intent.canRun(petAt(7, FOLLOWING)).ok());

        assertTrue(intent.canContinue(petAt(3, FOLLOWING)).ok());
        assertFalse(intent.canContinue(petAt(AnchorDistances.FOLLOW_ARRIVE, FOLLOWING)).ok());
    }

    @Test
    void neverStartsWithoutAnAnchorThatFollowsTheOwner() {
        GlobalPos far = GlobalPos.of(OVERWORLD, new BlockPos(30, 64, 0));
        PetAnchor offline = new PetAnchor(far, 0.0, PetAnchor.UNLEASHED, false, true);
        IntentContext ctx = new IntentContext(far, DayPhase.DAY, Personality.DEFAULT, offline, PerceivedTargets.NONE,
            false, false, false, false);

        assertEquals("intent.chiikawa.fail.owner_unavailable", intent.canRun(ctx).reasonKey());
        assertFalse(intent.canContinue(ctx).ok());
    }

    private static IntentContext petAt(int blocksEast, PetAnchor anchor) {
        GlobalPos pet = GlobalPos.of(OVERWORLD, OWNER.pos().east(blocksEast));
        return new IntentContext(pet, DayPhase.DAY, Personality.DEFAULT, anchor, PerceivedTargets.NONE, false, false, false, false);
    }
}
