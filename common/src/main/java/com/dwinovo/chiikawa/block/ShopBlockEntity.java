package com.dwinovo.chiikawa.block;

import com.dwinovo.chiikawa.data.ShopCatalogData;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.shop.ShopCatalog;
import com.dwinovo.chiikawa.shop.ShopCatalogs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Which price list a shop quotes from. That is all a shop is, for now: the prices live in
 * a data pack and the stock is whatever the world produces, so two shops differ by the
 * list they name and nothing else.
 */
public class ShopBlockEntity extends BlockEntity {
    private static final String CATALOG_KEY = "Catalog";

    private ResourceLocation catalogId = ShopCatalogData.GENERAL;

    public ShopBlockEntity(BlockPos pos, BlockState state) {
        super(InitBlockEntities.SHOP.get(), pos, state);
    }

    /** @return the price list this shop deals by; empty when the pack no longer has it */
    public ShopCatalog catalog() {
        return ShopCatalogs.get(catalogId);
    }

    public ResourceLocation catalogId() {
        return catalogId;
    }

    /** Points the shop at another price list, for a map maker putting up a themed stall. */
    public void setCatalogId(ResourceLocation catalogId) {
        this.catalogId = catalogId;
        setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(CATALOG_KEY)) {
            ResourceLocation saved = ResourceLocation.tryParse(tag.getString(CATALOG_KEY));
            catalogId = saved == null ? ShopCatalogData.GENERAL : saved;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(CATALOG_KEY, catalogId.toString());
    }
}
