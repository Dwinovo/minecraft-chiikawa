package com.dwinovo.chiikawa.block;

import com.dwinovo.chiikawa.data.ShopCatalogData;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.shop.ShopCatalog;
import com.dwinovo.chiikawa.shop.ShopCatalogs;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Which price list a shop quotes from. That is all a shop is, for now: the prices live in
 * a data pack and the stock is whatever the world produces, so two shops differ by the
 * list they name and nothing else.
 */
public class ShopBlockEntity extends BlockEntity {
    private static final String CATALOG_KEY = "Catalog";

    private Identifier catalogId = ShopCatalogData.GENERAL;

    public ShopBlockEntity(BlockPos pos, BlockState state) {
        super(InitBlockEntities.SHOP.get(), pos, state);
    }

    /** @return the price list this shop deals by; empty when the pack no longer has it */
    public ShopCatalog catalog() {
        return ShopCatalogs.get(catalogId);
    }

    public Identifier catalogId() {
        return catalogId;
    }

    /** Points the shop at another price list, for a map maker putting up a themed stall. */
    public void setCatalogId(Identifier catalogId) {
        this.catalogId = catalogId;
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getString(CATALOG_KEY).ifPresent(id -> {
            Identifier saved = Identifier.tryParse(id);
            catalogId = saved == null ? ShopCatalogData.GENERAL : saved;
        });
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString(CATALOG_KEY, catalogId.toString());
    }
}
