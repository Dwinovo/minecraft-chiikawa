package com.dwinovo.chiikawa.entity.brain.task.farmer.crop;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Registry of per-crop {@link CropHandler}s, mirroring TouhouLittleMaid's
 * {@code SpecialCropManager}. Seed items and crop blocks that have no special
 * handler fall back to {@link DefaultCropHandler#INSTANCE}, so vanilla and
 * standard modded crops work without any registration.
 *
 * <p>Register oddball crops from {@code CommonClass.init()} (or addon code), e.g.:
 * <pre>{@code
 * FarmRegistry.register(Items.NETHER_WART, Blocks.NETHER_WART, new NetherWartCropHandler());
 * }</pre>
 */
public final class FarmRegistry {
    private static final Map<Item, CropHandler> SEED_HANDLERS = new HashMap<>();
    private static final Map<Block, CropHandler> CROP_HANDLERS = new HashMap<>();

    private FarmRegistry() {
    }

    /**
     * Registers built-in special-crop handlers. Called once during common init.
     * Standard {@code CropBlock}/stem crops need no entry — they fall back to
     * {@link DefaultCropHandler}; only crops that break those conventions register here.
     */
    public static void init() {
        register(Items.NETHER_WART, Blocks.NETHER_WART, new NetherWartCropHandler());
    }

    /**
     * Registers a handler for both a seed item and its crop block.
     * @param seed the seed item
     * @param crop the grown crop block
     * @param handler the handler
     */
    public static void register(Item seed, Block crop, CropHandler handler) {
        SEED_HANDLERS.put(seed, handler);
        CROP_HANDLERS.put(crop, handler);
    }

    /**
     * Registers a handler for a seed item only (e.g. a crop the pet plants but
     * doesn't auto-harvest).
     * @param seed the seed item
     * @param handler the handler
     */
    public static void registerSeed(Item seed, CropHandler handler) {
        SEED_HANDLERS.put(seed, handler);
    }

    /**
     * Registers a handler for a crop block only (e.g. a crop the pet harvests but
     * doesn't plant).
     * @param crop the grown crop block
     * @param handler the handler
     */
    public static void registerCrop(Block crop, CropHandler handler) {
        CROP_HANDLERS.put(crop, handler);
    }

    /**
     * @param seed a seed item
     * @return the registered handler for this seed, or the default handler
     */
    public static CropHandler forSeed(Item seed) {
        return SEED_HANDLERS.getOrDefault(seed, DefaultCropHandler.INSTANCE);
    }

    /**
     * @param crop a crop block
     * @return the registered handler for this crop, or the default handler
     */
    public static CropHandler forCrop(Block crop) {
        return CROP_HANDLERS.getOrDefault(crop, DefaultCropHandler.INSTANCE);
    }
}
