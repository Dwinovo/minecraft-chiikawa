package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.qualification.ExamEnrollment;
import com.dwinovo.chiikawa.qualification.Ineligible;
import com.dwinovo.chiikawa.qualification.PassOdds;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** What an owner sees at an exam desk, and the pet they sign up there. */
public final class ExamDeskPayloads {
    public static final ResourceLocation EXAM_DESK_VIEW = new ResourceLocation(Constants.MOD_ID, "exam_desk_view");
    public static final ResourceLocation EXAM_DESK_SIGN_UP = new ResourceLocation(Constants.MOD_ID, "exam_desk_sign_up");

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

        static BookingView read(FriendlyByteBuf buffer) {
            return new BookingView(buffer.readUtf(), buffer.readResourceLocation(), buffer.readVarInt(),
                buffer.readEnum(Stage.class));
        }

        void write(FriendlyByteBuf buffer) {
            buffer.writeUtf(name);
            buffer.writeResourceLocation(qualification);
            buffer.writeVarInt(rank);
            buffer.writeEnum(stage);
        }
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
        static CandidateView read(FriendlyByteBuf buffer) {
            return new CandidateView(buffer.readUUID(), buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt(),
                new PassOdds(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat()),
                buffer.readOptional(buf -> buf.readEnum(Ineligible.class)));
        }

        void write(FriendlyByteBuf buffer) {
            buffer.writeUUID(pet);
            buffer.writeUtf(name);
            buffer.writeVarInt(heldRank);
            buffer.writeVarInt(nextRank);
            for (float part : new float[] {odds.base(), odds.practice(), odds.failing(), odds.book(), odds.aptitude(), odds.max()}) {
                buffer.writeFloat(part);
            }
            buffer.writeOptional(whyNot, (buf, reason) -> buf.writeEnum(reason));
        }
    }

    /**
     * Signing up for one licence at the desk.
     *
     * @param feeItem what the fee is paid in
     * @param feeCount how many
     */
    public record OfferView(ResourceLocation qualification, ResourceLocation feeItem, int feeCount,
                            List<CandidateView> candidates) {
        static OfferView read(FriendlyByteBuf buffer) {
            return new OfferView(buffer.readResourceLocation(), buffer.readResourceLocation(), buffer.readVarInt(),
                buffer.readList(CandidateView::read));
        }

        void write(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(qualification);
            buffer.writeResourceLocation(feeItem);
            buffer.writeVarInt(feeCount);
            buffer.writeCollection(candidates, (buf, candidate) -> candidate.write(buf));
        }
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
                                  List<OfferView> offers) implements MusicPayloads.Payload {
        public static DeskViewPayload read(FriendlyByteBuf buffer) {
            return new DeskViewPayload(buffer.readBlockPos(),
                buffer.readOptional(buf -> buf.readEnum(ExamEnrollment.Refusal.class)),
                buffer.readOptional(BookingView::read),
                buffer.readList(OfferView::read));
        }

        @Override
        public ResourceLocation id() {
            return EXAM_DESK_VIEW;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(desk);
            buffer.writeOptional(closed, (buf, refusal) -> buf.writeEnum(refusal));
            buffer.writeOptional(booking, (buf, view) -> view.write(buf));
            buffer.writeCollection(offers, (buf, offer) -> offer.write(buf));
        }
    }

    /**
     * A pet signed up at a desk. The fee is not in here: what it costs, and whether the pet
     * may go, is the server's business.
     *
     * @param desk which desk; the server checks the player is still standing at it
     */
    public record SignUpPayload(BlockPos desk, ResourceLocation qualification, UUID pet) implements MusicPayloads.Payload {
        public static SignUpPayload read(FriendlyByteBuf buffer) {
            return new SignUpPayload(buffer.readBlockPos(), buffer.readResourceLocation(), buffer.readUUID());
        }

        @Override
        public ResourceLocation id() {
            return EXAM_DESK_SIGN_UP;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(desk);
            buffer.writeResourceLocation(qualification);
            buffer.writeUUID(pet);
        }
    }
}
