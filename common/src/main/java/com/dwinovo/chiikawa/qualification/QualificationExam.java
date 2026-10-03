package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * The rules of a licence's exams, kept free of the pet so they can be tested on their own:
 * when one can be opened and sat, who may sit, when results are out, the odds, and what a
 * wild pet turns up holding.
 */
public final class QualificationExam {
    /** An owner may open an exam from an hour after sunrise... */
    public static final long EXAM_FROM = 1000L;
    /** ...until late enough in the afternoon that the pets called still have time to get there and write. */
    public static final long LAST_CALL = 9000L;
    /** A pet called that has not sat down to the paper by an hour before sunset has missed it. */
    public static final long EXAM_UNTIL = 11000L;
    /** Results go up at sunrise the day after, and anyone who has not been to see them by noon hears them anyway. */
    public static final long RESULTS_UNTIL = 6000L;

    private QualificationExam() {
    }

    /** @return the day number of a time of day, as boards and exams count days */
    public static long day(long dayTime) {
        return Math.floorDiv(dayTime, Level.TICKS_PER_DAY);
    }

    /** @return ticks since sunrise */
    public static long timeOfDay(long dayTime) {
        return Math.floorMod(dayTime, Level.TICKS_PER_DAY);
    }

    /** Whether an owner may open an exam now: in the working day, early enough to sit it. */
    public static boolean mayOpen(long dayTime) {
        long time = timeOfDay(dayTime);
        return time >= EXAM_FROM && time < LAST_CALL;
    }

    /**
     * Whether a pet may be called to an exam: it has a grade left to pass, has practised
     * enough since the last one, and has nothing on - neither called already nor waiting
     * to hear about the last one.
     */
    public static boolean maySit(Qualification qualification, Licence licence) {
        return licence.held() < qualification.grades()
            && licence.practice() >= qualification.requiredPractice()
            && licence.exam() instanceof ExamStage.None;
    }

    /** Whether a pet called to an exam can still sit it: the day it was called, before the exam closes. */
    public static boolean isCalledNow(Licence licence, long dayTime) {
        return licence.call()
            .filter(called -> called.day() == day(dayTime) && timeOfDay(dayTime) < EXAM_UNTIL)
            .isPresent();
    }

    /** Whether a pet was called to an exam it can no longer sit. */
    public static boolean missedCall(Licence licence, long dayTime) {
        return licence.call().isPresent() && !isCalledNow(licence, dayTime);
    }

    /**
     * Whether a pet's result is out: it sat an exam on an earlier day than today. The morning
     * after, it goes to see; see {@link #mustHearResults} for when it hears them anyway.
     */
    public static boolean resultsOut(Licence licence, long dayTime) {
        return licence.paper().filter(sat -> day(dayTime) > sat.day()).isPresent();
    }

    /** Whether a pet whose results are out goes to the board to see them, rather than hearing them where it is. */
    public static boolean goesToSeeResults(Licence licence, long dayTime) {
        return resultsOut(licence, dayTime)
            && licence.paper().filter(sat -> day(dayTime) == sat.day() + 1).isPresent()
            && timeOfDay(dayTime) < RESULTS_UNTIL;
    }

    /** Whether a pet whose results are out has missed the morning, and hears them wherever it is. */
    public static boolean mustHearResults(Licence licence, long dayTime) {
        return resultsOut(licence, dayTime) && !goesToSeeResults(licence, dayTime);
    }

    /**
     * The chance of passing the next grade: the grade's own odds, plus what practice,
     * failing and the book add, all scaled by how the pet leans, and never above the best
     * odds the licence allows.
     */
    public static float passChance(Qualification qualification, Licence licence, Personality.Leaning leaning) {
        float base = qualification.basePass().get(Math.min(licence.held(), qualification.grades() - 1));
        float practice = Math.min(licence.practice() * qualification.practicePerSlip(), qualification.practiceCap());
        float failing = Math.min(licence.fails() * qualification.failBonus(), qualification.failCap());
        float book = licence.read() ? qualification.bookBonus() + leaning.bookBonus() : 0.0F;
        return Mth.clamp((base + practice + failing + book) * leaning.aptitude(), 0.0F, qualification.maxPass());
    }

    /** Whether this sitting passes. */
    public static boolean passes(Qualification qualification, Licence licence, Personality.Leaning leaning,
                                 RandomSource random) {
        return random.nextFloat() < passChance(qualification, licence, leaning);
    }

    /** How long this pet takes over the paper, in ticks. */
    public static int writeTicks(Qualification qualification, RandomSource random) {
        return Mth.randomBetweenInclusive(random, qualification.writeTicks().minInclusive(),
            qualification.writeTicks().maxInclusive());
    }

    /**
     * The grades a wild pet turns up holding, drawn by the licence's weights. A pet that
     * takes to the licence turns up holding one more often: the weight of every grade but
     * none is multiplied by its aptitude.
     */
    public static int wildGrade(Qualification qualification, Personality.Leaning leaning, RandomSource random) {
        List<Integer> weights = qualification.wildGrades();
        float[] scaled = new float[weights.size()];
        float total = 0.0F;
        for (int grade = 0; grade < weights.size(); grade++) {
            scaled[grade] = weights.get(grade) * (grade == 0 ? 1.0F : leaning.aptitude());
            total += scaled[grade];
        }
        float pick = random.nextFloat() * total;
        for (int grade = 0; grade < scaled.length; grade++) {
            pick -= scaled[grade];
            if (pick < 0.0F) {
                return grade;
            }
        }
        return 0;
    }
}
