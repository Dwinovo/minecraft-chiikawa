package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * The rules of a licence's exams, kept free of the pet so they can be tested on their own:
 * when a pet can be signed up and sit, who may, when results are out, the odds, and what a
 * wild pet turns up holding.
 */
public final class QualificationExam {
    /** An owner may sign a pet up from an hour after sunrise... */
    public static final long EXAM_FROM = 1000L;
    /** ...until late enough in the afternoon that the pet still has time to get to the desk and write. */
    public static final long LAST_CALL = 9000L;
    /** A pet signed up that has not sat down to the paper by an hour before sunset has missed it. */
    public static final long EXAM_UNTIL = 11000L;
    /** Results go up at sunrise the day after, and anyone who has not been to see them by noon hears them anyway. */
    public static final long RESULTS_UNTIL = 6000L;

    private QualificationExam() {
    }

    /** @return the day number of a time of day, as exams count days */
    public static long day(long dayTime) {
        return Math.floorDiv(dayTime, Level.TICKS_PER_DAY);
    }

    /** @return ticks since sunrise */
    public static long timeOfDay(long dayTime) {
        return Math.floorMod(dayTime, Level.TICKS_PER_DAY);
    }

    /** Whether an owner may sign a pet up now: in the working day, early enough to sit the exam. */
    public static boolean maySignUp(long dayTime) {
        long time = timeOfDay(dayTime);
        return time >= EXAM_FROM && time < LAST_CALL;
    }

    /**
     * Why the licence keeps a pet from being signed up: it holds every grade, has not
     * practised enough since its last exam, or has something on - signed up already, or
     * waiting to hear about the last one.
     *
     * @return empty when it may be signed up
     */
    public static Optional<Ineligible> whyNot(Qualification qualification, Licence licence) {
        if (licence.held() >= qualification.grades()) {
            return Optional.of(Ineligible.TOP_GRADE);
        }
        if (!(licence.exam() instanceof ExamStage.None)) {
            return Optional.of(Ineligible.BUSY);
        }
        if (licence.practice() < qualification.requiredPractice()) {
            return Optional.of(Ineligible.UNPRACTISED);
        }
        return Optional.empty();
    }

    /** Whether an exam for {@code examDay} can still be sat: it is that day, and the exam has not closed. */
    public static boolean canSit(long examDay, long dayTime) {
        return day(dayTime) == examDay && timeOfDay(dayTime) < EXAM_UNTIL;
    }

    /** Whether an exam sat on {@code examDay} has its results out: it was an earlier day than today. */
    public static boolean resultsOut(long examDay, long dayTime) {
        return day(dayTime) > examDay;
    }

    /** Whether it is the morning the results of an exam sat on {@code examDay} are up to go and see. */
    public static boolean isResultsMorning(long examDay, long dayTime) {
        return day(dayTime) == examDay + 1 && timeOfDay(dayTime) < RESULTS_UNTIL;
    }

    /** Whether a pet signed up for an exam can still sit it. */
    public static boolean isCalledNow(Licence licence, long dayTime) {
        return licence.call().filter(called -> canSit(called.day(), dayTime)).isPresent();
    }

    /** Whether a pet was signed up for an exam it can no longer sit. */
    public static boolean missedCall(Licence licence, long dayTime) {
        return licence.call().isPresent() && !isCalledNow(licence, dayTime);
    }

    /**
     * Whether a pet's result is out. The morning after, it goes to see; see
     * {@link #mustHearResults} for when it hears them anyway.
     */
    public static boolean resultsOut(Licence licence, long dayTime) {
        return licence.paper().filter(sat -> resultsOut(sat.day(), dayTime)).isPresent();
    }

    /** Whether a pet whose results are out goes to its desk to see them, rather than hearing them where it is. */
    public static boolean goesToSeeResults(Licence licence, long dayTime) {
        return licence.paper().filter(sat -> isResultsMorning(sat.day(), dayTime)).isPresent();
    }

    /** Whether a pet whose results are out has missed the morning, and hears them wherever it is. */
    public static boolean mustHearResults(Licence licence, long dayTime) {
        return resultsOut(licence, dayTime) && !goesToSeeResults(licence, dayTime);
    }

    /** A pet's odds at its next grade, piece by piece. */
    public static PassOdds odds(Qualification qualification, Licence licence, Personality.Leaning leaning) {
        return new PassOdds(
            qualification.basePass().get(Math.min(licence.held(), qualification.grades() - 1)),
            Math.min(licence.practice() * qualification.practicePerSlip(), qualification.practiceCap()),
            Math.min(licence.fails() * qualification.failBonus(), qualification.failCap()),
            licence.read() ? qualification.bookBonus() + leaning.bookBonus() : 0.0F,
            leaning.aptitude(),
            qualification.maxPass());
    }

    /** The chance of passing the next grade; see {@link PassOdds}. */
    public static float passChance(Qualification qualification, Licence licence, Personality.Leaning leaning) {
        return odds(qualification, licence, leaning).chance();
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
