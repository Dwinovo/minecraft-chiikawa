package com.dwinovo.chiikawa.platform.services;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public interface IItemTransferHelper {
    boolean hasBlockStorage(ServerLevel level, BlockPos pos);

    /**
     * @return inserted item count
     */
    int insertIntoBlock(ServerLevel level, BlockPos pos, ItemStack stack, boolean simulate);
}
