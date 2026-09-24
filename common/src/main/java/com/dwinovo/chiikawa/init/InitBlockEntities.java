package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.block.ShopBlockEntity;
import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class InitBlockEntities {
    public static final Supplier<BlockEntityType<LaborBoardBlockEntity>> LABOR_BOARD = Services.REGISTRY.registerBlockEntity(
        Identifier.fromNamespaceAndPath(Constants.MOD_ID, "labor_board"),
        LaborBoardBlockEntity::new,
        InitBlocks.LABOR_BOARD
    );

    public static final Supplier<BlockEntityType<ShopBlockEntity>> SHOP = Services.REGISTRY.registerBlockEntity(
        Identifier.fromNamespaceAndPath(Constants.MOD_ID, "shop"),
        ShopBlockEntity::new,
        InitBlocks.SHOP
    );

    private InitBlockEntities() {
    }

    public static void init() {
    }
}
