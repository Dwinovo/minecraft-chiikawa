package com.dwinovo.chiikawa.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/**
 * Where the cases of each batch were run, so that a batch can be taken away when it is over. The game's test server
 * lays the yards of one batch after the other in one row and leaves them standing, with their pets walking about in
 * them, so a case would find a counter or a pet of an earlier batch five blocks from its own yard; and the batches run
 * in an order nobody chose, so which case that was changed from run to run. A case is to find the yards round it as
 * empty as the game once left them: its own batch's and nothing else.
 */
final class BatchYards {
    private static final Map<String, List<BoundingBox>> BY_BATCH = new HashMap<>();

    private BatchYards() {
    }

    /** Notes the yard a case of this batch is run in. */
    static synchronized void note(String batch, GameTestHelper helper) {
        AABB yard = helper.getBounds();
        BY_BATCH.computeIfAbsent(batch, name -> new ArrayList<>()).add(new BoundingBox(
            (int) yard.minX, (int) yard.minY, (int) yard.minZ, (int) yard.maxX - 1, (int) yard.maxY - 1, (int) yard.maxZ - 1));
    }

    /** Clears the yards of this batch's cases, the blocks and what lives in them, as the game clears a yard to place one. */
    static synchronized void clear(String batch, ServerLevel level) {
        List<BoundingBox> yards = BY_BATCH.remove(batch);
        if (yards != null) {
            yards.forEach(yard -> StructureUtils.clearSpaceForStructure(yard, level));
        }
    }
}
