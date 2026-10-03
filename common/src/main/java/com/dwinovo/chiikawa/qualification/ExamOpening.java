package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * An owner opening a licence's exam at a labor board: they pay the fee once, and every pet
 * of theirs that may sit it and can get to the board is called to sit it there today. The
 * board's screen shows beforehand who that would be, so nobody pays for an empty room.
 */
public final class ExamOpening {
    /**
     * How far around the board to look for the owner's pets. Only a pet with the board
     * within its reach is called, as only such a pet takes a slip from it; this only keeps
     * the search to the pets that might.
     */
    private static final double SEARCH = 48.0;

    private ExamOpening() {
    }

    /** Why an exam was not opened. */
    public enum Refusal {
        /** Too early or too late in the day to sit it. */
        CLOSED,
        /** None of the owner's pets would go. */
        NOBODY,
        /** The owner does not have the fee on them. */
        UNPAID
    }

    /**
     * What opening one licence's exam at a board would come to for an owner, now.
     *
     * @param open whether it may be opened at this time of day
     * @param going the owner's pets that would be called
     * @param called the owner's pets already called here today and not yet done
     */
    public record Offer(ResourceLocation qualification, ExamFee fee, boolean open, List<AbstractPet> going,
                        List<AbstractPet> called) {
    }

    /** What opening each licence's exam at this board would come to for this owner. */
    public static List<Offer> offers(ServerPlayer owner, LaborBoardBlockEntity board) {
        return Qualifications.all().entrySet().stream()
            .map(entry -> offer(owner, board, entry.getKey(), entry.getValue()))
            .toList();
    }

    /**
     * Opens the exam: takes the fee and calls the pets.
     *
     * @return why it was not opened, if it was not
     */
    public static Optional<Refusal> open(ServerPlayer owner, LaborBoardBlockEntity board, ResourceLocation id) {
        Optional<Qualification> found = Qualifications.get(id);
        if (found.isEmpty()) {
            return Optional.of(Refusal.NOBODY);
        }
        Offer offer = offer(owner, board, id, found.get());
        if (!offer.open()) {
            return Optional.of(Refusal.CLOSED);
        }
        if (offer.going().isEmpty()) {
            return Optional.of(Refusal.NOBODY);
        }
        if (!offer.fee().payFrom(owner.getInventory())) {
            return Optional.of(Refusal.UNPAID);
        }
        long today = QualificationExam.day(board.getLevel().getDayTime());
        GlobalPos where = GlobalPos.of(board.getLevel().dimension(), board.getBlockPos());
        for (AbstractPet pet : offer.going()) {
            pet.licences().set(id, pet.licences().get(id).called(where, today));
        }
        board.exam().opened(today);
        board.examChanged();
        owner.containerMenu.broadcastChanges();
        owner.level().playSound(null, board.getBlockPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
        owner.displayClientMessage(Component.translatable("message.chiikawa.exam.called",
            ComponentUtils.formatList(offer.going().stream().map(Entity::getDisplayName).toList(),
                Component.translatable("message.chiikawa.list_separator")),
            PetExams.name(id)), false);
        return Optional.empty();
    }

    private static Offer offer(ServerPlayer owner, LaborBoardBlockEntity board, ResourceLocation id,
                               Qualification qualification) {
        ServerLevel level = (ServerLevel) board.getLevel();
        GlobalPos where = GlobalPos.of(level.dimension(), board.getBlockPos());
        long dayTime = level.getDayTime();
        List<AbstractPet> pets = ownersPetsAround(level, where, owner.getUUID());
        List<AbstractPet> going = pets.stream()
            .filter(pet -> QualificationExam.maySit(qualification, pet.licences().get(id)))
            .filter(pet -> canGetTo(pet, where))
            .toList();
        List<AbstractPet> called = pets.stream()
            .filter(pet -> QualificationExam.isCalledNow(pet.licences().get(id), dayTime))
            .filter(pet -> pet.licences().get(id).call().filter(call -> call.board().equals(where)).isPresent())
            .toList();
        return new Offer(id, qualification.fee(), QualificationExam.mayOpen(dayTime), going, called);
    }

    private static List<AbstractPet> ownersPetsAround(ServerLevel level, GlobalPos board, UUID owner) {
        return level.getEntitiesOfClass(AbstractPet.class, new AABB(board.pos()).inflate(SEARCH),
            pet -> pet.isAlive() && PetOwnership.of(pet) instanceof PetOwnership.Owned owned
                && owned.ownerId().equals(owner));
    }

    /** Whether a pet would go to the board: one told to stay stays, and the rest go if it is within their reach. */
    private static boolean canGetTo(AbstractPet pet, GlobalPos board) {
        PetOwnership ownership = PetOwnership.of(pet);
        return PetConstraints.allows(pet, ownership, IntentCategory.EXAM)
            && PetConstraints.anchorOf(pet, ownership).withinReach(board);
    }
}
