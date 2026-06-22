package com.dwinovo.chiikawa.utils;

import net.minecraft.core.BlockPos;

// Spiral search helpers.
public final class BlockSearch {
    /**
     * Visitor invoked per position during a spiral traversal.
     */
    @FunctionalInterface
    public interface BlockVisitor {
        /**
         * Visits a position.
         * @param pos the position (nearest-first spiral order)
         * @return true to stop the traversal early
         */
        boolean visit(BlockPos pos);
    }

    private BlockSearch() {
    }

    /**
     * Visits positions around {@code center} in outward spiral order (nearest
     * first), stopping as soon as the visitor returns true. Unlike a predicate
     * search this lets one pass collect several different targets at once.
     * @param center the search center
     * @param maxRadius max horizontal radius
     * @param verticalRange vertical range to scan
     * @param visitor per-position callback; returns true to stop
     */
    public static void spiralVisit(BlockPos center, int maxRadius, int verticalRange, BlockVisitor visitor) {
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int quadrant = 0; quadrant < 4; quadrant++) {
                for (int i = -radius; i <= radius; i++) {
                    for (int y = -verticalRange; y <= verticalRange; y++) {
                        if (visitor.visit(calculateSpiralPos(center, radius, quadrant, i, y))) {
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Computes a spiral position by quadrant.
     * @param center the center position
     * @param radius radius from the center
     * @param quadrant quadrant index (0-3)
     * @param i horizontal offset index
     * @param y vertical offset
     * @return computed position
     */
    private static BlockPos calculateSpiralPos(BlockPos center, int radius, int quadrant, int i, int y) {
        return switch (quadrant) {
            case 0 -> center.offset(radius, y, i);
            case 1 -> center.offset(-radius, y, i);
            case 2 -> center.offset(i, y, radius);
            case 3 -> center.offset(i, y, -radius);
            default -> center;
        };
    }
}
