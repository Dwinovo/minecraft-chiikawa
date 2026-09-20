package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.shop.ShopCatalog;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The generated price list (gameplay doc, section 7). One shop for now, dealing in the
 * small comforts a pet spends its wages on and taking in what a farm produces, so the
 * emeralds a board pays out have somewhere to go and a harvest has somewhere to end up.
 *
 * <p>Prices are round numbers on purpose. A pet is paid a couple of emeralds for a slip,
 * so a snack at one and a cake at three make an afternoon's work read as an afternoon's
 * work without anyone doing arithmetic.
 */
public final class ShopCatalogData {
    /** The shop every {@code chiikawa:shop} block quotes from unless it is told otherwise. */
    public static final ResourceLocation GENERAL = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "general");

    private ShopCatalogData() {
    }

    /** @return price lists by id */
    public static Map<ResourceLocation, ShopCatalog> all() {
        return Map.of(GENERAL, new ShopCatalog(List.of(
            // Snacks and suppers: what most pets want, and what an owner feeds them.
            sells(Items.COOKIE, 1),
            sells(Items.BREAD, 1),
            sells(Items.APPLE, 1),
            sells(Items.SWEET_BERRIES, 1),
            sells(Items.BAKED_POTATO, 1),
            sells(Items.DRIED_KELP, 1),
            sells(Items.COOKED_COD, 2),
            sells(Items.PUMPKIN_PIE, 2),
            sells(Items.HONEY_BOTTLE, 2),
            sells(Items.MILK_BUCKET, 3),
            sells(Items.CAKE, 3),
            // Things to cook with.
            sells(Items.SUGAR, 1),
            sells(Items.EGG, 1),
            // Small pretty things.
            sells(Items.POPPY, 1),
            sells(Items.DANDELION, 1),
            sells(Items.PINK_TULIP, 1),
            // Things for a pet itself. A name tag is vanilla's own, and a player already
            // knows what one is for; the shop only has to have one.
            sells(InitItems.SIMPLE_DISH.get(), 3),
            sells(Items.NAME_TAG, 8),
            sells(InitItems.BEAR_BACKPACK.get(), 12),
            sells(InitItems.PET_BELL.get(), 16),
            // Reading.
            sells(Items.PAPER, 1),
            sells(Items.BOOK, 2),
            // What a farm brings in. Wheat is both: bought by the shop off a harvest, sold
            // back to anyone who needs it to cook with.
            trades(Items.WHEAT, 2, 1),
            buys(Items.POTATO, 1),
            buys(Items.CARROT, 1),
            buys(Items.BEETROOT, 1),
            buys(Items.RED_MUSHROOM, 1),
            buys(Items.BROWN_MUSHROOM, 1))));
    }

    /** On the shelf at this price, and not taken in. */
    private static ShopCatalog.Entry sells(Item item, int buy) {
        return new ShopCatalog.Entry(item, buy, 0);
    }

    /** Taken in at this price, and not on the shelf. */
    private static ShopCatalog.Entry buys(Item item, int sell) {
        return new ShopCatalog.Entry(item, 0, sell);
    }

    /** Both, at the shop's margin. */
    private static ShopCatalog.Entry trades(Item item, int buy, int sell) {
        return new ShopCatalog.Entry(item, buy, sell);
    }
}
