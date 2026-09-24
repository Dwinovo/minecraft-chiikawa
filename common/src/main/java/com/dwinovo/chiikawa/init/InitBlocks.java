package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.block.LaborBoardBlock;
import com.dwinovo.chiikawa.block.ShopBlock;
import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public final class InitBlocks {
    private static final Identifier LABOR_BOARD_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "labor_board");
    private static final Identifier SHOP_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "shop");

    public static final Supplier<LaborBoardBlock> LABOR_BOARD = Services.REGISTRY.<LaborBoardBlock>register(
        BuiltInRegistries.BLOCK,
        LABOR_BOARD_ID,
        () -> new LaborBoardBlock(BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, LABOR_BOARD_ID))
            .mapColor(MapColor.WOOD)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .ignitedByLava())
    );

    public static final Supplier<ShopBlock> SHOP = Services.REGISTRY.<ShopBlock>register(
        BuiltInRegistries.BLOCK,
        SHOP_ID,
        () -> new ShopBlock(BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, SHOP_ID))
            .mapColor(MapColor.WOOD)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .ignitedByLava())
    );

    /** Where labor boards stand, so pets can find the nearest one. */
    public static final ResourceKey<PoiType> LABOR_BOARD_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, LABOR_BOARD_ID);
    /** Where shops stand, likewise. */
    public static final ResourceKey<PoiType> SHOP_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, SHOP_ID);

    static {
        Services.REGISTRY.registerPoi(LABOR_BOARD_ID, LABOR_BOARD);
        Services.REGISTRY.registerPoi(SHOP_ID, SHOP);
    }

    private InitBlocks() {
    }

    public static void init() {
    }
}
