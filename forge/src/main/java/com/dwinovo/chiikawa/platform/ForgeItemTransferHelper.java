package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.platform.services.IItemTransferHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

public class ForgeItemTransferHelper implements IItemTransferHelper {
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

    @Override
    public boolean hasEntityStorage(Entity entity) {
        return entity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
    }

    @Override
    public int insertIntoEntity(Entity entity, ItemStack stack, boolean simulate) {
        IItemHandler handler = entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (handler == null || stack.isEmpty()) {
            return 0;
        }
        ItemStack remaining = insertIntoHandler(handler, stack, simulate);
        return stack.getCount() - remaining.getCount();
    }

    private static IItemHandler findBlockHandler(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }
        
        // Try to get capability from any side
        IItemHandler handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        if (handler != null) {
            return handler;
        }
        
        for (Direction direction : Direction.values()) {
            handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction).orElse(null);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }

    private static ItemStack insertIntoHandler(IItemHandler handler, ItemStack stack, boolean simulate) {
        return net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(handler, stack, simulate);
    }
}
