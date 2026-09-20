package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.client.board.ClientBoardPacketHandler;
import com.dwinovo.chiikawa.client.music.ClientMusicPacketHandler;
import com.dwinovo.chiikawa.client.shop.ClientShopPacketHandler;
import com.dwinovo.chiikawa.network.BoardPayloads;
import com.dwinovo.chiikawa.network.BoardServerPacketHandler;
import com.dwinovo.chiikawa.network.ShopPayloads;
import com.dwinovo.chiikawa.network.ShopServerPacketHandler;
import com.dwinovo.chiikawa.network.MusicPayloads;
import com.dwinovo.chiikawa.network.MusicServerPacketHandler;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Registers every payload the mod sends, and the client side of the ones it receives. */
public final class FabricModNetworking {
    private FabricModNetworking() {
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(MusicPayloads.MUSIC_BOX_SELECT_TRACK,
            (server, player, handler, buffer, responseSender) -> {
                MusicPayloads.MusicBoxSelectTrackPayload payload = MusicPayloads.MusicBoxSelectTrackPayload.read(buffer);
                server.execute(() -> MusicServerPacketHandler.handleSelectTrack(payload, player));
            });
        ServerPlayNetworking.registerGlobalReceiver(MusicPayloads.MUSIC_CATALOG_REQUEST,
            (server, player, handler, buffer, responseSender) -> {
                MusicPayloads.MusicCatalogRequestPayload payload = MusicPayloads.MusicCatalogRequestPayload.read(buffer);
                server.execute(() -> MusicServerPacketHandler.handleCatalogRequest(payload, player));
            });
        ServerPlayNetworking.registerGlobalReceiver(BoardPayloads.BOARD_UPGRADE,
            (server, player, handler, buffer, responseSender) -> {
                BoardPayloads.BoardUpgradePayload payload = BoardPayloads.BoardUpgradePayload.read(buffer);
                server.execute(() -> BoardServerPacketHandler.handleUpgrade(payload, player));
            });
        ServerPlayNetworking.registerGlobalReceiver(ShopPayloads.SHOP_TRADE,
            (server, player, handler, buffer, responseSender) -> {
                ShopPayloads.ShopTradePayload payload = ShopPayloads.ShopTradePayload.read(buffer);
                server.execute(() -> ShopServerPacketHandler.handleTrade(payload, player));
            });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BoardPayloads.BOARD_SLIPS,
            (client, handler, buffer, responseSender) -> {
                BoardPayloads.BoardSlipsPayload payload = BoardPayloads.BoardSlipsPayload.read(buffer);
                client.execute(() -> ClientBoardPacketHandler.handleSlips(payload));
            });
        ClientPlayNetworking.registerGlobalReceiver(ShopPayloads.SHOP_PRICES,
            (client, handler, buffer, responseSender) -> {
                ShopPayloads.ShopPricesPayload payload = ShopPayloads.ShopPricesPayload.read(buffer);
                client.execute(() -> ClientShopPacketHandler.handlePrices(payload));
            });
        ClientPlayNetworking.registerGlobalReceiver(MusicPayloads.MUSIC_CATALOG,
            (client, handler, buffer, responseSender) -> {
                MusicPayloads.MusicCatalogPayload payload = MusicPayloads.MusicCatalogPayload.read(buffer);
                client.execute(() -> ClientMusicPacketHandler.handleCatalog(payload));
            });
        ClientPlayNetworking.registerGlobalReceiver(MusicPayloads.MUSIC_STREAM_START,
            (client, handler, buffer, responseSender) -> {
                MusicPayloads.MusicStreamStartPayload payload = MusicPayloads.MusicStreamStartPayload.read(buffer);
                client.execute(() -> ClientMusicPacketHandler.handleStreamStart(payload));
            });
        ClientPlayNetworking.registerGlobalReceiver(MusicPayloads.MUSIC_STREAM_CHUNK,
            (client, handler, buffer, responseSender) -> {
                MusicPayloads.MusicStreamChunkPayload payload = MusicPayloads.MusicStreamChunkPayload.read(buffer);
                client.execute(() -> ClientMusicPacketHandler.handleStreamChunk(payload));
            });
        ClientPlayNetworking.registerGlobalReceiver(MusicPayloads.MUSIC_STREAM_STOP,
            (client, handler, buffer, responseSender) -> {
                MusicPayloads.MusicStreamStopPayload payload = MusicPayloads.MusicStreamStopPayload.read(buffer);
                client.execute(() -> ClientMusicPacketHandler.handleStreamStop(payload));
            });
    }

    public static void sendToClient(ServerPlayer player, MusicPayloads.Payload payload) {
        ServerPlayNetworking.send(player, payload.id(), write(payload));
    }

    public static void sendToServer(MusicPayloads.Payload payload) {
        ClientPlayNetworking.send(payload.id(), write(payload));
    }

    private static FriendlyByteBuf write(MusicPayloads.Payload payload) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        payload.write(buffer);
        return buffer;
    }
}
