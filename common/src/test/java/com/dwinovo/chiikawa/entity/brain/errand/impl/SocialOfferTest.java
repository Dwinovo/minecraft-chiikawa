package com.dwinovo.chiikawa.entity.brain.errand.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.ErrandSettingsData;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.social.SocialCooldowns;
import com.dwinovo.chiikawa.testing.TestScenes;
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

class SocialOfferTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos OWNER = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor AT_HEEL =
        TestAnchors.following(OWNER);

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void bothSidesOfASceneAreSocial() {
        assertEquals(ErrandCategory.SOCIAL, TestErrands.errand(ErrandIds.SOCIALIZE).category());
        assertEquals(ErrandCategory.SOCIAL, TestErrands.errand(ErrandIds.COOPERATE).category());
    }

    @Test
    void aPetGoesOverToThePartnerItHasThoughtOf() {
        TestView view = partnerAt(10);

        assertEquals(1, TestErrands.offers(ErrandIds.SOCIALIZE, view).size());
        assertEquals(Optional.of(GlobalPos.of(OVERWORLD, OWNER.pos().east(10))),
            TestErrands.planOf(ErrandIds.SOCIALIZE, view).destination(view));
    }

    @Test
    void thePartnerOfASceneStaysWhereItIs() {
        TestView view = TestView.at(OWNER, AT_HEEL).askedToPlay();

        assertEquals(Optional.empty(), TestErrands.planOf(ErrandIds.COOPERATE, view).destination(view));
    }

    @Test
    void withoutAnIdeaThereIsNobodyToGoTo() {
        assertEquals("intent.chiikawa.fail.no_partner",
            TestErrands.whyNoOffer(ErrandIds.SOCIALIZE, TestView.at(OWNER, AT_HEEL)).reasonKey());
    }

    @Test
    void aPetOnlyPlaysAlongWhenSomebodyIsComingOver() {
        assertFalse(TestErrands.offers(ErrandIds.COOPERATE, TestView.at(OWNER, AT_HEEL).askedToPlay()).isEmpty());
        assertEquals("intent.chiikawa.fail.not_asked",
            TestErrands.whyNoOffer(ErrandIds.COOPERATE, TestView.at(OWNER, AT_HEEL)).reasonKey());
    }

    @Test
    void aPetPlaysAlongOnlyWhileItIsStillWaitedFor() {
        TestView asked = TestView.at(OWNER, AT_HEEL).askedToPlay();
        Plan plan = TestErrands.planOf(ErrandIds.COOPERATE, asked);

        assertTrue(TestErrands.stillWanted(ErrandIds.COOPERATE, plan, asked).ok());
        assertEquals("intent.chiikawa.fail.not_asked",
            TestErrands.stillWanted(ErrandIds.COOPERATE, plan, TestView.at(OWNER, AT_HEEL)).reasonKey(),
            "the pet that asked has stopped waiting");
    }

    @Test
    void eachSideRestsOnTheSceneWithTheOtherPet() {
        TestView idea = partnerAt(10);
        TestView asked = TestView.at(OWNER, AT_HEEL).askedToPlay();

        assertEquals(Optional.of(SocialCooldowns.subject(TestScenes.SCENE, TestScenes.PARTNER)),
            TestErrands.planOf(ErrandIds.SOCIALIZE, idea).subject());
        assertEquals(Optional.of(SocialCooldowns.subject(TestScenes.SCENE, TestScenes.PARTNER)),
            TestErrands.planOf(ErrandIds.COOPERATE, asked).subject());
        assertEquals(Optional.of(TestScenes.SCENE), TestErrands.planOf(ErrandIds.SOCIALIZE, idea).restKey());
        assertEquals(Optional.of(TestScenes.SCENE), TestErrands.planOf(ErrandIds.COOPERATE, asked).restKey());
    }

    @Test
    void aSceneComesAfterAnyErrandButBeforePotteringAboutAndAnsweringComesFirst() {
        ErrandSettings<?> scene = ErrandSettingsData.all().get(ErrandIds.SOCIALIZE);
        ErrandSettings<?> answer = ErrandSettingsData.all().get(ErrandIds.COOPERATE);
        ErrandSettings<?> pickUp = ErrandSettingsData.all().get(ErrandIds.PICK_UP_ITEM);
        ErrandSettings<?> wander = ErrandSettingsData.all().get(ErrandIds.WANDER);

        assertEquals(pickUp.priority(), scene.priority());
        assertTrue(scene.score() < pickUp.score(), "picking up an item comes first");
        assertTrue(scene.priority() > wander.priority(), "wandering comes after");
        assertTrue(answer.priority() > scene.priority(), "a pet being visited answers rather than setting off itself");
    }

    private static TestView partnerAt(int blocksEast) {
        return TestView.at(OWNER, AT_HEEL).socialPartner(GlobalPos.of(OVERWORLD, OWNER.pos().east(blocksEast)));
    }
}
