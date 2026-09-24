package com.dwinovo.chiikawa.entity.brain.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.AnchorDistances;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.testing.TestContext;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class ForageIntentsTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("dimension")),
        Identifier.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor WILD_ANCHOR = new PetAnchor(HOME, AnchorDistances.WILD_REACH, AnchorDistances.WILD_LEASH, false, true);

    private final PetIntent weed = PetIntents.get(PetIntents.WEED);
    private final PetIntent mushroom = PetIntents.get(PetIntents.PICK_MUSHROOM);

    @Test
    void foragingIsItsOwnCategoryAndReportsItsWork() {
        assertEquals(IntentCategory.FORAGE, weed.category());
        assertEquals(IntentCategory.FORAGE, mushroom.category());
        assertEquals(Optional.of(PetWorkCounters.WEED), weed.workCounter());
        assertEquals(Optional.of(PetWorkCounters.PICK_MUSHROOM), mushroom.workCounter());
    }

    @Test
    void wildFarmerPullsWeedsWithinReachAtAnyTime() {
        for (DayPhase phase : DayPhase.values()) {
            assertTrue(weed.canRun(ctx(PetOwnership.WILD, phase, 20).build()).ok(), phase::name);
        }
        assertEquals("intent.chiikawa.fail.out_of_reach", weed.canRun(ctx(PetOwnership.WILD, DayPhase.DAY, 24).build()).reasonKey());
        assertEquals("intent.chiikawa.fail.no_weed",
            weed.canRun(TestContext.at(HOME, WILD_ANCHOR).ownership(PetOwnership.WILD).build()).reasonKey());
    }

    @Test
    void ownedFarmerForagesOnlyWithAMatchingSlip() {
        assertEquals("intent.chiikawa.fail.needs_slip", weed.canRun(ctx(TestContext.OWNED, DayPhase.DAY, 1).build()).reasonKey());
        assertEquals("intent.chiikawa.fail.needs_slip", mushroom.canRun(ctx(TestContext.OWNED, DayPhase.NIGHT, 1).build()).reasonKey());

        assertTrue(weed.canRun(ctx(TestContext.OWNED, DayPhase.DAY, 1).task(slip(PetWorkCounters.WEED)).build()).ok());
        assertTrue(mushroom.canRun(ctx(TestContext.OWNED, DayPhase.NIGHT, 1).task(slip(PetWorkCounters.PICK_MUSHROOM)).build()).ok());
        // A weeding slip does not send it after mushrooms.
        assertEquals("intent.chiikawa.fail.needs_slip",
            mushroom.canRun(ctx(TestContext.OWNED, DayPhase.NIGHT, 1).task(slip(PetWorkCounters.WEED)).build()).reasonKey());
    }

    @Test
    void mushroomsArePickedOnlyAtNight() {
        assertTrue(mushroom.canRun(ctx(PetOwnership.WILD, DayPhase.NIGHT, 1).build()).ok());
        for (DayPhase phase : new DayPhase[] {DayPhase.MORNING, DayPhase.DAY, DayPhase.EVENING}) {
            assertEquals("intent.chiikawa.fail.not_night", mushroom.canRun(ctx(PetOwnership.WILD, phase, 1).build()).reasonKey());
        }
    }

    /** Both a weed and a mushroom {@code blocksEast} of home. */
    private static TestContext ctx(PetOwnership ownership, DayPhase phase, int blocksEast) {
        Optional<GlobalPos> target = Optional.of(GlobalPos.of(OVERWORLD, HOME.pos().east(blocksEast)));
        PerceivedTargets targets = new PerceivedTargets(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), target, target, Optional.empty());
        return TestContext.at(HOME, WILD_ANCHOR).ownership(ownership).phase(phase).targets(targets);
    }

    private static PetTask slip(Identifier counter) {
        return new PetTask(Identifier.fromNamespaceAndPath("chiikawa", "test"), Identifier.fromNamespaceAndPath("chiikawa", "farmer"),
            counter, PetTask.NO_ICON, 10, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("chiikawa", "pet_task/test")), 0);
    }
}
