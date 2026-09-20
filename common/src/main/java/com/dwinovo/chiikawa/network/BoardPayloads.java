package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import java.util.ArrayList;
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
     * Opens the labor board screen with the day's slips, and sends it again after an
     * upgrade so the screen shows what was just paid for.
     *
     * @param board which board; the screen sends it back when the owner buys a level
     * @param level how far the board has been paid up
     * @param price what the next level costs, or 0 when there is none left to buy
     */
    public record BoardSlipsPayload(BlockPos board, int level, int price, List<SlipView> slips)
            implements MusicPayloads.Payload {
        public static BoardSlipsPayload read(FriendlyByteBuf buffer) {
            BlockPos board = buffer.readBlockPos();
            int level = buffer.readVarInt();
            int price = buffer.readVarInt();
            int size = buffer.readVarInt();
            List<SlipView> slips = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                slips.add(SlipView.read(buffer));
            }
            return new BoardSlipsPayload(board, level, price, slips);
        }

        @Override
        public ResourceLocation id() {
            return BOARD_SLIPS;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(board);
            buffer.writeVarInt(level);
            buffer.writeVarInt(price);
            buffer.writeVarInt(slips.size());
            for (SlipView slip : slips) {
                slip.write(buffer);
            }
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
