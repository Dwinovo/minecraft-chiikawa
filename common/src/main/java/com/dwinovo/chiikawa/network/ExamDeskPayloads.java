package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.qualification.ExamEnrollment;
import com.dwinovo.chiikawa.qualification.Ineligible;
import com.dwinovo.chiikawa.qualification.PassOdds;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** What an owner sees at an exam desk, and the pet they sign up there. */
public final class ExamDeskPayloads {
    private ExamDeskPayloads() {
    }

    /**
     * Who the desk has, as its screen shows it.
     *
     * @param rank the grade being sat, as the player reads it
     */
    public record BookingView(String name, ResourceLocation qualification, int rank, Stage stage) {
        /** Where that pet is with it. */
        public enum Stage {
            SITTING, HANDED_IN, PASSED, FAILED
        }

        static final StreamCodec<FriendlyByteBuf, BookingView> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeUtf(value.name);
                buffer.writeResourceLocation(value.qualification);
                buffer.writeVarInt(value.rank);
                buffer.writeEnum(value.stage);
            },
            buffer -> new BookingView(buffer.readUtf(), buffer.readResourceLocation(), buffer.readVarInt(),
                buffer.readEnum(Stage.class))
        );
    }

    /**
     * One of the owner's pets, as the desk offers it for one licence.
     *
     * @param heldRank the grade it holds, as the player reads it; 0 for none
     * @param nextRank the grade it would sit
     * @param whyNot why it cannot be signed up, if it cannot
     */
    public record CandidateView(UUID pet, String name, int heldRank, int nextRank, PassOdds odds,
                                Optional<Ineligible> whyNot) {
        static final StreamCodec<FriendlyByteBuf, CandidateView> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeUUID(value.pet);
                buffer.writeUtf(value.name);
                buffer.writeVarInt(value.heldRank);
                buffer.writeVarInt(value.nextRank);
                PassOdds odds = value.odds;
                for (float part : new float[] {odds.base(), odds.practice(), odds.failing(), odds.book(), odds.aptitude(), odds.max()}) {
                    buffer.writeFloat(part);
                }
                buffer.writeOptional(value.whyNot, (buf, reason) -> buf.writeEnum(reason));
            },
            buffer -> new CandidateView(buffer.readUUID(), buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt(),
                new PassOdds(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat()),
                buffer.readOptional(buf -> buf.readEnum(Ineligible.class)))
        );
    }

    /**
     * Signing up for one licence at the desk.
     *
     * @param feeItem what the fee is paid in
     * @param feeCount how many
     */
    public record OfferView(ResourceLocation qualification, ResourceLocation feeItem, int feeCount,
                            List<CandidateView> candidates) {
        static final StreamCodec<FriendlyByteBuf, OfferView> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeResourceLocation(value.qualification);
                buffer.writeResourceLocation(value.feeItem);
                buffer.writeVarInt(value.feeCount);
                buffer.writeCollection(value.candidates, (buf, candidate) -> CandidateView.STREAM_CODEC.encode(buf, candidate));
            },
            buffer -> new OfferView(buffer.readResourceLocation(), buffer.readResourceLocation(), buffer.readVarInt(),
                buffer.readList(buf -> CandidateView.STREAM_CODEC.decode(buf)))
        );
    }

    /**
     * Opens the exam desk screen, and sends it again after a sign-up so the screen shows the
     * desk as it now stands. Worked out on the server for the owner looking: the pets are
     * theirs.
     *
     * @param closed why the desk takes nobody now, if it does not
     * @param booking who it has, while it has anything to show for them
     */
    public record DeskViewPayload(BlockPos desk, Optional<ExamEnrollment.Refusal> closed, Optional<BookingView> booking,
                                  List<OfferView> offers) implements CustomPacketPayload {
        public static final Type<DeskViewPayload> TYPE = new Type<>(
            new ResourceLocation(Constants.MOD_ID, "exam_desk_view"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DeskViewPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeBlockPos(value.desk);
                buffer.writeOptional(value.closed, (buf, refusal) -> buf.writeEnum(refusal));
                buffer.writeOptional(value.booking, (buf, booking) -> BookingView.STREAM_CODEC.encode(buf, booking));
                buffer.writeCollection(value.offers, (buf, offer) -> OfferView.STREAM_CODEC.encode(buf, offer));
            },
            buffer -> new DeskViewPayload(buffer.readBlockPos(),
                buffer.readOptional(buf -> buf.readEnum(ExamEnrollment.Refusal.class)),
                buffer.readOptional(buf -> BookingView.STREAM_CODEC.decode(buf)),
                buffer.readList(buf -> OfferView.STREAM_CODEC.decode(buf)))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * A pet signed up at a desk. The fee is not in here: what it costs, and whether the pet
     * may go, is the server's business.
     *
     * @param desk which desk; the server checks the player is still standing at it
     */
    public record SignUpPayload(BlockPos desk, ResourceLocation qualification, UUID pet) implements CustomPacketPayload {
        public static final Type<SignUpPayload> TYPE = new Type<>(
            new ResourceLocation(Constants.MOD_ID, "exam_desk_sign_up"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SignUpPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SignUpPayload::desk,
            ResourceLocation.STREAM_CODEC, SignUpPayload::qualification,
            UUIDUtil.STREAM_CODEC, SignUpPayload::pet,
            SignUpPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
