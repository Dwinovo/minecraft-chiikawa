package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import java.util.List;
import net.minecraft.core.BlockPos;
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
     * @param icon the item the slip is pictured as
     * @param capability the job it is for
     * @param target how much work it asks for
     * @param taker who took it; empty while it is still up
     */
    public record SlipView(ResourceLocation type, ResourceLocation icon, ResourceLocation capability,
                           int target, String taker) {
        public static final StreamCodec<FriendlyByteBuf, SlipView> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeResourceLocation(value.type);
                buffer.writeResourceLocation(value.icon);
                buffer.writeResourceLocation(value.capability);
                buffer.writeVarInt(value.target);
                buffer.writeUtf(value.taker);
            },
            buffer -> new SlipView(
                buffer.readResourceLocation(),
                buffer.readResourceLocation(),
                buffer.readResourceLocation(),
                buffer.readVarInt(),
                buffer.readUtf()
            )
        );
    }

    /**
     * Opens the labor board screen with the day's slips, and sends it again after an
     * upgrade so the screen shows what was just paid for.
     *
     * @param board which board; the screen sends it back when the owner buys a level
     * @param level how far the board has been paid up
     * @param price what the next level costs, or 0 when there is none left to buy
     */
    public record BoardSlipsPayload(BlockPos board, int level, int price, List<SlipView> slips)
            implements CustomPacketPayload {
        public static final Type<BoardSlipsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "board_slips"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BoardSlipsPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeBlockPos(value.board);
                buffer.writeVarInt(value.level);
                buffer.writeVarInt(value.price);
                buffer.writeCollection(value.slips, (buf, slip) -> SlipView.STREAM_CODEC.encode(buf, slip));
            },
            buffer -> new BoardSlipsPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readList(buf -> SlipView.STREAM_CODEC.decode(buf)))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * A level bought for a board. The price is not in here: what a level costs is the
     * server's business, and a screen only asks for the next one.
     *
     * @param board which board; the server checks the player is still standing at it
     */
    public record BoardUpgradePayload(BlockPos board) implements CustomPacketPayload {
        public static final Type<BoardUpgradePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "board_upgrade"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BoardUpgradePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeBlockPos(value.board),
            buffer -> new BoardUpgradePayload(buffer.readBlockPos())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
