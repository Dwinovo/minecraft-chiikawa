package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.platform.services.IItemTransferHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;

public class NeoForgeItemTransferHelper implements IItemTransferHelper {
    @Override
    public boolean hasBlockStorage(ServerLevel level, BlockPos pos) {
        return findBlockHandler(level, pos) != null;
    }

    @Override
    public int insertIntoBlock(ServerLevel level, BlockPos pos, ItemStack stack, boolean simulate) {
        ResourceHandler<ItemResource> handler = findBlockHandler(level, pos);
        if (handler == null || stack.isEmpty()) {
            return 0;
        }
        ItemStack remaining = insertIntoHandler(handler, stack, simulate);
        return stack.getCount() - remaining.getCount();
    }

    private static ResourceHandler<ItemResource> findBlockHandler(ServerLevel level, BlockPos pos) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        if (handler != null) {
            return handler;
        }
        for (Direction direction : Direction.values()) {
            handler = level.getCapability(Capabilities.Item.BLOCK, pos, direction);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }

    private static ItemStack insertIntoHandler(ResourceHandler<ItemResource> handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < handler.size() && !remaining.isEmpty(); slot++) {
            remaining = ItemUtil.insertItemReturnRemaining(handler, slot, remaining, simulate, null);
        }
        return remaining;
    }
}
