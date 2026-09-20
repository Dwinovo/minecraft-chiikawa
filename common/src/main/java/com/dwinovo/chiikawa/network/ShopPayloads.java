package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** What a shop shows its customer, and what the customer asks it for. */
public final class ShopPayloads {
    public static final ResourceLocation SHOP_PRICES = new ResourceLocation(Constants.MOD_ID, "shop_prices");
    public static final ResourceLocation SHOP_TRADE = new ResourceLocation(Constants.MOD_ID, "shop_trade");

    private ShopPayloads() {
    }

    /**
     * One line of a shop's price list, as the screen needs it.
     *
     * @param item what is being priced
     * @param buy what the customer pays for one; 0 when it is not for sale
     * @param sell what the shop pays for one; 0 when it does not want any
     */
    public record PriceView(ResourceLocation item, int buy, int sell) {
        public static PriceView read(FriendlyByteBuf buffer) {
            return new PriceView(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readVarInt());
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(item);
            buffer.writeVarInt(buy);
            buffer.writeVarInt(sell);
        }
    }

    /** Opens the shop screen with what this counter deals in today. */
    public record ShopPricesPayload(BlockPos shop, List<PriceView> prices) implements MusicPayloads.Payload {
        public static ShopPricesPayload read(FriendlyByteBuf buffer) {
            BlockPos shop = buffer.readBlockPos();
            int size = buffer.readVarInt();
            List<PriceView> prices = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                prices.add(PriceView.read(buffer));
            }
            return new ShopPricesPayload(shop, prices);
        }

        @Override
        public ResourceLocation id() {
            return SHOP_PRICES;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(shop);
            buffer.writeVarInt(prices.size());
            for (PriceView price : prices) {
                price.write(buffer);
            }
        }
    }

    /**
     * One trade, asked for by the customer.
     *
     * @param shop which counter; the server checks the player is still standing at it
     * @param item what is being traded, rather than a row number — a data pack reload
     *             between opening the screen and clicking would shift the rows under it
     * @param buying true to buy one from the shop, false to sell it one
     */
    public record ShopTradePayload(BlockPos shop, ResourceLocation item, boolean buying) implements MusicPayloads.Payload {
        public static ShopTradePayload read(FriendlyByteBuf buffer) {
            return new ShopTradePayload(buffer.readBlockPos(), buffer.readResourceLocation(), buffer.readBoolean());
        }

        @Override
        public ResourceLocation id() {
            return SHOP_TRADE;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(shop);
            buffer.writeResourceLocation(item);
            buffer.writeBoolean(buying);
        }
    }
}
