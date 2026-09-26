package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.block.BoardExam;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A pet and its licences' exams: what it means to sit now, whether it has results to go
 * and see, the once-a-second upkeep that makes up its mind on exam day and tells it its
 * results when it misses the morning, sitting an exam, and hearing how it went.
 */
public final class PetExams {
    /** How often a pet looks at the calendar. */
    public static final int UPKEEP_TICKS = 20;

    private PetExams() {
    }

    /**
     * The licence this pet means to sit now: an exam day, in working hours, one it may sit
     * and has made up its mind to.
     */
    public static Optional<ResourceLocation> toSit(AbstractPet pet) {
        long dayTime = pet.level().getDayTime();
        long today = QualificationExam.day(dayTime);
        for (Map.Entry<ResourceLocation, Qualification> entry : Qualifications.all().entrySet()) {
            Licence licence = pet.licences().get(entry.getKey());
            if (QualificationExam.isExamTime(entry.getValue(), dayTime)
                    && QualificationExam.maySit(entry.getValue(), licence)
                    && licence.decidedDay() == today && licence.wantsToSit()) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /** Whether this pet has results out that it goes to a board to see this morning. */
    public static boolean goesToSeeResults(AbstractPet pet) {
        long dayTime = pet.level().getDayTime();
        return Qualifications.all().keySet().stream()
            .anyMatch(id -> QualificationExam.goesToSeeResults(pet.licences().get(id), dayTime));
    }

    /**
     * Once a second: on exam day a pet that may sit makes up its mind, once, whether it
     * wants to; a pet whose results are out and that has missed the morning hears them
     * where it is. Wild pets have nothing to do with exams.
     */
    public static void upkeep(AbstractPet pet) {
        if (pet.tickCount % UPKEEP_TICKS != 0 || !pet.isTame()) {
            return;
        }
        long now = pet.level().getDayTime();
        pet.showLicences(Qualifications.all().entrySet().stream()
            .map(entry -> LicenceView.of(entry.getKey(), entry.getValue(), pet.licences().get(entry.getKey()), now))
            .toList());
        long dayTime = pet.level().getDayTime();
        long today = QualificationExam.day(dayTime);
        Personality personality = PetPersonalities.of(pet.getType());
        Qualifications.all().forEach((id, qualification) -> {
            Licence licence = pet.licences().get(id);
            if (QualificationExam.isExamDay(qualification, today) && licence.decidedDay() != today
                    && QualificationExam.maySit(qualification, licence)) {
                pet.licences().set(id, licence.decided(today,
                    QualificationExam.wantsToSit(personality.leaning(id), licence, pet.getRandom())));
            }
            if (QualificationExam.mustHearResults(pet.licences().get(id), dayTime)) {
                hearResults(pet, id);
            }
        });
    }

    /**
     * Hands the paper in: the result is decided now, kept with the pet until the morning,
     * and the board keeps it to post then.
     */
    public static void handIn(AbstractPet pet, ResourceLocation id, LaborBoardBlockEntity board) {
        Qualifications.get(id).ifPresent(qualification -> {
            Licence licence = pet.licences().get(id);
            Personality.Leaning leaning = PetPersonalities.of(pet.getType()).leaning(id);
            boolean passed = QualificationExam.passes(qualification, licence, leaning, pet.getRandom());
            long today = QualificationExam.day(pet.level().getDayTime());
            pet.licences().set(id, licence.sat(passed));
            BoardExam exam = board.exam();
            exam.forgetBefore(today);
            exam.record(new BoardExam.Sitting(pet.getUUID(), pet.getDisplayName().getString(), id,
                qualification.rank(licence.held() + 1), passed, today));
            exam.leave(pet.getUUID());
            board.examChanged();
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
        Optional<Qualification> qualification = Qualifications.get(id);
        if (licence.pending().isEmpty() || qualification.isEmpty()) {
            return;
        }
        boolean passed = licence.pending().get();
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
