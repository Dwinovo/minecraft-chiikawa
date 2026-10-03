package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.block.ExamResults;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A pet and its licences' exams, from the pet's side: the exam it has been called to sit
 * now, the results it has to go and see, the once-a-second upkeep that lets it off an exam
 * it missed and tells it results it did not go to see, handing a paper in, and hearing how
 * it went. Being called is {@link ExamOpening}'s business.
 */
public final class PetExams {
    /** How often a pet looks at the clock. */
    public static final int UPKEEP_TICKS = 20;

    private PetExams() {
    }

    /**
     * An exam a pet has been called to sit now.
     *
     * @param board where its owner opened it
     */
    public record Summons(ResourceLocation qualification, GlobalPos board) {
    }

    /** The exam this pet has been called to and can still sit, if any. */
    public static Optional<Summons> toSit(AbstractPet pet) {
        long dayTime = pet.level().getDayTime();
        for (ResourceLocation id : Qualifications.all().keySet()) {
            Licence licence = pet.licences().get(id);
            if (QualificationExam.isCalledNow(licence, dayTime)) {
                return licence.call().map(called -> new Summons(id, called.board()));
            }
        }
        return Optional.empty();
    }

    /** The board this pet sat at and goes to this morning to see how it did, if any. */
    public static Optional<GlobalPos> resultsBoard(AbstractPet pet) {
        long dayTime = pet.level().getDayTime();
        return Qualifications.all().keySet().stream()
            .map(id -> pet.licences().get(id))
            .filter(licence -> QualificationExam.goesToSeeResults(licence, dayTime))
            .flatMap(licence -> licence.paper().stream())
            .map(ExamStage.Sat::board)
            .findFirst();
    }

    /**
     * Once a second: shows the owner's screen where the pet stands; lets it off an exam it
     * was called to and can no longer sit, and tells its owner so; and tells it results it
     * has missed the morning to go and see. Wild pets have nothing to do with exams.
     */
    public static void upkeep(AbstractPet pet) {
        if (pet.tickCount % UPKEEP_TICKS != 0 || !pet.isTame()) {
            return;
        }
        long dayTime = pet.level().getDayTime();
        Qualifications.all().forEach((id, qualification) -> {
            Licence licence = pet.licences().get(id);
            if (QualificationExam.missedCall(licence, dayTime)) {
                pet.licences().set(id, licence.excused());
                pet.tellOwner(Component.translatable("message.chiikawa.exam.missed", pet.getDisplayName(), name(id)));
            }
            if (QualificationExam.mustHearResults(pet.licences().get(id), dayTime)) {
                hearResults(pet, id);
            }
        });
        pet.showLicences(Qualifications.all().entrySet().stream()
            .map(entry -> LicenceView.of(entry.getKey(), entry.getValue(), pet.licences().get(entry.getKey()), dayTime))
            .toList());
    }

    /**
     * Hands the paper in: the result is decided now, kept with the pet until the morning,
     * and the board keeps it to post then. The owner hears it is done.
     */
    public static void handIn(AbstractPet pet, ResourceLocation id, LaborBoardBlockEntity board) {
        Qualifications.get(id).ifPresent(qualification -> {
            Licence licence = pet.licences().get(id);
            Personality.Leaning leaning = PetPersonalities.of(pet.getType()).leaning(id);
            boolean passed = QualificationExam.passes(qualification, licence, leaning, pet.getRandom());
            long today = QualificationExam.day(pet.level().getDayTime());
            pet.licences().set(id, licence.sat(passed));
            ExamResults results = board.exam().results();
            results.forgetBefore(today);
            results.record(new ExamResults.Sitting(pet.getUUID(), pet.getDisplayName().getString(), id,
                qualification.rank(licence.held() + 1), passed, today));
            board.exam().seats().leave(pet.getUUID());
            board.examChanged();
            pet.tellOwner(Component.translatable("message.chiikawa.exam.handed_in", pet.getDisplayName(), name(id)));
        });
    }

    /** Hears every result that is out: at the board in the morning, or wherever it is after. */
    public static void hearAllResults(AbstractPet pet) {
        long dayTime = pet.level().getDayTime();
        Qualifications.all().keySet().stream()
            .filter(id -> QualificationExam.resultsOut(pet.licences().get(id), dayTime))
            .toList()
            .forEach(id -> hearResults(pet, id));
    }

    /**
     * Hears one result: up a grade, or one more fail to try again after. The pet shows it and
     * says so, and its owner reads it in chat.
     */
    public static void hearResults(AbstractPet pet, ResourceLocation id) {
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
    public static Optional<ResourceLocation> bookOf(ItemStack stack) {
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
    public static boolean read(AbstractPet pet, ResourceLocation id) {
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
    public static Component name(ResourceLocation id) {
        return Component.translatable("qualification." + id.getNamespace() + "." + id.getPath());
    }
}
