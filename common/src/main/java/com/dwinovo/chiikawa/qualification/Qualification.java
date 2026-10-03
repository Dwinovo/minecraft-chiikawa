package com.dwinovo.chiikawa.qualification;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.item.Item;

/**
 * A licence pets sit exams for, loaded from {@code data/<namespace>/pet_qualification/<id>.json}
 * by {@link QualificationLoader}: how many grades it has, what an owner pays to open its exam,
 * what counts as practice for it, and the odds.
 *
 * <p>Grades are counted up from nothing, as levels of anything are: a pet holds 0 until it
 * passes its first exam, and the top is {@link #grades()}. The series counts them the other
 * way - grade 5 first, grade 1 the best - and so do the words the player reads; see
 * {@link #rank}.
 *
 * @param grades how many grades there are
 * @param fee what an owner pays at a board to open the exam there
 * @param practiceTask the slip type whose completion counts as practice
 * @param requiredPractice slips finished since the last exam before a pet may sit the next
 * @param basePass the chance of passing the exam for each grade, the first grade first
 * @param practicePerSlip what each slip finished since the last exam adds
 * @param practiceCap the most practice adds
 * @param failBonus what each exam failed since the last pass adds
 * @param failCap the most failing adds
 * @param book the book a pet reads to prepare
 * @param bookBonus what having read it adds
 * @param maxPass the best odds anyone gets
 * @param writeTicks how long a pet takes over the paper
 * @param wildGrades how likely a wild pet is to turn up holding each grade, from none up
 */
public record Qualification(
    int grades,
    ExamFee fee,
    ResourceLocation practiceTask,
    int requiredPractice,
    List<Float> basePass,
    float practicePerSlip,
    float practiceCap,
    float failBonus,
    float failCap,
    Item book,
    float bookBonus,
    float maxPass,
    InclusiveRange<Integer> writeTicks,
    List<Integer> wildGrades
) {
    private static final Codec<Float> CHANCE = Codec.floatRange(0.0F, 1.0F);

    /** Lazy because the item registry only exists once the game has bootstrapped. */
    public static final Codec<Qualification> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.<Qualification>create(
        instance -> instance.group(
            Codec.intRange(1, 10).fieldOf("grades").forGetter(Qualification::grades),
            ExamFee.CODEC.fieldOf("fee").forGetter(Qualification::fee),
            ResourceLocation.CODEC.fieldOf("practice_task").forGetter(Qualification::practiceTask),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("required_practice").forGetter(Qualification::requiredPractice),
            CHANCE.listOf().fieldOf("base_pass").forGetter(Qualification::basePass),
            CHANCE.fieldOf("practice_per_slip").forGetter(Qualification::practicePerSlip),
            CHANCE.fieldOf("practice_cap").forGetter(Qualification::practiceCap),
            CHANCE.fieldOf("fail_bonus").forGetter(Qualification::failBonus),
            CHANCE.fieldOf("fail_cap").forGetter(Qualification::failCap),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("book").forGetter(Qualification::book),
            CHANCE.fieldOf("book_bonus").forGetter(Qualification::bookBonus),
            CHANCE.fieldOf("max_pass").forGetter(Qualification::maxPass),
            InclusiveRange.codec(Codec.INT, 20, 24000).fieldOf("write_ticks").forGetter(Qualification::writeTicks),
            ExtraCodecs.NON_NEGATIVE_INT.listOf().fieldOf("wild_grades").forGetter(Qualification::wildGrades)
        ).apply(instance, Qualification::new)).validate(Qualification::validate));

    public Qualification {
        basePass = List.copyOf(basePass);
        wildGrades = List.copyOf(wildGrades);
    }

    private static DataResult<Qualification> validate(Qualification qualification) {
        if (qualification.basePass().size() != qualification.grades()) {
            return DataResult.error(() -> "base_pass has " + qualification.basePass().size()
                + " chances for " + qualification.grades() + " grades");
        }
        if (qualification.wildGrades().size() != qualification.grades() + 1) {
            return DataResult.error(() -> "wild_grades has " + qualification.wildGrades().size()
                + " weights for no grade and " + qualification.grades() + " grades");
        }
        if (qualification.wildGrades().stream().mapToInt(Integer::intValue).sum() == 0) {
            return DataResult.error(() -> "wild_grades weighs every grade at nothing");
        }
        return DataResult.success(qualification);
    }

    /**
     * What the series calls a grade: the first grade a pet passes is the lowest-ranked and
     * numbered highest, grade 5 of 5; the top grade is grade 1.
     *
     * @param held grades passed, at least 1
     * @return the grade's number as the player reads it
     */
    public int rank(int held) {
        return grades - held + 1;
    }
}
