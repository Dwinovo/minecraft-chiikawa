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
    public static final ResourceLocation BOARD_EXAM = new ResourceLocation(Constants.MOD_ID, "board_exam");

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
     * The exams a board's screen offers its owner, and the results it has posted. Worked out
     * on the server for the owner looking, since who would go is their pets.
     *
     * @param offers one for each licence: what opening its exam here would come to
     * @param posted who sat what here last time, and how they did
     */
    public record ExamView(List<Offer> offers, List<Posted> posted) {
        public static ExamView read(FriendlyByteBuf buffer) {
            return new ExamView(
                buffer.readList(buf -> new Offer(buf.readResourceLocation(), buf.readResourceLocation(), buf.readVarInt(),
                    buf.readBoolean(), buf.readList(FriendlyByteBuf::readUtf), buf.readList(FriendlyByteBuf::readUtf))),
                buffer.readList(buf -> new Posted(buf.readUtf(), buf.readResourceLocation(), buf.readVarInt(),
                    buf.readBoolean())));
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeCollection(offers, (buf, offer) -> {
                buf.writeResourceLocation(offer.qualification());
                buf.writeResourceLocation(offer.feeItem());
                buf.writeVarInt(offer.feeCount());
                buf.writeBoolean(offer.open());
                buf.writeCollection(offer.going(), FriendlyByteBuf::writeUtf);
                buf.writeCollection(offer.called(), FriendlyByteBuf::writeUtf);
            });
            buffer.writeCollection(posted, (buf, sitting) -> {
                buf.writeUtf(sitting.name());
                buf.writeResourceLocation(sitting.qualification());
                buf.writeVarInt(sitting.rank());
                buf.writeBoolean(sitting.passed());
            });
        }

        /**
         * One licence's exam, as the owner could open it here now.
         *
         * @param feeItem what the fee is paid in
         * @param feeCount how many
         * @param open whether it may be opened at this time of day
         * @param going the owner's pets that would be called
         * @param called the owner's pets called here today that have not handed in yet
         */
        public record Offer(ResourceLocation qualification, ResourceLocation feeItem, int feeCount, boolean open,
                            List<String> going, List<String> called) {
        }

        /** @param rank the grade sat, as the player reads it */
        public record Posted(String name, ResourceLocation qualification, int rank, boolean passed) {
        }
    }

    /**
     * Opens the labor board screen with the day's slips, and sends it again after an
     * upgrade or an exam is opened so the screen shows what was just paid for.
     *
     * @param board which board; the screen sends it back when the owner buys a level
     * @param level how far the board has been paid up
     * @param daily how many slips a day it puts up at that level
     * @param next what the level after it costs and gives
     * @param exams the exams the owner can open here, and the results posted
     */
    public record BoardSlipsPayload(BlockPos board, int level, int daily, NextLevel next, List<SlipView> slips,
                                    ExamView exams) implements MusicPayloads.Payload {
        public static BoardSlipsPayload read(FriendlyByteBuf buffer) {
            return new BoardSlipsPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(),
                NextLevel.read(buffer), buffer.readList(SlipView::read), ExamView.read(buffer));
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
            exams.write(buffer);
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

    /**
     * An exam opened at a board. The fee is not in here: what it costs and who goes are the
     * server's business, and a screen only asks.
     *
     * @param board which board; the server checks the player is still standing at it
     * @param qualification the licence whose exam is opened
     */
    public record BoardExamPayload(BlockPos board, ResourceLocation qualification) implements MusicPayloads.Payload {
        public static BoardExamPayload read(FriendlyByteBuf buffer) {
            return new BoardExamPayload(buffer.readBlockPos(), buffer.readResourceLocation());
        }

        @Override
        public ResourceLocation id() {
            return BOARD_EXAM;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(board);
            buffer.writeResourceLocation(qualification);
        }
    }
}
