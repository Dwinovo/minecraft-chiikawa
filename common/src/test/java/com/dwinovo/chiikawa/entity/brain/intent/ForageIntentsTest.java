package com.dwinovo.chiikawa.entity.brain.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.AnchorDistances;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class ForageIntentsTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor WILD_ANCHOR = new PetAnchor(HOME, AnchorDistances.WILD_REACH, AnchorDistances.WILD_LEASH, false, true);
    private static final PetOwnership OWNED = new PetOwnership.Owned(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    private final PetIntent weed = PetIntents.get(PetIntents.WEED);
    private final PetIntent mushroom = PetIntents.get(PetIntents.PICK_MUSHROOM);

    @Test
    void foragingIsItsOwnCategory() {
        assertEquals(IntentCategory.FORAGE, weed.category());
        assertEquals(IntentCategory.FORAGE, mushroom.category());
    }

    @Test
    void wildFarmerPullsWeedsWithinReachAtAnyTime() {
        for (DayPhase phase : DayPhase.values()) {
            assertTrue(weed.canRun(ctx(PetOwnership.WILD, phase, 20)).ok(), phase::name);
        }
        assertEquals("intent.chiikawa.fail.out_of_reach", weed.canRun(ctx(PetOwnership.WILD, DayPhase.DAY, 24)).reasonKey());
        assertEquals("intent.chiikawa.fail.no_weed", weed.canRun(context(PetOwnership.WILD, DayPhase.DAY, PerceivedTargets.NONE)).reasonKey());
    }

    @Test
    void ownedFarmerNeverForagesOnItsOwn() {
        assertEquals("intent.chiikawa.fail.wild_only", weed.canRun(ctx(OWNED, DayPhase.DAY, 1)).reasonKey());
        assertEquals("intent.chiikawa.fail.wild_only", mushroom.canRun(ctx(OWNED, DayPhase.NIGHT, 1)).reasonKey());
    }

    @Test
    void mushroomsArePickedOnlyAtNight() {
        assertTrue(mushroom.canRun(ctx(PetOwnership.WILD, DayPhase.NIGHT, 1)).ok());
        for (DayPhase phase : new DayPhase[] {DayPhase.MORNING, DayPhase.DAY, DayPhase.EVENING}) {
            assertEquals("intent.chiikawa.fail.not_night", mushroom.canRun(ctx(PetOwnership.WILD, phase, 1)).reasonKey());
        }
    }

    /** Both a weed and a mushroom {@code blocksEast} of home. */
    private static IntentContext ctx(PetOwnership ownership, DayPhase phase, int blocksEast) {
        Optional<GlobalPos> target = Optional.of(GlobalPos.of(OVERWORLD, HOME.pos().east(blocksEast)));
        PerceivedTargets targets = new PerceivedTargets(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), target, target, Optional.empty());
        return context(ownership, phase, targets);
    }

    private static IntentContext context(PetOwnership ownership, DayPhase phase, PerceivedTargets targets) {
        return new IntentContext(HOME, phase, ownership, Personality.DEFAULT, WILD_ANCHOR, targets, false, false, false, false);
    }
}
