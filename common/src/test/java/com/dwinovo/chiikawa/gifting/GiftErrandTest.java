package com.dwinovo.chiikawa.gifting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.AnchorDistances;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

/** A present is only walked to an owner within the area the pet acts in. */
class GiftErrandTest {
    private static final ResourceKey<Level> OVERWORLD = dimension("overworld");
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, BlockPos.ZERO);
    private static final PetAnchor FREE = new PetAnchor(HOME, AnchorDistances.FREE_REACH, AnchorDistances.FREE_LEASH, false, true);

    private static ResourceKey<Level> dimension(String path) {
        return ResourceKey.create(ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
            ResourceLocation.withDefaultNamespace(path));
    }

    private static GlobalPos owner(int blocksEast) {
        return GlobalPos.of(OVERWORLD, new BlockPos(blocksEast, 0, 0));
    }

    @Test
    void aFreePetSetsOffForAnOwnerWithinItsReachOnly() {
        assertTrue(GiftErrand.ownerAtHome(FREE, owner(10), true));
        assertFalse(GiftErrand.ownerAtHome(FREE, owner(15), true), "past its reach");
        assertFalse(GiftErrand.ownerAtHome(FREE, owner(40), true));
    }

    @Test
    void aFreePetOnItsWayGoesOnToAnOwnerWithinTheLeash() {
        assertTrue(GiftErrand.ownerAtHome(FREE, owner(15), false), "a little further is no reason to turn back");
        assertFalse(GiftErrand.ownerAtHome(FREE, owner(25), false), "an owner who has left");
    }

    @Test
    void anOwnerInAnotherDimensionIsNeverAtHome() {
        GlobalPos nether = GlobalPos.of(dimension("the_nether"), BlockPos.ZERO);

        assertFalse(GiftErrand.ownerAtHome(FREE, nether, true));
        assertFalse(GiftErrand.ownerAtHome(FREE, nether, false));
    }

    @Test
    void aFollowingPetHasItsOwnerForTheMiddleOfItsAreaAndSoTheyAreAlwaysAtHome() {
        GlobalPos far = owner(500);
        PetAnchor following = new PetAnchor(far, AnchorDistances.FOLLOW_REACH, AnchorDistances.FOLLOW_LEASH, true, true);

        assertTrue(GiftErrand.ownerAtHome(following, far, true));
        assertTrue(GiftErrand.ownerAtHome(following, far, false));
    }
}
