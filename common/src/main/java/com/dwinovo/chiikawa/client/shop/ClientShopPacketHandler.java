package com.dwinovo.chiikawa.client.shop;

import com.dwinovo.chiikawa.client.screen.ShopScreen;
import com.dwinovo.chiikawa.network.ShopPayloads.ShopPricesPayload;
import net.minecraft.client.Minecraft;

/** Opens the shop screen with the prices the server sent. */
public final class ClientShopPacketHandler {
    private ClientShopPacketHandler() {
    }

    public static void handlePrices(ShopPricesPayload payload) {
        Minecraft.getInstance().setScreen(new ShopScreen(payload.shop(), payload.prices()));
    }
}
