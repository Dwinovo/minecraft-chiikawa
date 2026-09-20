package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** What a player sees on a labor board, sent when they open one. */
public final class BoardPayloads {
    private BoardPayloads() {
    }

    /**
     * One slip on the board.
     *
     * @param type the slip type, named in the screen
     * @param capability the job it is for
     * @param target how much work it asks for
     * @param taker who took it; empty while it is still up
     */
    public record SlipView(ResourceLocation type, ResourceLocation capability, int target, String taker) {
        public static final StreamCodec<FriendlyByteBuf, SlipView> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeResourceLocation(value.type);
                buffer.writeResourceLocation(value.capability);
                buffer.writeVarInt(value.target);
                buffer.writeUtf(value.taker);
            },
            buffer -> new SlipView(
                buffer.readResourceLocation(),
                buffer.readResourceLocation(),
                buffer.readVarInt(),
                buffer.readUtf()
            )
        );
    }

    /** Opens the labor board screen with the day's slips. */
    public record BoardSlipsPayload(List<SlipView> slips) implements CustomPacketPayload {
        public static final Type<BoardSlipsPayload> TYPE = new Type<>(
            new ResourceLocation(Constants.MOD_ID, "board_slips"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BoardSlipsPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeCollection(value.slips, (buf, slip) -> SlipView.STREAM_CODEC.encode(buf, slip)),
            buffer -> new BoardSlipsPayload(buffer.readList(buf -> SlipView.STREAM_CODEC.decode(buf)))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
