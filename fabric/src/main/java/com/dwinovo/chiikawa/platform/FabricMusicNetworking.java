package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.client.music.ClientMusicPacketHandler;
import com.dwinovo.chiikawa.network.MusicPayloads;
import com.dwinovo.chiikawa.network.MusicServerPacketHandler;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class FabricMusicNetworking {
    private FabricMusicNetworking() {
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
    }

    public static void registerClient() {
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
