package com.dwinovo.chiikawa.entity.brain.errand.decide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.LackKey;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.testing.FixedRandom;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestRests;
import com.dwinovo.chiikawa.testing.TestTargets;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Why a pet is not at work: what a decision finds when none of its job's work can start. */
class WhyNotTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor FREE = TestAnchors.free(HOME);
    private static final Set<ResourceLocation> WORK = Set.of(WorkData.HARVEST, WorkData.PLANT, WorkData.DELIVER);
    private static final List<ResourceLocation> FARM = List.of(ErrandIds.WANDER, WorkData.HARVEST, WorkData.PLANT, WorkData.DELIVER);
    private static final long NOW = 5000L;

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void aFarmerWithNoSeedsAndNothingToReapIsShortOfSeedsAndNotOfTheCrop() {
        WhyNot why = whyNot(TestView.at(HOME, FREE)).orElseThrow();

        assertEquals(WorkData.PLANT, why.errand());
        assertEquals("intent.chiikawa.fail.no_farmland", why.reasonKey());
        assertEquals(Optional.of(LackKey.tag(InitTag.ENTITY_PLANT_CROPS)), why.lack());
        assertEquals(NOW, why.since());
    }

    @Test
    void aFarmerWithSeedsButNothingToDoHasNoLackAndSaysTheFirstThingItFound() {
        WhyNot why = whyNot(TestView.at(HOME, FREE).carryingSeeds()).orElseThrow();

        assertEquals(WorkData.HARVEST, why.errand());
        assertEquals("intent.chiikawa.fail.no_crop", why.reasonKey());
        assertEquals(Optional.empty(), why.lack());
    }

    @Test
    void aFarmerWhoseWorkCanStartIsAtWorkAndHasNothingToSayForItself() {
        TestView ripe = TestView.at(HOME, FREE).targets(TestTargets.of(Optional.empty(), Optional.of(GlobalPos.of(OVERWORLD,
            new BlockPos(3, 64, 0))), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));

        assertEquals(Optional.empty(), whyNot(ripe));
    }

    @Test
    void anErrandThatIsNotWorkIsNotWhatThePetIsNotDoing() {
        // Nothing of the pet's work was offered at all, which is no account of why the pet is not at work.
        DecisionTrace trace = new DecisionTrace();
        Decision.decide(List.<ErrandType<?, ?>>of(TestErrands.errand(ErrandIds.PICK_UP_ITEM)), TestView.at(HOME, FREE),
            category -> true, TestRests.noRests(), Integer.MIN_VALUE, null, NOW, trace);

        assertEquals(Optional.empty(), WhyNot.of(trace, WORK::contains, NOW));
    }

    @Test
    void aWorkTheDirectiveForbidsIsNotAnAccountEither() {
        DecisionTrace trace = new DecisionTrace();
        Decision.decide(offered(FARM), TestView.at(HOME, FREE), category -> false, TestRests.noRests(), Integer.MIN_VALUE,
            FixedRandom.floats(), NOW, trace);

        assertEquals(Optional.empty(), WhyNot.of(trace, WORK::contains, NOW));
    }

    @Test
    void theSameMatterFoundAgainKeepsTheTimeItWasFirstFound() {
        WhyNot first = whyNot(TestView.at(HOME, FREE)).orElseThrow();
        WhyNot later = new WhyNot(first.errand(), first.reasonKey(), first.lack(), NOW + 700);

        assertSame(first, later.lastingFrom(Optional.of(first)));
        assertEquals(0, first.lasted(NOW));
        assertEquals(700, first.lasted(NOW + 700));
    }

    @Test
    void anotherMatterStartsTheCountAgain() {
        WhyNot lacking = whyNot(TestView.at(HOME, FREE)).orElseThrow();
        WhyNot seeking = new WhyNot(WorkData.HARVEST, "intent.chiikawa.fail.no_crop", Optional.empty(), NOW + 700);

        assertSame(seeking, seeking.lastingFrom(Optional.of(lacking)));
        assertSame(seeking, seeking.lastingFrom(Optional.empty()));
    }

    private static Optional<WhyNot> whyNot(TestView view) {
        DecisionTrace trace = new DecisionTrace();
        Decision.decide(offered(FARM), view, category -> true, TestRests.noRests(), Integer.MIN_VALUE, null, NOW, trace);
        return WhyNot.of(trace, WORK::contains, NOW);
    }

    private static List<ErrandType<?, ?>> offered(List<ResourceLocation> ids) {
        return ids.stream().<ErrandType<?, ?>>map(TestErrands::errand).toList();
    }
}
