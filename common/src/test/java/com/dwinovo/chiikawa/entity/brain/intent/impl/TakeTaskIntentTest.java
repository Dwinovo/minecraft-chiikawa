package com.dwinovo.chiikawa.entity.brain.intent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.AnchorDistances;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.testing.TestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class TakeTaskIntentTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(new ResourceLocation("dimension")),
        new ResourceLocation("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor FREE = new PetAnchor(HOME, AnchorDistances.FREE_REACH, AnchorDistances.FREE_LEASH, false, true);

    private final TakeTaskIntent intent = new TakeTaskIntent();

    @Test
    void takesASlipFromABoardWithinReach() {
        assertEquals(IntentCategory.TAKE_TASK, intent.category());
        assertTrue(intent.canRun(boardAt(10).build()).ok());
        assertEquals("intent.chiikawa.fail.out_of_reach", intent.canRun(boardAt(15).build()).reasonKey());
    }

    @Test
    void keepsGoingWhileTheBoardIsWithinTheLeash() {
        assertTrue(intent.canContinue(boardAt(15).build()).ok());
        assertEquals("intent.chiikawa.fail.out_of_leash", intent.canContinue(boardAt(25).build()).reasonKey());
    }

    @Test
    void carriesOneSlipAtATime() {
        PetTask slip = new PetTask(new ResourceLocation("chiikawa", "weeding"),
            new ResourceLocation("chiikawa", "farmer"), PetWorkCounters.WEED, PetTask.NO_ICON, 8,
            ResourceKey.create(Registries.LOOT_TABLE, new ResourceLocation("chiikawa", "pet_task/weeding")), 0);

        assertEquals("intent.chiikawa.fail.has_slip", intent.canRun(boardAt(1).task(slip).build()).reasonKey());
        // Having taken it ends the walk to the board.
        assertEquals("intent.chiikawa.fail.has_slip", intent.canContinue(boardAt(1).task(slip).build()).reasonKey());
    }

    @Test
    void restsAfterMissingASlipAndNeedsABoardOfferingOne() {
        assertEquals("intent.chiikawa.fail.board_resting", intent.canRun(boardAt(1).takeTaskCoolingDown().build()).reasonKey());
        assertEquals("intent.chiikawa.fail.no_slip", intent.canRun(TestContext.at(HOME, FREE).build()).reasonKey());
    }

    private static TestContext boardAt(int blocksEast) {
        return TestContext.at(HOME, FREE).offeringBoard(GlobalPos.of(OVERWORLD, HOME.pos().east(blocksEast)));
    }
}
