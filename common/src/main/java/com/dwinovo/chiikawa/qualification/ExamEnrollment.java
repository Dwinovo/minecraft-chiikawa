package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.block.DeskBooking;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

/**
 * An owner signing one of their pets up for a licence exam at an exam desk, as a parent signs
 * a child up: they see each of their pets nearby with the grade it would sit and its odds, or
 * why it cannot go, pick one, and pay. The pet goes to the desk and sits it there today.
 */
public final class ExamEnrollment {
    /**
     * How far around the desk to look for the owner's pets. Only a pet with the desk within
     * its reach can go, as only such a pet takes a slip from a board; this only keeps the
     * search to the pets that might.
     */
    private static final double SEARCH = 48.0;

    private ExamEnrollment() {
    }

    /** Why a pet was not signed up. */
    public enum Refusal {
        /** Too early or too late in the day to sit an exam. */
        CLOSED,
        /** Another pet has the desk. */
        TAKEN,
        /** Something is in the way over the chair, where the pet would sit. */
        NO_SEAT,
        /** No licence by that name, or no such pet of the owner's nearby. */
        UNKNOWN,
        /** The pet cannot go; see {@link Candidate#whyNot}. */
        INELIGIBLE,
        /** The owner does not have the fee on them. */
        UNPAID
    }

    /**
     * One of the owner's pets, as the desk offers it for one licence.
     *
     * @param heldRank the grade it holds, as the player reads it; 0 for none
     * @param nextRank the grade it would sit
     * @param odds its odds at that grade
     * @param whyNot why it cannot be signed up, if it cannot
     */
    public record Candidate(AbstractPet pet, int heldRank, int nextRank, PassOdds odds, Optional<Ineligible> whyNot) {
    }

    /** What signing up for one licence at a desk comes to for an owner: the fee, and each of their pets nearby. */
    public record Offer(Identifier qualification, ExamFee fee, List<Candidate> candidates) {
    }

    /** Why the desk itself takes nobody now, if it does not: the time of day, another pet at it, or no room on the chair. */
    public static Optional<Refusal> deskRefusal(ExamDeskBlockEntity desk) {
        ServerLevel level = (ServerLevel) desk.getLevel();
        if (!QualificationExam.maySignUp(level.getOverworldClockTime())) {
            return Optional.of(Refusal.CLOSED);
        }
        if (!desk.isFree()) {
            return Optional.of(Refusal.TAKEN);
        }
        BlockPos overChair = desk.chair().above();
        if (!level.getBlockState(overChair).getCollisionShape(level, overChair).isEmpty()) {
            return Optional.of(Refusal.NO_SEAT);
        }
        return Optional.empty();
    }

    /** What signing up for each licence at this desk comes to for this owner. */
    public static List<Offer> offers(ServerPlayer owner, ExamDeskBlockEntity desk) {
        List<AbstractPet> pets = ownersPetsAround(desk, owner.getUUID());
        return Qualifications.all().entrySet().stream()
            .map(entry -> new Offer(entry.getKey(), entry.getValue().fee(), pets.stream()
                .map(pet -> candidate(pet, desk, entry.getKey(), entry.getValue()))
                .toList()))
            .toList();
    }

    /**
     * Signs the pet up: takes the fee, books the desk, and sends the pet to it.
     *
     * @return why it was not signed up, if it was not
     */
    public static Optional<Refusal> signUp(ServerPlayer owner, ExamDeskBlockEntity desk, Identifier id, UUID petId) {
        Optional<Refusal> closed = deskRefusal(desk);
        if (closed.isPresent()) {
            return closed;
        }
        Optional<Qualification> qualification = Qualifications.get(id);
        Optional<AbstractPet> found = ownersPetsAround(desk, owner.getUUID()).stream()
            .filter(pet -> pet.getUUID().equals(petId))
            .findFirst();
        if (qualification.isEmpty() || found.isEmpty()) {
            return Optional.of(Refusal.UNKNOWN);
        }
        AbstractPet pet = found.get();
        Candidate candidate = candidate(pet, desk, id, qualification.get());
        if (candidate.whyNot().isPresent()) {
            return Optional.of(Refusal.INELIGIBLE);
        }
        if (!qualification.get().fee().payFrom(owner.getInventory())) {
            return Optional.of(Refusal.UNPAID);
        }
        long today = QualificationExam.day(desk.getLevel().getOverworldClockTime());
        pet.licences().set(id, pet.licences().get(id).called(where(desk), today));
        desk.book(new DeskBooking(pet.getUUID(), pet.getDisplayName().getString(), id, candidate.nextRank(), today,
            Optional.empty()));
        owner.containerMenu.broadcastChanges();
        owner.level().playSound(null, desk.getBlockPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
        owner.sendSystemMessage(Component.translatable("message.chiikawa.exam.signed_up",
            pet.getDisplayName(), PetExams.name(id), candidate.nextRank()));
        return Optional.empty();
    }

    private static Candidate candidate(AbstractPet pet, ExamDeskBlockEntity desk, Identifier id,
                                       Qualification qualification) {
        Licence licence = pet.licences().get(id);
        int heldRank = licence.held() == 0 ? 0 : qualification.rank(licence.held());
        int nextRank = qualification.rank(Math.min(licence.held() + 1, qualification.grades()));
        PassOdds odds = QualificationExam.odds(qualification, licence, PetPersonalities.of(pet.getType()).leaning(id));
        Optional<Ineligible> whyNot = QualificationExam.whyNot(qualification, licence)
            .or(() -> whereItIs(pet, desk));
        return new Candidate(pet, heldRank, nextRank, odds, whyNot);
    }

    /** Whether the pet would go to the desk from where it is: one told to stay stays, and the rest go within their reach. */
    private static Optional<Ineligible> whereItIs(AbstractPet pet, ExamDeskBlockEntity desk) {
        PetOwnership ownership = PetOwnership.of(pet);
        if (!PetConstraints.allows(pet, ownership, IntentCategory.EXAM)) {
            return Optional.of(Ineligible.STAYING);
        }
        GlobalPos chair = GlobalPos.of(desk.getLevel().dimension(), desk.chair());
        return PetConstraints.anchorOf(pet, ownership).withinReach(chair) ? Optional.empty() : Optional.of(Ineligible.OUT_OF_REACH);
    }

    private static List<AbstractPet> ownersPetsAround(ExamDeskBlockEntity desk, UUID owner) {
        return desk.getLevel().getEntitiesOfClass(AbstractPet.class, new AABB(desk.getBlockPos()).inflate(SEARCH),
            pet -> pet.isAlive() && PetOwnership.of(pet) instanceof PetOwnership.Owned owned
                && owned.ownerId().equals(owner));
    }

    private static GlobalPos where(ExamDeskBlockEntity desk) {
        return GlobalPos.of(desk.getLevel().dimension(), desk.getBlockPos());
    }
}
