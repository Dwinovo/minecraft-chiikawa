package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.client.board.ClientBoardPacketHandler;
import com.dwinovo.chiikawa.client.music.ClientMusicPacketHandler;
import com.dwinovo.chiikawa.client.shop.ClientShopPacketHandler;
import com.dwinovo.chiikawa.network.BoardPayloads;
import com.dwinovo.chiikawa.network.BoardServerPacketHandler;
import com.dwinovo.chiikawa.network.PetPayloads;
import com.dwinovo.chiikawa.network.PetServerPacketHandler;
import com.dwinovo.chiikawa.network.ShopPayloads;
import com.dwinovo.chiikawa.network.ShopServerPacketHandler;
import com.dwinovo.chiikawa.network.MusicPayloads;
import com.dwinovo.chiikawa.network.MusicServerPacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers every payload the mod sends, and how the client handles the ones it receives. */
public final class NeoForgeModNetworking {
    private NeoForgeModNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(BoardPayloads.BoardSlipsPayload.TYPE, BoardPayloads.BoardSlipsPayload.STREAM_CODEC,
            (payload, context) -> ClientBoardPacketHandler.handleSlips(payload));
        registrar.playToServer(BoardPayloads.BoardUpgradePayload.TYPE, BoardPayloads.BoardUpgradePayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    BoardServerPacketHandler.handleUpgrade(payload, player);
                }
            });
        registrar.playToServer(PetPayloads.PetDirectivePayload.TYPE, PetPayloads.PetDirectivePayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    PetServerPacketHandler.handleDirective(payload, player);
                }
            });
        registrar.playToClient(ShopPayloads.ShopPricesPayload.TYPE, ShopPayloads.ShopPricesPayload.STREAM_CODEC,
            (payload, context) -> ClientShopPacketHandler.handlePrices(payload));
        registrar.playToServer(ShopPayloads.ShopTradePayload.TYPE, ShopPayloads.ShopTradePayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    ShopServerPacketHandler.handleTrade(payload, player);
                }
            });
        registrar.playToClient(MusicPayloads.MusicCatalogPayload.TYPE, MusicPayloads.MusicCatalogPayload.STREAM_CODEC,
            (payload, context) -> ClientMusicPacketHandler.handleCatalog(payload));
        registrar.playToClient(MusicPayloads.MusicStreamStartPayload.TYPE, MusicPayloads.MusicStreamStartPayload.STREAM_CODEC,
            (payload, context) -> ClientMusicPacketHandler.handleStreamStart(payload));
        registrar.playToClient(MusicPayloads.MusicStreamChunkPayload.TYPE, MusicPayloads.MusicStreamChunkPayload.STREAM_CODEC,
            (payload, context) -> ClientMusicPacketHandler.handleStreamChunk(payload));
        registrar.playToClient(MusicPayloads.MusicStreamStopPayload.TYPE, MusicPayloads.MusicStreamStopPayload.STREAM_CODEC,
            (payload, context) -> ClientMusicPacketHandler.handleStreamStop(payload));
        registrar.playToServer(MusicPayloads.MusicCatalogRequestPayload.TYPE, MusicPayloads.MusicCatalogRequestPayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    MusicServerPacketHandler.handleCatalogRequest(payload, player);
                }
            });
        registrar.playToServer(MusicPayloads.MusicBoxSelectTrackPayload.TYPE, MusicPayloads.MusicBoxSelectTrackPayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    MusicServerPacketHandler.handleSelectTrack(payload, player);
                }
            });
    }

}
