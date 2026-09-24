package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

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
    public record SlipView(Identifier type, Identifier icon, Identifier capability,
                           int target, String taker) {
        public static final StreamCodec<FriendlyByteBuf, SlipView> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeIdentifier(value.type);
                buffer.writeIdentifier(value.icon);
                buffer.writeIdentifier(value.capability);
                buffer.writeVarInt(value.target);
                buffer.writeUtf(value.taker);
            },
            buffer -> new SlipView(
                buffer.readIdentifier(),
                buffer.readIdentifier(),
                buffer.readIdentifier(),
                buffer.readVarInt(),
                buffer.readUtf()
            )
        );
    }

    /**
     * The next level of a board, as its screen offers it. The levels are server data, so the
     * screen is told rather than working it out.
     *
     * @param price what it costs, or 0 when there is none left to buy
     * @param daily how many slips a day it puts up
     * @param unlocks the kinds of work a board first puts up at it
     */
    public record NextLevel(int price, int daily, List<Identifier> unlocks) {
        /** A board at the top: nothing left to buy. */
        public static final NextLevel NONE = new NextLevel(0, 0, List.of());

        public static final StreamCodec<FriendlyByteBuf, NextLevel> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.price);
                buffer.writeVarInt(value.daily);
                buffer.writeCollection(value.unlocks, FriendlyByteBuf::writeIdentifier);
            },
            buffer -> new NextLevel(buffer.readVarInt(), buffer.readVarInt(),
                buffer.readList(FriendlyByteBuf::readIdentifier))
        );
    }

    /**
     * Opens the labor board screen with the day's slips, and sends it again after an
     * upgrade so the screen shows what was just paid for.
     *
     * @param board which board; the screen sends it back when the owner buys a level
     * @param level how far the board has been paid up
     * @param daily how many slips a day it puts up at that level
     * @param next what the level after it costs and gives
     */
    public record BoardSlipsPayload(BlockPos board, int level, int daily, NextLevel next, List<SlipView> slips)
            implements CustomPacketPayload {
        public static final Type<BoardSlipsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "board_slips"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BoardSlipsPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeBlockPos(value.board);
                buffer.writeVarInt(value.level);
                buffer.writeVarInt(value.daily);
                NextLevel.STREAM_CODEC.encode(buffer, value.next);
                buffer.writeCollection(value.slips, (buf, slip) -> SlipView.STREAM_CODEC.encode(buf, slip));
            },
            buffer -> new BoardSlipsPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(),
                NextLevel.STREAM_CODEC.decode(buffer), buffer.readList(buf -> SlipView.STREAM_CODEC.decode(buf)))
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
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "board_upgrade"));
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
