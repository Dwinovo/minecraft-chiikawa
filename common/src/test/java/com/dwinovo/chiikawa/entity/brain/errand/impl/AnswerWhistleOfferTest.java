package com.dwinovo.chiikawa.entity.brain.errand.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Going over to see who whistled: when the pet sets off, and where the errand takes it. */
class AnswerWhistleOfferTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final GlobalPos BLOWN = GlobalPos.of(OVERWORLD, new BlockPos(6, 64, 0));
    private static final PetAnchor FREE = TestAnchors.free(HOME);

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void aWhistleThePetHasSetOffForIsOneOfferThatGoesToWhereItWasBlown() {
        TestView view = TestView.at(HOME, FREE).whistleHeard(BLOWN);

        assertEquals(1, TestErrands.offers(ErrandIds.ANSWER_WHISTLE, view).size());
        assertEquals(Optional.of(BLOWN), TestErrands.planOf(ErrandIds.ANSWER_WHISTLE, view).destination(view));
    }

    @Test
    void withoutOneThereIsNothingToGoOverToAndWhyIsSaid() {
        assertEquals("intent.chiikawa.fail.no_whistle",
            TestErrands.whyNoOffer(ErrandIds.ANSWER_WHISTLE, TestView.at(HOME, FREE)).reasonKey());
    }

    @Test
    void theCuriosityRunningOutIsTheStepsToNoticeAndNotTheErrandsToCutShort() {
        TestView view = TestView.at(HOME, FREE).whistleHeard(BLOWN);

        assertTrue(TestErrands.stillWanted(ErrandIds.ANSWER_WHISTLE, TestErrands.planOf(ErrandIds.ANSWER_WHISTLE, view),
            TestView.at(HOME, FREE)).ok());
    }
}
