package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** What a shop shows its customer, and what the customer asks it for. */
public final class ShopPayloads {
    private ShopPayloads() {
    }

    /**
     * One line of a shop's price list, as the screen needs it.
     *
     * @param item what is being priced
     * @param buy what the customer pays for one; 0 when it is not for sale
     * @param sell what the shop pays for one; 0 when it does not want any
     */
    public record PriceView(Identifier item, int buy, int sell) {
        public static final StreamCodec<FriendlyByteBuf, PriceView> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeIdentifier(value.item);
                buffer.writeVarInt(value.buy);
                buffer.writeVarInt(value.sell);
            },
            buffer -> new PriceView(buffer.readIdentifier(), buffer.readVarInt(), buffer.readVarInt())
        );
    }

    /** Opens the shop screen with what this counter deals in today. */
    public record ShopPricesPayload(BlockPos shop, List<PriceView> prices) implements CustomPacketPayload {
        public static final Type<ShopPricesPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "shop_prices"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ShopPricesPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeBlockPos(value.shop);
                buffer.writeCollection(value.prices, (buf, price) -> PriceView.STREAM_CODEC.encode(buf, price));
            },
            buffer -> new ShopPricesPayload(buffer.readBlockPos(),
                buffer.readList(buf -> PriceView.STREAM_CODEC.decode(buf)))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
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
    public record ShopTradePayload(BlockPos shop, Identifier item, boolean buying) implements CustomPacketPayload {
        public static final Type<ShopTradePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "shop_trade"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ShopTradePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeBlockPos(value.shop);
                buffer.writeIdentifier(value.item);
                buffer.writeBoolean(value.buying);
            },
            buffer -> new ShopTradePayload(buffer.readBlockPos(), buffer.readIdentifier(), buffer.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
