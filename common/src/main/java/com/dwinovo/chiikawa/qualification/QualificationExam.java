package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * The rules of a licence's exams, kept free of the pet so they can be tested on their own:
 * who may sit, who wants to, the odds, and what a wild pet turns up holding.
 */
public final class QualificationExam {
    /** Exams are sat in the working day: from an hour after sunrise until an hour before sunset. */
    public static final long EXAM_FROM = 1000L;
    public static final long EXAM_UNTIL = 11000L;
    /** Results go up at sunrise the day after, and anyone who has not been to see them by noon hears them anyway. */
    public static final long RESULTS_UNTIL = 6000L;
    /** Owners hear about tomorrow's exam at sunset the day before. */
    public static final long EVE_REMINDER_AT = 12000L;

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

    /** Whether an exam can be sat now: an exam day, in working hours. */
    public static boolean isExamTime(Qualification qualification, long dayTime) {
        long time = timeOfDay(dayTime);
        return isExamDay(qualification, day(dayTime)) && time >= EXAM_FROM && time < EXAM_UNTIL;
    }

    /** Days until an exam can next be sat: today while today's is still on, otherwise the next exam day. */
    public static int daysToExam(Qualification qualification, long dayTime) {
        long today = day(dayTime);
        boolean todaysIsOver = timeOfDay(dayTime) >= EXAM_UNTIL;
        for (int days = 0; days <= qualification.everyDays(); days++) {
            if (isExamDay(qualification, today + days) && !(days == 0 && todaysIsOver)) {
                return days;
            }
        }
        return qualification.everyDays();
    }

    /** Whether tomorrow is an exam day. */
    public static boolean isExamEve(Qualification qualification, long dayTime) {
        return isExamDay(qualification, day(dayTime) + 1);
    }

    /**
     * Whether a pet's result is out: it sat an exam on an earlier day than today. The morning
     * after, it goes to see; see {@link #mustHearResults} for when it hears them anyway.
     */
    public static boolean resultsOut(Licence licence, long dayTime) {
        return licence.pending().isPresent() && day(dayTime) > licence.decidedDay();
    }

    /** Whether a pet whose results are out goes to the board to see them, rather than hearing them where it is. */
    public static boolean goesToSeeResults(Licence licence, long dayTime) {
        return resultsOut(licence, dayTime) && day(dayTime) == licence.decidedDay() + 1
            && timeOfDay(dayTime) < RESULTS_UNTIL;
    }

    /** Whether a pet whose results are out has missed the morning, and hears them wherever it is. */
    public static boolean mustHearResults(Licence licence, long dayTime) {
        return resultsOut(licence, dayTime) && !goesToSeeResults(licence, dayTime);
    }

    /**
     * Whether {@code day} is an exam day: every {@link Qualification#everyDays()} days, the
     * same days everywhere, the first one that many days in.
     *
     * @param day the day number, counted in whole days of time of day
     */
    public static boolean isExamDay(Qualification qualification, long day) {
        return day > 0 && (day + 1) % qualification.everyDays() == 0;
    }

    /**
     * Whether a pet may sit the next exam at all: it has a grade left to pass, has practised
     * enough since the last one, and is not still waiting to hear about the last one.
     */
    public static boolean maySit(Qualification qualification, Licence licence) {
        return licence.held() < qualification.grades()
            && licence.practice() >= qualification.requiredPractice()
            && licence.pending().isEmpty();
    }

    /**
     * Whether a pet wants to sit today's exam: a pet that has read the book always does;
     * otherwise as eager as its personality makes it.
     */
    public static boolean wantsToSit(Personality.Leaning leaning, Licence licence, RandomSource random) {
        return licence.read() || random.nextFloat() < leaning.eagerness();
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
