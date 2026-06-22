package com.dwinovo.chiikawa.entity.brain.task.farmer.crop;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Standard farming behaviour for crops that follow vanilla conventions. Used as
 * the fallback whenever {@link FarmRegistry} has no special handler for a seed
 * item or crop block.
 *
 * <ul>
 *   <li><b>Seeds</b>: any {@link BlockItem} placing a {@link CropBlock} or
 *       {@link StemBlock} (covers vanilla + most modded crops with zero config),
 *       plus anything in the {@link InitTag#ENTITY_PLANT_CROPS} tag (which pulls
 *       in the {@code c:seeds} convention tag).</li>
 *   <li><b>Harvest</b>: a {@link CropBlock} at max age, stem fruit, or anything
 *       in {@link InitTag#ENTITY_HARVEST_CROPS}.</li>
 *   <li><b>Planting</b>: native {@link BlockItem#place} so the crop's own
 *       placement rules and survival checks apply.</li>
 *   <li><b>Collection</b>: loot-table drops (with the held tool, so Fortune
 *       applies) go straight into the backpack; mature crops are replanted in
 *       place by resetting to age 0 rather than destroyed.</li>
 * </ul>
 */
public class DefaultCropHandler implements CropHandler {
    public static final DefaultCropHandler INSTANCE = new DefaultCropHandler();

    @Override
    public boolean isSeed(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof CropBlock || block instanceof StemBlock) {
                return true;
            }
        }
        return stack.is(InitTag.ENTITY_PLANT_CROPS);
    }

    @Override
    public boolean canHarvest(ServerLevel level, BlockPos cropPos, BlockState cropState) {
        Block block = cropState.getBlock();
        // Any vanilla/modded crop extending CropBlock is harvestable once grown.
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(cropState);
        }
        // Stem fruit has no age property.
        if (cropState.is(Blocks.MELON) || cropState.is(Blocks.PUMPKIN)) {
            return true;
        }
        // Datapack/whitelist escape hatch for crops that aren't CropBlock subclasses.
        return cropState.is(InitTag.ENTITY_HARVEST_CROPS);
    }

    @Override
    public void harvest(ServerLevel level, AbstractPet pet, BlockPos cropPos, BlockState cropState) {
        Block block = cropState.getBlock();
        ItemStack tool = pet.getMainHandItem();
        BlockEntity blockEntity = cropState.hasBlockEntity() ? level.getBlockEntity(cropPos) : null;

        pet.dropResourcesToPetInv(cropState, level, cropPos, blockEntity, tool);

        if (block instanceof CropBlock crop && crop.isMaxAge(cropState)) {
            // Replant in place: reset to age 0 rather than destroying the crop.
            level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, cropPos, Block.getId(cropState));
            level.setBlock(cropPos, crop.defaultBlockState(), Block.UPDATE_ALL);
            level.gameEvent(pet, GameEvent.BLOCK_CHANGE, cropPos);
        } else {
            // Drops were already collected above, so don't drop again on removal.
            level.destroyBlock(cropPos, false, pet);
        }
    }

    @Override
    public boolean canPlantOn(ServerLevel level, BlockPos basePos, BlockState baseState, ItemStack seed) {
        return baseState.is(Blocks.FARMLAND);
    }

    @Override
    public boolean plant(ServerLevel level, AbstractPet pet, BlockPos farmlandPos, BlockState farmlandState, ItemStack seed) {
        if (!(seed.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        // Aim at the top face of the farmland; BlockPlaceContext resolves the
        // placement to the empty block above it.
        Vec3 hitVec = new Vec3(farmlandPos.getX() + 0.5D, farmlandPos.getY() + 1.0D, farmlandPos.getZ() + 0.5D);
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, farmlandPos, false);
        // The (Level, Player, ...) constructor is protected in vanilla, so reach it
        // through an anonymous subclass — lets us place with no player behind it.
        BlockPlaceContext context = new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, seed, hit) {};
        return blockItem.place(context).consumesAction();
    }
}
