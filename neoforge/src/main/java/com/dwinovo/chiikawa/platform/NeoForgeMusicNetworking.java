package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.client.music.ClientMusicPacketHandler;
import com.dwinovo.chiikawa.network.MusicPayloads;
import com.dwinovo.chiikawa.network.MusicServerPacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NeoForgeMusicNetworking {
    private NeoForgeMusicNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(MusicPayloads.MusicCatalogPayload.TYPE, MusicPayloads.MusicCatalogPayload.STREAM_CODEC);
        registrar.playToClient(MusicPayloads.MusicStreamStartPayload.TYPE, MusicPayloads.MusicStreamStartPayload.STREAM_CODEC);
        registrar.playToClient(MusicPayloads.MusicStreamChunkPayload.TYPE, MusicPayloads.MusicStreamChunkPayload.STREAM_CODEC);
        registrar.playToClient(MusicPayloads.MusicStreamStopPayload.TYPE, MusicPayloads.MusicStreamStopPayload.STREAM_CODEC);
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

    public static void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(MusicPayloads.MusicCatalogPayload.TYPE, (payload, context) -> ClientMusicPacketHandler.handleCatalog(payload));
        event.register(MusicPayloads.MusicStreamStartPayload.TYPE, (payload, context) -> ClientMusicPacketHandler.handleStreamStart(payload));
        event.register(MusicPayloads.MusicStreamChunkPayload.TYPE, (payload, context) -> ClientMusicPacketHandler.handleStreamChunk(payload));
        event.register(MusicPayloads.MusicStreamStopPayload.TYPE, (payload, context) -> ClientMusicPacketHandler.handleStreamStop(payload));
    }
}
