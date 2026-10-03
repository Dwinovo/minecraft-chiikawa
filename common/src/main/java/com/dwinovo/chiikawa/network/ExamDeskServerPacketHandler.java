package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.block.DeskBooking;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.qualification.ExamEnrollment;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

/**
 * An exam desk's screen: what it is shown, and the pets signed up from it. The screen shows a
 * fee and the server charges it, as a board's level is bought; the sign-up itself is
 * {@link ExamEnrollment}'s.
 */
public final class ExamDeskServerPacketHandler {
    /** How far from a desk an owner may stand and still sign a pet up there. */
    private static final double REACH_SQR = 64.0;

    private ExamDeskServerPacketHandler() {
    }

    /** Opens the desk's screen for {@code player}. */
    public static void show(ExamDeskBlockEntity desk, ServerPlayer player) {
        Services.NETWORK.sendToClient(player, view(desk, player));
    }

    /** @return the desk as its screen shows it to {@code player} */
    public static ExamDeskPayloads.DeskViewPayload view(ExamDeskBlockEntity desk, ServerPlayer player) {
        return new ExamDeskPayloads.DeskViewPayload(desk.getBlockPos(), ExamEnrollment.deskRefusal(desk),
            desk.booking().map(booking -> bookingView(booking, desk.getLevel().getDayTime())),
            ExamEnrollment.offers(player, desk).stream()
                .map(offer -> new ExamDeskPayloads.OfferView(offer.qualification(),
                    BuiltInRegistries.ITEM.getKey(offer.fee().item()), offer.fee().count(),
                    offer.candidates().stream()
                        .map(candidate -> new ExamDeskPayloads.CandidateView(candidate.pet().getUUID(),
                            candidate.pet().getDisplayName().getString(), candidate.heldRank(), candidate.nextRank(),
                            candidate.odds(), candidate.whyNot()))
                        .toList()))
                .toList());
    }

    public static void handleSignUp(ExamDeskPayloads.SignUpPayload payload, ServerPlayer player) {
        deskInReach(payload.desk(), player)
            .ifPresent(desk -> ExamEnrollment.signUp(player, desk, payload.qualification(), payload.pet()));
        // Signed up or not, the screen is sent back as the desk now stands, so an owner who
        // could not pay sees why rather than clicking at a button that does nothing.
        player.level().getBlockEntity(payload.desk(), InitBlockEntities.EXAM_DESK.get())
            .ifPresent(desk -> show(desk, player));
    }

    /** Who the desk has, and where they are with it: at the paper, handed in, or their results out. */
    private static ExamDeskPayloads.BookingView bookingView(DeskBooking booking, long dayTime) {
        ExamDeskPayloads.BookingView.Stage stage = switch (booking.sheet(dayTime)) {
            case ANSWER -> ExamDeskPayloads.BookingView.Stage.SITTING;
            case PASSED -> ExamDeskPayloads.BookingView.Stage.PASSED;
            case FAILED -> ExamDeskPayloads.BookingView.Stage.FAILED;
            case NONE -> ExamDeskPayloads.BookingView.Stage.HANDED_IN;
        };
        return new ExamDeskPayloads.BookingView(booking.name(), booking.qualification(), booking.rank(), stage);
    }

    /** @return the desk at {@code pos}, while the player is standing at it */
    private static Optional<ExamDeskBlockEntity> deskInReach(BlockPos pos, ServerPlayer player) {
        if (player.distanceToSqr(pos.getCenter()) > REACH_SQR) {
            return Optional.empty();
        }
        return player.level().getBlockEntity(pos, InitBlockEntities.EXAM_DESK.get());
    }
}
