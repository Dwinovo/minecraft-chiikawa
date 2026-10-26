package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.gifting.GiftOccasion;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A pet and its licences' exams, from the pet's side: the exam it is signed up to sit now,
 * the results it has to go and see, the once-a-second upkeep that has it let off an exam it
 * missed or whose desk is gone and told results it did not go to see, handing a paper
 * in, and hearing how it went. Signing up is {@link ExamEnrollment}'s business.
 *
 * <p>The upkeep only tells the pet's mind, as the pet's other surroundings do; what changes in the licence
 * when it is told, {@link #excuse} and {@link #grant}, is what the mind keeps the moment it is told, and
 * what the pet makes of it, {@link #tellCalledOff} and {@link #show}, is for its next tick.
 */
public final class PetExams {
    /** How often a pet looks at the clock. */
    public static final int UPKEEP_TICKS = 20;
    /** Why an exam was called off: the desk it was to be sat at is gone. */
    public static final String DESK_GONE = "desk_gone";
    /** Why an exam was called off: the pet did not sit it in time. */
    public static final String MISSED = "missed";

    private PetExams() {
    }

    /**
     * An exam a pet is signed up to sit now.
     *
     * @param desk the desk its owner signed it up at
     */
    public record Summons(ResourceLocation qualification, GlobalPos desk) {
    }

    /** The exam this pet is signed up for and can still sit, if any. */
    public static Optional<Summons> toSit(AbstractPet pet) {
        long dayTime = pet.level().getDayTime();
        for (ResourceLocation id : Qualifications.all().keySet()) {
            Licence licence = pet.licences().get(id);
            if (QualificationExam.isCalledNow(licence, dayTime)) {
                return licence.call().map(called -> new Summons(id, called.desk()));
            }
        }
        return Optional.empty();
    }

    /**
     * Whether the pet is in its chair at an exam desk, at the paper or handing it in: what its errand says,
     * and not how it happens to be drawn.
     */
    public static boolean seatedAtExam(AbstractPet pet) {
        return pet.getMind().errands().seated();
    }

    /** The desk this pet sat at and goes back to this morning to see how it did, if any. */
    public static Optional<GlobalPos> resultsDesk(AbstractPet pet) {
        long dayTime = pet.level().getDayTime();
        return Qualifications.all().keySet().stream()
            .map(id -> pet.licences().get(id))
            .filter(licence -> QualificationExam.goesToSeeResults(licence, dayTime))
            .flatMap(licence -> licence.paper().stream())
            .map(ExamStage.Sat::desk)
            .findFirst();
    }

    /**
     * Once a second: shows the owner's screen where the pet stands; tells the pet's mind of an exam it can
     * no longer sit, or whose desk has gone, and of results it has missed the morning to go and see, or that
     * it has no desk to go and see them at. Wild pets have nothing to do with exams.
     */
    public static void upkeep(AbstractPet pet) {
        if (pet.tickCount % UPKEEP_TICKS != 0 || !pet.isTame()) {
            return;
        }
        long dayTime = pet.level().getDayTime();
        Qualifications.all().forEach((id, qualification) -> {
            Licence licence = pet.licences().get(id);
            Optional<ExamStage.Called> call = licence.call();
            if (call.isPresent() && ExamDeskBlockEntity.isGone(pet.level(), call.get().desk())) {
                PetMind.post(pet, new Message.ExamCalledOff(id, DESK_GONE));
            } else if (QualificationExam.missedCall(licence, dayTime)) {
                PetMind.post(pet, new Message.ExamCalledOff(id, MISSED));
            }
            // What a call-off changed in the licence was kept as the pet was told, so this is the licence it left.
            Licence now = pet.licences().get(id);
            boolean deskGone = now.paper().filter(sat -> ExamDeskBlockEntity.isGone(pet.level(), sat.desk())).isPresent();
            if (QualificationExam.mustHearResults(now, dayTime)
                    || QualificationExam.resultsOut(now, dayTime) && deskGone) {
                PetMind.post(pet, new Message.ResultsDue(id));
            }
        });
        pet.showLicences(Qualifications.all().entrySet().stream()
            .map(entry -> LicenceView.of(entry.getKey(), entry.getValue(), pet.licences().get(entry.getKey()), dayTime))
            .toList());
    }

    /** Lets the pet off an exam it was signed up for and cannot sit: as if it had not been. */
    public static void excuse(AbstractPet pet, ResourceLocation id) {
        pet.licences().set(id, pet.licences().get(id).excused());
    }

    /** Tells the owner an exam is off, and why: {@link #DESK_GONE} or {@link #MISSED}. */
    public static void tellCalledOff(AbstractPet pet, ResourceLocation id, String reason) {
        pet.tellOwner(Component.translatable("message.chiikawa.exam." + reason, pet.getDisplayName(), name(id)));
    }

    /**
     * Hands the paper in: the result is decided now, kept with the pet until the morning,
     * and the desk keeps it to show then. The owner hears it is done.
     */
    public static void handIn(AbstractPet pet, ResourceLocation id, ExamDeskBlockEntity desk) {
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
        long dayTime = pet.level().getDayTime();
        Qualifications.all().keySet().stream()
            .filter(id -> QualificationExam.resultsOut(pet.licences().get(id), dayTime))
            .toList()
            .forEach(id -> grant(pet, id).ifPresent(result -> show(pet, id, result)));
    }

    /**
     * Hears one result, which is when the licence changes: up a grade, or one more fail to try again after. A pet that
     * passed may have a present in mind for its owner because of it.
     *
     * @return how it went, if there was a result to hear
     */
    public static Optional<ExamResult> grant(AbstractPet pet, ResourceLocation id) {
        Licence licence = pet.licences().get(id);
        Optional<ExamStage.Sat> paper = licence.paper();
        Optional<Qualification> qualification = Qualifications.get(id);
        if (paper.isEmpty() || qualification.isEmpty()) {
            return Optional.empty();
        }
        int rank = qualification.get().rank(licence.held() + 1);
        pet.licences().set(id, licence.announced());
        if (paper.get().passed()) {
            PetMind.post(pet, new Message.Occasion(GiftOccasion.PASSED_EXAM, Optional.empty()));
        }
        return Optional.of(new ExamResult(paper.get().passed(), rank));
    }

    /** The pet shows how an exam went and says so, and its owner reads it in chat. */
    public static void show(AbstractPet pet, ResourceLocation id, ExamResult result) {
        pet.triggerReaction(result.passed() ? PetReaction.HAPPY
            : PetPersonalities.of(pet.getType()).leaning(id).failReaction());
        PetSpeech.say(pet, result.passed() ? VoiceMoment.EXAM_PASS : VoiceMoment.EXAM_FAIL);
        pet.tellOwner(Component.translatable(result.passed() ? "message.chiikawa.exam.passed" : "message.chiikawa.exam.failed",
            pet.getDisplayName(), name(id), result.rank()));
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
