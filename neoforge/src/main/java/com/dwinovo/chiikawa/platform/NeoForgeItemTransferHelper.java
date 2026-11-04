package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.platform.services.IItemTransferHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public class NeoForgeItemTransferHelper implements IItemTransferHelper {
    @Override
    public boolean hasBlockStorage(ServerLevel level, BlockPos pos) {
        return findBlockHandler(level, pos) != null;
    }

    @Override
    public int insertIntoBlock(ServerLevel level, BlockPos pos, ItemStack stack, boolean simulate) {
        IItemHandler handler = findBlockHandler(level, pos);
        if (handler == null || stack.isEmpty()) {
            return 0;
        }
        ItemStack remaining = insertIntoHandler(handler, stack, simulate);
        return stack.getCount() - remaining.getCount();
    }

    private static IItemHandler findBlockHandler(ServerLevel level, BlockPos pos) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler != null) {
            return handler;
        }
        for (Direction direction : Direction.values()) {
            handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }

    private static ItemStack insertIntoHandler(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }
}
