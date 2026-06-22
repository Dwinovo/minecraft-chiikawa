package com.dwinovo.chiikawa.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.pathfinder.Path;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.task.farmer.crop.FarmRegistry;

// Utility helpers. Farming methods dispatch through FarmRegistry to the matching
// CropHandler (DefaultCropHandler for standard crops); the actual logic lives there.
public class Utils {

    /**
     * Returns true if the crop at the position can be harvested.
     * @param world the server world
     * @param pos the block position
     * @return whether the crop is harvestable
     */
    public static boolean canHarvesr(ServerLevel world, BlockPos pos) {
        return FarmRegistry.forCrop(world.getBlockState(pos).getBlock())
            .canHarvest(world, pos, world.getBlockState(pos));
    }
    /**
     * Finds a seed stack in the pet backpack.
     * @param pet the pet
     * @return the first seed stack or empty
     */
    public static ItemStack getSeed(AbstractPet pet) {
        for(int i = 0; i < pet.getBackpack().getContainerSize(); i++) {
            ItemStack item = pet.getBackpack().getItem(i);
            if(isSeed(item)) {
                return item;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Generic seed test, dispatched to the matching {@link FarmRegistry} handler.
     * @param stack the candidate stack
     * @return whether the stack should be treated as a plantable seed
     */
    public static boolean isSeed(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return FarmRegistry.forSeed(stack.getItem()).isSeed(stack);
    }

    /**
     * Plants a seed on the farmland at {@code farmlandPos}, dispatched to the
     * matching {@link FarmRegistry} handler. The seed stack is decremented on
     * success.
     * @param world the server level
     * @param pet the planting pet
     * @param farmlandPos the soil block to plant on
     * @param seed the seed stack (consumed by one on success)
     * @return whether a crop was actually placed
     */
    public static boolean plantSeed(ServerLevel world, AbstractPet pet, BlockPos farmlandPos, ItemStack seed) {
        return FarmRegistry.forSeed(seed.getItem())
            .plant(world, pet, farmlandPos, world.getBlockState(farmlandPos), seed);
    }

    /**
     * Harvests the crop at {@code cropPos}, dispatched to the matching
     * {@link FarmRegistry} handler. Caller is expected to have checked
     * {@link #canHarvesr}.
     * @param world the server level
     * @param pet the harvesting pet
     * @param cropPos the crop block position
     */
    public static void harvestCrop(ServerLevel world, AbstractPet pet, BlockPos cropPos) {
        FarmRegistry.forCrop(world.getBlockState(cropPos).getBlock())
            .harvest(world, pet, cropPos, world.getBlockState(cropPos));
    }
    /**
     * Finds an arrow stack in the pet backpack.
     * @param pet the pet
     * @return the first arrow stack or empty
     */
    public static ItemStack getArrow(AbstractPet pet) {
        for (int i = 0; i < pet.getBackpack().getContainerSize(); i++) {
            ItemStack item = pet.getBackpack().getItem(i);
            if (ProjectileWeaponItem.ARROW_ONLY.test(item)) {
                return item;
            }
        }
        return ItemStack.EMPTY;
    }
    /**
     * Checks whether the entity can path to the position.
     * @param entity the pet entity
     * @param pos the target position
     * @return whether the position is reachable
     */
    public static boolean canReach(AbstractPet entity, BlockPos pos) {
        PathNavigation navigation = entity.getNavigation();
        Path path = navigation.createPath(pos, 0);
        return path != null && path.canReach();
    }
    /**
     * Whether the pet's current seed can be planted on the block at {@code basePos}:
     * the space above must be air and the soil must be valid for that seed (per the
     * seed's {@link FarmRegistry} handler — farmland for vanilla crops, soul sand for
     * nether wart, etc.). Returns false if the pet has no seed.
     * @param world the server world
     * @param pet the pet (its first seed decides the required soil)
     * @param basePos the candidate soil block position
     * @return whether the pet's seed can be planted here
     */
    public static boolean isPlantableBase(ServerLevel world, AbstractPet pet, BlockPos basePos) {
        ItemStack seed = getSeed(pet);
        if (seed.isEmpty() || !world.getBlockState(basePos.above()).isAir()) {
            return false;
        }
        return FarmRegistry.forSeed(seed.getItem())
            .canPlantOn(world, basePos, world.getBlockState(basePos), seed);
    }
}
