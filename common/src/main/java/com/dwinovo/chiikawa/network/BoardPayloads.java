package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** What a player sees on a labor board, sent when they open one. */
public final class BoardPayloads {
    public static final ResourceLocation BOARD_SLIPS = new ResourceLocation(Constants.MOD_ID, "board_slips");
    public static final ResourceLocation BOARD_UPGRADE = new ResourceLocation(Constants.MOD_ID, "board_upgrade");

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
        public static SlipView read(FriendlyByteBuf buffer) {
            return new SlipView(
                buffer.readResourceLocation(),
                buffer.readResourceLocation(),
                buffer.readResourceLocation(),
                buffer.readVarInt(),
                buffer.readUtf()
            );
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(type);
            buffer.writeResourceLocation(icon);
            buffer.writeResourceLocation(capability);
            buffer.writeVarInt(target);
            buffer.writeUtf(taker);
        }
    }

    /**
     * The next level of a board, as its screen offers it. The levels are server data, so the
     * screen is told rather than working it out.
     *
     * @param price what it costs, or 0 when there is none left to buy
     * @param daily how many slips a day it puts up
     * @param unlocks the kinds of work a board first puts up at it
     */
    public record NextLevel(int price, int daily, List<ResourceLocation> unlocks) {
        /** A board at the top: nothing left to buy. */
        public static final NextLevel NONE = new NextLevel(0, 0, List.of());

        public static NextLevel read(FriendlyByteBuf buffer) {
            return new NextLevel(buffer.readVarInt(), buffer.readVarInt(),
                buffer.readList(FriendlyByteBuf::readResourceLocation));
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(price);
            buffer.writeVarInt(daily);
            buffer.writeCollection(unlocks, FriendlyByteBuf::writeResourceLocation);
        }
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
            implements MusicPayloads.Payload {
        public static BoardSlipsPayload read(FriendlyByteBuf buffer) {
            return new BoardSlipsPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(),
                NextLevel.read(buffer), buffer.readList(SlipView::read));
        }

        @Override
        public ResourceLocation id() {
            return BOARD_SLIPS;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(board);
            buffer.writeVarInt(level);
            buffer.writeVarInt(daily);
            next.write(buffer);
            buffer.writeCollection(slips, (buf, slip) -> slip.write(buf));
        }
    }

    /**
     * A level bought for a board. The price is not in here: what a level costs is the
     * server's business, and a screen only asks for the next one.
     *
     * @param board which board; the server checks the player is still standing at it
     */
    public record BoardUpgradePayload(BlockPos board) implements MusicPayloads.Payload {
        public static BoardUpgradePayload read(FriendlyByteBuf buffer) {
            return new BoardUpgradePayload(buffer.readBlockPos());
        }

        @Override
        public ResourceLocation id() {
            return BOARD_UPGRADE;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(board);
        }
    }
}
