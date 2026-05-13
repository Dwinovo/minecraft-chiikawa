package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.client.music.ClientMusicPacketHandler;
import com.dwinovo.chiikawa.network.MusicPayloads;
import com.dwinovo.chiikawa.network.MusicServerPacketHandler;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ForgeMusicNetworking {
    private static final int PROTOCOL_VERSION = 1;
    private static final SimpleChannel CHANNEL = ChannelBuilder.named(new ResourceLocation(Constants.MOD_ID, "music"))
        .networkProtocolVersion(PROTOCOL_VERSION)
        .simpleChannel();
    private static int packetId;

    private ForgeMusicNetworking() {
    }

    public static void register() {
        clientbound(MusicPayloads.MusicCatalogPayload.class, MusicPayloads.MusicCatalogPayload::read,
            (payload, context) -> ClientMusicPacketHandler.handleCatalog(payload));
        clientbound(MusicPayloads.MusicStreamStartPayload.class, MusicPayloads.MusicStreamStartPayload::read,
            (payload, context) -> ClientMusicPacketHandler.handleStreamStart(payload));
        clientbound(MusicPayloads.MusicStreamChunkPayload.class, MusicPayloads.MusicStreamChunkPayload::read,
            (payload, context) -> ClientMusicPacketHandler.handleStreamChunk(payload));
        clientbound(MusicPayloads.MusicStreamStopPayload.class, MusicPayloads.MusicStreamStopPayload::read,
            (payload, context) -> ClientMusicPacketHandler.handleStreamStop(payload));
        serverbound(MusicPayloads.MusicCatalogRequestPayload.class, MusicPayloads.MusicCatalogRequestPayload::read,
            (payload, context) -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    MusicServerPacketHandler.handleCatalogRequest(payload, player);
                }
            });
        serverbound(MusicPayloads.MusicBoxSelectTrackPayload.class, MusicPayloads.MusicBoxSelectTrackPayload::read,
            (payload, context) -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    MusicServerPacketHandler.handleSelectTrack(payload, player);
                }
            });
    }

    public static void sendToClient(ServerPlayer player, MusicPayloads.Payload payload) {
        CHANNEL.send(payload, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToServer(MusicPayloads.Payload payload) {
        CHANNEL.send(payload, PacketDistributor.SERVER.noArg());
    }

    private static <T extends MusicPayloads.Payload> void clientbound(
        Class<T> type,
        Function<FriendlyByteBuf, T> decoder,
        BiConsumer<T, CustomPayloadEvent.Context> handler
    ) {
        message(type, decoder, handler, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static <T extends MusicPayloads.Payload> void serverbound(
        Class<T> type,
        Function<FriendlyByteBuf, T> decoder,
        BiConsumer<T, CustomPayloadEvent.Context> handler
    ) {
        message(type, decoder, handler, NetworkDirection.PLAY_TO_SERVER);
    }

    private static <T extends MusicPayloads.Payload> void message(
        Class<T> type,
        Function<FriendlyByteBuf, T> decoder,
        BiConsumer<T, CustomPayloadEvent.Context> handler,
        NetworkDirection direction
    ) {
        CHANNEL.messageBuilder(type, packetId++, direction)
            .encoder(MusicPayloads.Payload::write)
            .decoder(decoder)
            .consumerMainThread((payload, context) -> {
                handler.accept(payload, context);
                context.setPacketHandled(true);
            })
            .add();
    }
}
