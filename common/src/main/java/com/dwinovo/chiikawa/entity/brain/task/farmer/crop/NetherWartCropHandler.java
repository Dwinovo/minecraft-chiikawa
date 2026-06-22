package com.dwinovo.chiikawa.entity.brain.task.farmer.crop;

import com.dwinovo.chiikawa.entity.AbstractPet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Special-crop handler for nether wart — the first non-{@code CropBlock} crop, and
 * a worked example of extending {@link DefaultCropHandler}. Nether wart grows on
 * soul sand (not farmland) and uses its own {@code AGE} property instead of
 * {@code CropBlock}'s, so seed detection, soil and harvest/replant are overridden;
 * planting reuses the default native placement (the nether wart item is a normal
 * {@code BlockItem} that places onto soul sand).
 */
public class NetherWartCropHandler extends DefaultCropHandler {

    @Override
    public boolean isSeed(ItemStack stack) {
        return stack.getItem() == Items.NETHER_WART;
    }

    @Override
    public boolean canPlantOn(ServerLevel level, BlockPos basePos, BlockState baseState, ItemStack seed) {
        return baseState.is(Blocks.SOUL_SAND);
    }

    @Override
    public boolean canHarvest(ServerLevel level, BlockPos cropPos, BlockState cropState) {
        return cropState.getBlock() instanceof NetherWartBlock
            && cropState.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
    }

    @Override
    public void harvest(ServerLevel level, AbstractPet pet, BlockPos cropPos, BlockState cropState) {
        BlockEntity blockEntity = cropState.hasBlockEntity() ? level.getBlockEntity(cropPos) : null;
        pet.dropResourcesToPetInv(cropState, level, cropPos, blockEntity, pet.getMainHandItem());
        // Replant in place: nether wart isn't a CropBlock, so reset to its age-0 state.
        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, cropPos, Block.getId(cropState));
        level.setBlock(cropPos, Blocks.NETHER_WART.defaultBlockState(), Block.UPDATE_ALL);
        level.gameEvent(pet, GameEvent.BLOCK_CHANGE, cropPos);
    }
}
