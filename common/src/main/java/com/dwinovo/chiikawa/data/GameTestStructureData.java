package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import java.util.Map;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The ground the in-game cases are run on. A case gets a bare floor and builds whatever it
 * needs on top of it in code, so a crop, a board or a wall is written where the case can be
 * read rather than hidden in a structure file nobody opens.
 *
 * <p>Sizes, not shapes: one floor big enough to walk about on and one small enough that a
 * pet cannot wander out of sight of what it is meant to be doing.
 */
public final class GameTestStructureData {
    public static final ResourceLocation FLOOR_16 = id("floor16");
    public static final ResourceLocation FLOOR_8 = id("floor8");

    /** Stone rather than grass: nothing grows on it, so nothing a case did not ask for happens. */
    public static final BlockState GROUND = Blocks.SMOOTH_STONE.defaultBlockState();

    private GameTestStructureData() {
    }

    /**
     * @return the floors to write, by id. The height is room for a pet to stand, jump and be
     *         looked at, and for a case to stack a wall or a tree on the floor.
     */
    public static Map<ResourceLocation, Vec3i> all() {
        return Map.of(
            FLOOR_16, new Vec3i(16, 8, 16),
            FLOOR_8, new Vec3i(8, 8, 8));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
