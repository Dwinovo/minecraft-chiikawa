package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.qualification.ExamOpening;
import com.dwinovo.chiikawa.qualification.QualificationExam;
import com.dwinovo.chiikawa.shop.Wallet;
import com.dwinovo.chiikawa.task.BoardLevels;
import com.dwinovo.chiikawa.task.PetTaskTypes;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * What a labor board's screen is shown and what its owner buys there: a level for the
 * board, or an exam for their pets. The screen shows a price and the server charges it: a
 * screen is a picture of what the board was a moment ago, and the money leaves the
 * player's pockets here.
 */
public final class BoardServerPacketHandler {
    /** How far from a board an owner may stand and still buy anything there. */
    private static final double REACH_SQR = 64.0;

    private BoardServerPacketHandler() {
    }

    /** @return the board as its screen shows it to {@code player} */
    public static BoardPayloads.BoardSlipsPayload view(BlockPos pos, LaborBoardBlockEntity board, ServerPlayer player) {
        BoardLevels levels = BoardLevels.current();
        int level = board.boardLevel();
        int price = levels.priceAfter(level);
        BoardPayloads.NextLevel next = price > 0
            ? new BoardPayloads.NextLevel(price, levels.slipsAt(level + 1), firstPutUpAt(level + 1))
            : BoardPayloads.NextLevel.NONE;
        return new BoardPayloads.BoardSlipsPayload(pos, level, levels.slipsAt(level), next, board.slipViews(),
            exams(board, player));
    }

    /** The exams this player could open here, and what the board has posted since the last one. */
    private static BoardPayloads.ExamView exams(LaborBoardBlockEntity board, ServerPlayer player) {
        List<BoardPayloads.ExamView.Offer> offers = ExamOpening.offers(player, board).stream()
            .map(offer -> new BoardPayloads.ExamView.Offer(offer.qualification(),
                BuiltInRegistries.ITEM.getKey(offer.fee().item()), offer.fee().count(), offer.open(),
                names(offer.going()), names(offer.called())))
            .toList();
        long today = QualificationExam.day(board.getLevel().getDayTime());
        List<BoardPayloads.ExamView.Posted> posted = board.exam().results().posted(today).stream()
            .map(sitting -> new BoardPayloads.ExamView.Posted(sitting.name(), sitting.qualification(), sitting.rank(),
                sitting.passed()))
            .toList();
        return new BoardPayloads.ExamView(offers, posted);
    }

    private static List<String> names(List<AbstractPet> pets) {
        return pets.stream().map(pet -> pet.getDisplayName().getString()).toList();
    }

    /** The kinds of work a board first puts up at this level: what buying it adds besides slips. */
    private static List<ResourceLocation> firstPutUpAt(int level) {
        return PetTaskTypes.all().entrySet().stream()
            .filter(entry -> entry.getValue().minLevel() == level)
            .map(Map.Entry::getKey)
            .toList();
    }

    public static void handleUpgrade(BoardPayloads.BoardUpgradePayload payload, ServerPlayer player) {
        buyLevel(payload.board(), player);
        showAgain(payload.board(), player);
    }

    public static void handleExam(BoardPayloads.BoardExamPayload payload, ServerPlayer player) {
        boardInReach(payload.board(), player)
            .ifPresent(board -> ExamOpening.open(player, board, payload.qualification()));
        showAgain(payload.board(), player);
    }

    /**
     * Sold or not, the screen is sent back as the board now stands, so an owner who could not
     * pay sees why rather than clicking at a button that does nothing.
     */
    private static void showAgain(BlockPos pos, ServerPlayer player) {
        player.level().getBlockEntity(pos, InitBlockEntities.LABOR_BOARD.get())
            .ifPresent(board -> Services.NETWORK.sendToClient(player, view(pos, board, player)));
    }

    /** @return the board at {@code pos}, while the player is standing at it */
    private static Optional<LaborBoardBlockEntity> boardInReach(BlockPos pos, ServerPlayer player) {
        if (player.distanceToSqr(pos.getCenter()) > REACH_SQR) {
            return Optional.empty();
        }
        return player.level().getBlockEntity(pos, InitBlockEntities.LABOR_BOARD.get());
    }

    /**
     * Sells a board its next level, if the owner is standing at it and has the money.
     *
     * @return whether the board went up a level
     */
    public static boolean buyLevel(BlockPos pos, ServerPlayer player) {
        Optional<LaborBoardBlockEntity> found = boardInReach(pos, player);
        if (found.isEmpty()) {
            return false;
        }
        LaborBoardBlockEntity board = found.get();
        int price = BoardLevels.current().priceAfter(board.boardLevel());
        // A board at its top level has nothing to sell, and a price is paid in full or not
        // at all: nobody leaves half the money on the counter for half a level.
        if (price <= 0 || !Wallet.pay(player.getInventory(), price)) {
            return false;
        }
        board.upgrade();
        player.level().playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.4F);
        player.containerMenu.broadcastChanges();
        return true;
    }
}
