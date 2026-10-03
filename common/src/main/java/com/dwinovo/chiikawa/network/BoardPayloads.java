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

        public static final StreamCodec<FriendlyByteBuf, NextLevel> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.price);
                buffer.writeVarInt(value.daily);
                buffer.writeCollection(value.unlocks, FriendlyByteBuf::writeResourceLocation);
            },
            buffer -> new NextLevel(buffer.readVarInt(), buffer.readVarInt(),
                buffer.readList(FriendlyByteBuf::readResourceLocation))
        );
    }

    /**
     * The exams a board's screen offers its owner, and the results it has posted. Worked out
     * on the server for the owner looking, since who would go is their pets.
     *
     * @param offers one for each licence: what opening its exam here would come to
     * @param posted who sat what here last time, and how they did
     */
    public record ExamView(List<Offer> offers, List<Posted> posted) {
        public static final StreamCodec<FriendlyByteBuf, ExamView> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeCollection(value.offers, (buf, offer) -> {
                    buf.writeResourceLocation(offer.qualification());
                    buf.writeResourceLocation(offer.feeItem());
                    buf.writeVarInt(offer.feeCount());
                    buf.writeBoolean(offer.open());
                    buf.writeCollection(offer.going(), FriendlyByteBuf::writeUtf);
                    buf.writeCollection(offer.called(), FriendlyByteBuf::writeUtf);
                });
                buffer.writeCollection(value.posted, (buf, sitting) -> {
                    buf.writeUtf(sitting.name());
                    buf.writeResourceLocation(sitting.qualification());
                    buf.writeVarInt(sitting.rank());
                    buf.writeBoolean(sitting.passed());
                });
            },
            buffer -> new ExamView(
                buffer.readList(buf -> new Offer(buf.readResourceLocation(), buf.readResourceLocation(), buf.readVarInt(),
                    buf.readBoolean(), buf.readList(FriendlyByteBuf::readUtf), buf.readList(FriendlyByteBuf::readUtf))),
                buffer.readList(buf -> new Posted(buf.readUtf(), buf.readResourceLocation(), buf.readVarInt(),
                    buf.readBoolean())))
        );

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
                                    ExamView exams) implements CustomPacketPayload {
        public static final Type<BoardSlipsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "board_slips"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BoardSlipsPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeBlockPos(value.board);
                buffer.writeVarInt(value.level);
                buffer.writeVarInt(value.daily);
                NextLevel.STREAM_CODEC.encode(buffer, value.next);
                buffer.writeCollection(value.slips, (buf, slip) -> SlipView.STREAM_CODEC.encode(buf, slip));
                ExamView.STREAM_CODEC.encode(buffer, value.exams);
            },
            buffer -> new BoardSlipsPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(),
                NextLevel.STREAM_CODEC.decode(buffer), buffer.readList(buf -> SlipView.STREAM_CODEC.decode(buf)),
                ExamView.STREAM_CODEC.decode(buffer))
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

    /**
     * An exam opened at a board. The fee is not in here: what it costs and who goes are the
     * server's business, and a screen only asks.
     *
     * @param board which board; the server checks the player is still standing at it
     * @param qualification the licence whose exam is opened
     */
    public record BoardExamPayload(BlockPos board, ResourceLocation qualification) implements CustomPacketPayload {
        public static final Type<BoardExamPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "board_exam"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BoardExamPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeBlockPos(value.board);
                buffer.writeResourceLocation(value.qualification);
            },
            buffer -> new BoardExamPayload(buffer.readBlockPos(), buffer.readResourceLocation())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
