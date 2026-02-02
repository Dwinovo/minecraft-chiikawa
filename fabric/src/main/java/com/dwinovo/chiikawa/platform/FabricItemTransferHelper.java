package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.platform.services.IItemTransferHelper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import com.dwinovo.chiikawa.entity.AbstractPet;

public class FabricItemTransferHelper implements IItemTransferHelper {
    @Override
    public boolean hasBlockStorage(ServerLevel level, BlockPos pos) {
        return findBlockStorage(level, pos) != null;
    }

    @Override
    public int insertIntoBlock(ServerLevel level, BlockPos pos, ItemStack stack, boolean simulate) {
        Storage<ItemVariant> storage = findBlockStorage(level, pos);
        return insertIntoStorage(storage, stack, simulate);
    }

    @Override
    public boolean hasEntityStorage(Entity entity) {
        return findEntityStorage(entity) != null;
    }

    @Override
    public int insertIntoEntity(Entity entity, ItemStack stack, boolean simulate) {
        Storage<ItemVariant> storage = findEntityStorage(entity);
        return insertIntoStorage(storage, stack, simulate);
    }

    private static Storage<ItemVariant> findBlockStorage(ServerLevel level, BlockPos pos) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, null);
        if (storage != null) {
            return storage;
        }
        for (Direction direction : Direction.values()) {
            storage = ItemStorage.SIDED.find(level, pos, direction);
            if (storage != null) {
                return storage;
            }
        }
        return null;
    }

    private static Storage<ItemVariant> findEntityStorage(Entity entity) {
        if (entity instanceof AbstractPet pet) {
            return InventoryStorage.of(pet.getBackpack(), null);
        }
        if (entity instanceof Container container) {
            return InventoryStorage.of(container, null);
        }
        return null;
    }

    private static int insertIntoStorage(Storage<ItemVariant> storage, ItemStack stack, boolean simulate) {
        if (storage == null || stack.isEmpty()) {
            return 0;
        }
        long amount = stack.getCount();
        if (amount <= 0) {
            return 0;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = storage.insert(ItemVariant.of(stack), amount, transaction);
            if (!simulate) {
                transaction.commit();
            }
            return (int) Math.min(inserted, Integer.MAX_VALUE);
        }
    }
}
