package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The rules of a licence's exams, kept free of the pet so they can be tested on their own:
 * who may sit, who wants to, the odds, and what a wild pet turns up holding.
 */
public final class QualificationExam {
    private QualificationExam() {
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
