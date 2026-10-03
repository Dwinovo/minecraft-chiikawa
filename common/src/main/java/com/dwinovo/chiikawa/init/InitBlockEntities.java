package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.block.ShopBlockEntity;
import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class InitBlockEntities {
    public static final Supplier<BlockEntityType<LaborBoardBlockEntity>> LABOR_BOARD = Services.REGISTRY.registerBlockEntity(
        new ResourceLocation(Constants.MOD_ID, "labor_board"),
        LaborBoardBlockEntity::new,
        InitBlocks.LABOR_BOARD
    );

    public static final Supplier<BlockEntityType<ShopBlockEntity>> SHOP = Services.REGISTRY.registerBlockEntity(
        new ResourceLocation(Constants.MOD_ID, "shop"),
        ShopBlockEntity::new,
        InitBlocks.SHOP
    );

    public static final Supplier<BlockEntityType<ExamDeskBlockEntity>> EXAM_DESK = Services.REGISTRY.registerBlockEntity(
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "exam_desk"),
        ExamDeskBlockEntity::new,
        InitBlocks.EXAM_DESK
    );

    private InitBlockEntities() {
    }

    public static void init() {
    }
}
