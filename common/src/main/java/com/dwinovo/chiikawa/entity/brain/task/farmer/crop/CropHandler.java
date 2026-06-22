package com.dwinovo.chiikawa.entity.brain.task.farmer.crop;

import com.dwinovo.chiikawa.entity.AbstractPet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pluggable per-crop farming strategy, mirroring TouhouLittleMaid's
 * {@code IFarmTask} / {@code ISpecialCropHandler} split. {@link DefaultCropHandler}
 * implements the standard {@code CropBlock}/stem behaviour used for vanilla and
 * the vast majority of modded crops; register a handler for a specific seed item
 * and/or crop block via {@link FarmRegistry} to customise an oddball crop (one
 * with a non-standard maturity property, custom drops, multi-block layout, etc.).
 *
 * <p>The simplest way to write one is to {@code extends DefaultCropHandler} and
 * override only the methods that differ.
 */
public interface CropHandler {
    /**
     * @param stack a candidate backpack stack
     * @return whether this stack should be treated as a plantable seed
     */
    boolean isSeed(ItemStack stack);

    /**
     * @param level the server level
     * @param cropPos the crop block position
     * @param cropState the crop block state
     * @return whether the crop here is ready to be harvested
     */
    boolean canHarvest(ServerLevel level, BlockPos cropPos, BlockState cropState);

    /**
     * Harvests the crop: collect drops (typically into the pet's backpack) and
     * either replant in place or remove the block. Called only after
     * {@link #canHarvest} returned true.
     *
     * @param level the server level
     * @param pet the harvesting pet
     * @param cropPos the crop block position
     * @param cropState the crop block state
     */
    void harvest(ServerLevel level, AbstractPet pet, BlockPos cropPos, BlockState cropState);

    /**
     * Whether this crop's seed can be planted on the given soil block. The space
     * directly above is already known to be air; this only judges the soil itself
     * (e.g. farmland for vanilla crops, soul sand for nether wart).
     *
     * @param level the server level
     * @param basePos the soil block position
     * @param baseState the soil block state
     * @param seed the seed stack to be planted
     * @return whether this soil can host the crop
     */
    boolean canPlantOn(ServerLevel level, BlockPos basePos, BlockState baseState, ItemStack seed);

    /**
     * Plants a seed on the given soil block.
     *
     * @param level the server level
     * @param pet the planting pet
     * @param farmlandPos the soil block to plant on
     * @param farmlandState the soil block state
     * @param seed the seed stack (consumed by one on success)
     * @return whether a crop was actually placed
     */
    boolean plant(ServerLevel level, AbstractPet pet, BlockPos farmlandPos, BlockState farmlandState, ItemStack seed);
}
