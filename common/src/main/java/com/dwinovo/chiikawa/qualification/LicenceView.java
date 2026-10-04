package com.dwinovo.chiikawa.qualification;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * What a pet's screen shows about one of its licences, worked out on the server, where the
 * licences are, and sent to the owner's game with the pet: the game there has none.
 *
 * @param qualification the licence's id
 * @param book the licence's book, the picture the screen shows it by
 * @param rank the grade held, as the player reads it; 0 for none
 * @param topRank the best grade there is, 1 as the series counts
 * @param practised whether the pet has done the practice to sit it
 * @param called whether it has been called to sit the exam today
 * @param awaitingResults whether it sat the last exam and has not heard yet
 */
public record LicenceView(Identifier qualification, Identifier book, int rank, int topRank,
                          boolean practised, boolean called, boolean awaitingResults) {
    public static final Codec<LicenceView> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("qualification").forGetter(LicenceView::qualification),
        Identifier.CODEC.fieldOf("book").forGetter(LicenceView::book),
        Codec.INT.fieldOf("rank").forGetter(LicenceView::rank),
        Codec.INT.fieldOf("top_rank").forGetter(LicenceView::topRank),
        Codec.BOOL.fieldOf("practised").forGetter(LicenceView::practised),
        Codec.BOOL.fieldOf("called").forGetter(LicenceView::called),
        Codec.BOOL.fieldOf("awaiting_results").forGetter(LicenceView::awaitingResults)
    ).apply(instance, LicenceView::new));
    public static final Codec<List<LicenceView>> LIST_CODEC = CODEC.listOf();

    /**
     * @param dayTime the pet's level's time of day, in ticks
     */
    public static LicenceView of(Identifier id, Qualification qualification, Licence licence, long dayTime) {
        return new LicenceView(id, BuiltInRegistries.ITEM.getKey(qualification.book()),
            licence.held() == 0 ? 0 : qualification.rank(licence.held()), qualification.rank(qualification.grades()),
            licence.practice() >= qualification.requiredPractice(), QualificationExam.isCalledNow(licence, dayTime),
            licence.paper().isPresent());
    }

    /** Whether the pet holds every grade there is. */
    public boolean top() {
        return rank == topRank;
    }
}
