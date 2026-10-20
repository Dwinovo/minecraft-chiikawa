package com.dwinovo.chiikawa.entity.brain.intent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.ErrandSettingsData;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.testing.TestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FollowOwnerIntentTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos OWNER = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor FOLLOWING = TestAnchors.following(OWNER);

    private final FollowOwnerIntent intent = new FollowOwnerIntent();
    private final ErrandSettings<?> settings = ErrandSettingsData.all().get(ErrandIds.FOLLOW_OWNER);

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void scoreRisesFromHalfAtFollowStartToOneAtReach() {
        assertEquals(0.5F, intent.score(petAt(7, FOLLOWING), settings), 1.0E-6F);
        assertEquals(0.6F, intent.score(petAt(8, FOLLOWING), settings), 1.0E-6F);
        assertEquals(0.8F, intent.score(petAt(10, FOLLOWING), settings), 1.0E-6F);
        assertEquals(1.0F, intent.score(petAt(12, FOLLOWING), settings), 1.0E-6F);
    }

    @Test
    void scoreIsClampedOutsideTheFollowRange() {
        assertEquals(0.5F, intent.score(petAt(3, FOLLOWING), settings), 1.0E-6F);
        assertEquals(1.0F, intent.score(petAt(19, FOLLOWING), settings), 1.0E-6F);
    }

    @Test
    void aFarOwnerOutscoresEveryOtherErrandOfFollowingsPriority() {
        float far = intent.score(petAt(12, FOLLOWING), settings);

        ErrandSettingsData.all().forEach((errand, other) -> {
            if (!errand.equals(ErrandIds.FOLLOW_OWNER) && other.priority() == settings.priority()) {
                assertTrue(far > other.score(), errand.toString());
            }
        });
    }

    @Test
    void startsAtFollowStartAndContinuesUntilArrived() {
        assertFalse(intent.canRun(petAt(6, FOLLOWING)).ok());
        assertTrue(intent.canRun(petAt(7, FOLLOWING)).ok());

        assertTrue(intent.canContinue(petAt(3, FOLLOWING)).ok());
        assertFalse(intent.canContinue(petAt(TestAnchors.SETTINGS.follow().arrive(), FOLLOWING)).ok());
    }

    @Test
    void neverStartsWithoutAnAnchorThatFollowsTheOwner() {
        GlobalPos far = GlobalPos.of(OVERWORLD, new BlockPos(30, 64, 0));
        PetAnchor offline = new PetAnchor(far, 0.0, PetAnchor.UNLEASHED, false, true);
        IntentContext ctx = TestContext.at(far, offline).build();

        assertEquals("intent.chiikawa.fail.owner_unavailable", intent.canRun(ctx).reasonKey());
        assertFalse(intent.canContinue(ctx).ok());
    }

    private static IntentContext petAt(int blocksEast, PetAnchor anchor) {
        GlobalPos pet = GlobalPos.of(OVERWORLD, OWNER.pos().east(blocksEast));
        return TestContext.at(pet, anchor).build();
    }
}
