package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * A pet and its licences' exams, from the pet's side: the exam it is signed up to sit now,
 * the results it has to go and see, the once-a-second upkeep that lets it off an exam it
 * missed or whose desk is gone and tells it results it did not go to see, handing a paper
 * in, and hearing how it went. Signing up is {@link ExamEnrollment}'s business.
 */
public final class PetExams {
    /** How often a pet looks at the clock. */
    public static final int UPKEEP_TICKS = 20;

    private PetExams() {
    }

    /**
     * An exam a pet is signed up to sit now.
     *
     * @param desk the desk its owner signed it up at
     */
    public record Summons(Identifier qualification, GlobalPos desk) {
    }

    /** The exam this pet is signed up for and can still sit, if any. */
    public static Optional<Summons> toSit(AbstractPet pet) {
        long dayTime = pet.level().getOverworldClockTime();
        for (Identifier id : Qualifications.all().keySet()) {
            Licence licence = pet.licences().get(id);
            if (QualificationExam.isCalledNow(licence, dayTime)) {
                return licence.call().map(called -> new Summons(id, called.desk()));
            }
        }
        return Optional.empty();
    }

    /** Whether the pet is in its chair at an exam desk, at the paper or handing it in. */
    public static boolean seatedAtExam(AbstractPet pet) {
        return pet.getActivity() == PetActivity.EXAM;
    }

    /** The desk this pet sat at and goes back to this morning to see how it did, if any. */
    public static Optional<GlobalPos> resultsDesk(AbstractPet pet) {
        long dayTime = pet.level().getOverworldClockTime();
        return Qualifications.all().keySet().stream()
            .map(id -> pet.licences().get(id))
            .filter(licence -> QualificationExam.goesToSeeResults(licence, dayTime))
            .flatMap(licence -> licence.paper().stream())
            .map(ExamStage.Sat::desk)
            .findFirst();
    }

    /**
     * Once a second: shows the owner's screen where the pet stands; lets it off an exam it can
     * no longer sit, or whose desk has gone, and tells its owner so; and tells it results it
     * has missed the morning to go and see, or that it has no desk to go and see them at.
     * Wild pets have nothing to do with exams.
     */
    public static void upkeep(AbstractPet pet) {
        if (pet.tickCount % UPKEEP_TICKS != 0 || !pet.isTame()) {
            return;
        }
        long dayTime = pet.level().getOverworldClockTime();
        Qualifications.all().forEach((id, qualification) -> {
            Licence licence = pet.licences().get(id);
            Optional<ExamStage.Called> call = licence.call();
            if (call.isPresent() && ExamDeskBlockEntity.isGone(pet.level(), call.get().desk())) {
                pet.licences().set(id, licence.excused());
                pet.tellOwner(Component.translatable("message.chiikawa.exam.desk_gone", pet.getDisplayName(), name(id)));
            } else if (QualificationExam.missedCall(licence, dayTime)) {
                pet.licences().set(id, licence.excused());
                pet.tellOwner(Component.translatable("message.chiikawa.exam.missed", pet.getDisplayName(), name(id)));
            }
            Licence now = pet.licences().get(id);
            boolean deskGone = now.paper().filter(sat -> ExamDeskBlockEntity.isGone(pet.level(), sat.desk())).isPresent();
            if (QualificationExam.mustHearResults(now, dayTime)
                    || QualificationExam.resultsOut(now, dayTime) && deskGone) {
                hearResults(pet, id);
            }
        });
        pet.showLicences(Qualifications.all().entrySet().stream()
            .map(entry -> LicenceView.of(entry.getKey(), entry.getValue(), pet.licences().get(entry.getKey()), dayTime))
            .toList());
    }

    /**
     * Hands the paper in: the result is decided now, kept with the pet until the morning,
     * and the desk keeps it to show then. The owner hears it is done.
     */
    public static void handIn(AbstractPet pet, Identifier id, ExamDeskBlockEntity desk) {
        Qualifications.get(id).ifPresent(qualification -> {
            Licence licence = pet.licences().get(id);
            Personality.Leaning leaning = PetPersonalities.of(pet.getType()).leaning(id);
            boolean passed = QualificationExam.passes(qualification, licence, leaning, pet.getRandom());
            pet.licences().set(id, licence.sat(passed));
            desk.handIn(pet.getUUID(), passed);
            pet.tellOwner(Component.translatable("message.chiikawa.exam.handed_in", pet.getDisplayName(), name(id)));
        });
    }

    /** Hears every result that is out: at the desk in the morning, or wherever it is after. */
    public static void hearAllResults(AbstractPet pet) {
        long dayTime = pet.level().getOverworldClockTime();
        Qualifications.all().keySet().stream()
            .filter(id -> QualificationExam.resultsOut(pet.licences().get(id), dayTime))
            .toList()
            .forEach(id -> hearResults(pet, id));
    }

    /**
     * Hears one result: up a grade, or one more fail to try again after. The pet shows it and
     * says so, and its owner reads it in chat.
     */
    public static void hearResults(AbstractPet pet, Identifier id) {
        Licence licence = pet.licences().get(id);
        Optional<ExamStage.Sat> paper = licence.paper();
        Optional<Qualification> qualification = Qualifications.get(id);
        if (paper.isEmpty() || qualification.isEmpty()) {
            return;
        }
        boolean passed = paper.get().passed();
        int rank = qualification.get().rank(licence.held() + 1);
        pet.licences().set(id, licence.announced());
        pet.triggerReaction(passed ? PetReaction.HAPPY
            : PetPersonalities.of(pet.getType()).leaning(id).failReaction());
        PetSpeech.say(pet, passed ? VoiceMoment.EXAM_PASS : VoiceMoment.EXAM_FAIL);
        pet.tellOwner(Component.translatable(passed ? "message.chiikawa.exam.passed" : "message.chiikawa.exam.failed",
            pet.getDisplayName(), name(id), rank));
    }

    /** @return the licence this is the book for, if it is one */
    public static Optional<Identifier> bookOf(ItemStack stack) {
        return Qualifications.all().entrySet().stream()
            .filter(entry -> stack.is(entry.getValue().book()))
            .map(Map.Entry::getKey)
            .findFirst();
    }

    /**
     * Reads the licence's book for the next exam.
     *
     * @return whether it did: a pet that has read it already and not sat since, or holds
     *         every grade, has no use for another
     */
    public static boolean read(AbstractPet pet, Identifier id) {
        Licence licence = pet.licences().get(id);
        boolean useful = Qualifications.get(id)
            .map(qualification -> !licence.read() && licence.held() < qualification.grades())
            .orElse(false);
        if (useful) {
            pet.licences().set(id, licence.withBookRead());
        }
        return useful;
    }

    /** @return what the licence is called, as the player reads it */
    public static Component name(Identifier id) {
        return Component.translatable("qualification." + id.getNamespace() + "." + id.getPath());
    }
}
