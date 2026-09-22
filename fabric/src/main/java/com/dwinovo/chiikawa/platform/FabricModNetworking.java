package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.client.board.ClientBoardPacketHandler;
import com.dwinovo.chiikawa.client.manual.ClientManualPacketHandler;
import com.dwinovo.chiikawa.client.music.ClientMusicPacketHandler;
import com.dwinovo.chiikawa.client.shop.ClientShopPacketHandler;
import com.dwinovo.chiikawa.network.BoardPayloads;
import com.dwinovo.chiikawa.network.BoardServerPacketHandler;
import com.dwinovo.chiikawa.network.ManualPayloads;
import com.dwinovo.chiikawa.network.PetPayloads;
import com.dwinovo.chiikawa.network.PetServerPacketHandler;
import com.dwinovo.chiikawa.network.ShopPayloads;
import com.dwinovo.chiikawa.network.ShopServerPacketHandler;
import com.dwinovo.chiikawa.network.MusicPayloads;
import com.dwinovo.chiikawa.network.MusicServerPacketHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Registers every payload the mod sends, and the client side of the ones it receives. */
public final class FabricModNetworking {
    private FabricModNetworking() {
    }

    public static void registerServer() {
        PayloadTypeRegistry.clientboundPlay().register(BoardPayloads.BoardSlipsPayload.TYPE, BoardPayloads.BoardSlipsPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BoardPayloads.BoardUpgradePayload.TYPE, BoardPayloads.BoardUpgradePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PetPayloads.PetDirectivePayload.TYPE, PetPayloads.PetDirectivePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ShopPayloads.ShopPricesPayload.TYPE, ShopPayloads.ShopPricesPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ManualPayloads.OpenHandbookPayload.TYPE, ManualPayloads.OpenHandbookPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShopPayloads.ShopTradePayload.TYPE, ShopPayloads.ShopTradePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MusicPayloads.MusicCatalogPayload.TYPE, MusicPayloads.MusicCatalogPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MusicPayloads.MusicStreamStartPayload.TYPE, MusicPayloads.MusicStreamStartPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MusicPayloads.MusicStreamChunkPayload.TYPE, MusicPayloads.MusicStreamChunkPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MusicPayloads.MusicStreamStopPayload.TYPE, MusicPayloads.MusicStreamStopPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MusicPayloads.MusicBoxSelectTrackPayload.TYPE, MusicPayloads.MusicBoxSelectTrackPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MusicPayloads.MusicCatalogRequestPayload.TYPE, MusicPayloads.MusicCatalogRequestPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MusicPayloads.MusicBoxSelectTrackPayload.TYPE,
            (payload, context) -> context.server().execute(() -> MusicServerPacketHandler.handleSelectTrack(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(MusicPayloads.MusicCatalogRequestPayload.TYPE,
            (payload, context) -> context.server().execute(() -> MusicServerPacketHandler.handleCatalogRequest(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(BoardPayloads.BoardUpgradePayload.TYPE,
            (payload, context) -> context.server().execute(() -> BoardServerPacketHandler.handleUpgrade(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(PetPayloads.PetDirectivePayload.TYPE,
            (payload, context) -> context.server().execute(() -> PetServerPacketHandler.handleDirective(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(ShopPayloads.ShopTradePayload.TYPE,
            (payload, context) -> context.server().execute(() -> ShopServerPacketHandler.handleTrade(payload, context.player())));
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BoardPayloads.BoardSlipsPayload.TYPE,
            (payload, context) -> context.client().execute(() -> ClientBoardPacketHandler.handleSlips(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ShopPayloads.ShopPricesPayload.TYPE,
            (payload, context) -> context.client().execute(() -> ClientShopPacketHandler.handlePrices(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ManualPayloads.OpenHandbookPayload.TYPE,
            (payload, context) -> context.client().execute(ClientManualPacketHandler::open));
        ClientPlayNetworking.registerGlobalReceiver(MusicPayloads.MusicCatalogPayload.TYPE,
            (payload, context) -> context.client().execute(() -> ClientMusicPacketHandler.handleCatalog(payload)));
        ClientPlayNetworking.registerGlobalReceiver(MusicPayloads.MusicStreamStartPayload.TYPE,
            (payload, context) -> context.client().execute(() -> ClientMusicPacketHandler.handleStreamStart(payload)));
        ClientPlayNetworking.registerGlobalReceiver(MusicPayloads.MusicStreamChunkPayload.TYPE,
            (payload, context) -> context.client().execute(() -> ClientMusicPacketHandler.handleStreamChunk(payload)));
        ClientPlayNetworking.registerGlobalReceiver(MusicPayloads.MusicStreamStopPayload.TYPE,
            (payload, context) -> context.client().execute(() -> ClientMusicPacketHandler.handleStreamStop(payload)));
    }
}
