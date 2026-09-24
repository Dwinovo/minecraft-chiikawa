package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.init.InitBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Block states and models of the mod's blocks. The labor board and the shop are drawn from
 * their Bedrock models by their block entities' renderers, so their block models are only
 * there for their break particles, and their item models come with the other props' from
 * {@link PropItemModelProvider}. Shared by both loaders' data generators: the game has every
 * block state of a mod written by the one model provider that writes all of the mod's
 * models, so each loader's calls this from its own.
 */
public final class ModBlockModelProvider {
    private ModBlockModelProvider() {
    }

    public static void generate(BlockModelGenerators blockModels) {
        drawnByItsRenderer(blockModels, InitBlocks.LABOR_BOARD.get(), Blocks.SPRUCE_PLANKS);
        drawnByItsRenderer(blockModels, InitBlocks.SHOP.get(), Blocks.BIRCH_PLANKS);
    }

    /**
     * A block drawn by its block entity's renderer, as vanilla's chest is: one block model
     * for every state, with nothing for it to draw, only a texture for the bits that fly off
     * the block when it breaks. Which way the block faces is the renderer's business.
     */
    private static void drawnByItsRenderer(BlockModelGenerators blockModels, Block block, Block like) {
        blockModels.createParticleOnlyBlock(block, like);
    }
}
