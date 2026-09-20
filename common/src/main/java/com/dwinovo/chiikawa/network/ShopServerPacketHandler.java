package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.block.ShopBlockEntity;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.shop.ShopCatalog;
import com.dwinovo.chiikawa.shop.ShopTrade;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;

/**
 * Trades asked for from a shop screen. Everything the screen says is checked again here:
 * a screen is a picture of what the server had a moment ago, and the counter is where the
 * deal actually happens.
 */
public final class ShopServerPacketHandler {
    /** How far from the counter a customer may stand and still be served. */
    private static final double REACH_SQR = 64.0;

    private ShopServerPacketHandler() {
    }

    public static void handleTrade(ShopPayloads.ShopTradePayload payload, ServerPlayer player) {
        if (player.distanceToSqr(payload.shop().getCenter()) > REACH_SQR) {
            return;
        }
        Optional<ShopCatalog> catalog = player.level()
            .getBlockEntity(payload.shop(), InitBlockEntities.SHOP.get())
            .map(ShopBlockEntity::catalog);
        if (catalog.isEmpty()) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(payload.item());
        Optional<ShopCatalog.Entry> entry = payload.buying()
            ? catalog.get().sale(item)
            : catalog.get().purchase(item);
        if (entry.isEmpty()) {
            return;
        }
        boolean done = payload.buying()
            ? ShopTrade.buy(player.getInventory(), entry.get())
            : ShopTrade.sell(player.getInventory(), entry.get());
        if (done) {
            player.level().playSound(null, payload.shop(), SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 0.6F, 1.2F);
            player.containerMenu.broadcastChanges();
        }
    }
}
