package com.dwinovo.chiikawa.item;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class CakeReviveRitual {
    public static final int LAST_BITE = 6;
    public static final int REVIVE_DELAY_TICKS = 60;

    private CakeReviveRitual() {
    }

    public static boolean isOfferingBlock(BlockState state) {
        return state.is(Blocks.CAKE);
    }

    public static int nextBitesAfterOffering(BlockState cakeState) {
        validateCakeState(cakeState);
        return nextBitesAfterOffering(cakeState.getValue(CakeBlock.BITES));
    }

    public static boolean shouldRemoveCakeAfterOffering(BlockState cakeState) {
        validateCakeState(cakeState);
        return shouldRemoveCakeAfterOffering(cakeState.getValue(CakeBlock.BITES));
    }

    public static int nextBitesAfterOffering(int currentBites) {
        validateBites(currentBites);
        return Math.min(currentBites + 1, LAST_BITE);
    }

    public static boolean shouldRemoveCakeAfterOffering(int currentBites) {
        validateBites(currentBites);
        return currentBites >= LAST_BITE;
    }

    private static void validateCakeState(BlockState state) {
        if (!isOfferingBlock(state)) {
            throw new IllegalArgumentException("Expected cake block state");
        }
    }

    private static void validateBites(int bites) {
        if (bites < 0 || bites > LAST_BITE) {
            throw new IllegalArgumentException("Cake bites must be in [0, " + LAST_BITE + "]");
        }
    }
}
