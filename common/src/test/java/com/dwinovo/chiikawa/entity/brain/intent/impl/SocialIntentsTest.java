package com.dwinovo.chiikawa.entity.brain.intent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.AnchorDistances;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.testing.TestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class SocialIntentsTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("dimension")),
        Identifier.withDefaultNamespace("overworld"));
    private static final GlobalPos OWNER = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor AT_HEEL =
        new PetAnchor(OWNER, AnchorDistances.FOLLOW_REACH, AnchorDistances.FOLLOW_LEASH, true, true);

    private final SocializeIntent socialize = new SocializeIntent();
    private final CooperateIntent cooperate = new CooperateIntent();

    @Test
    void bothSidesOfASceneAreSocial() {
        assertEquals(IntentCategory.SOCIAL, socialize.category());
        assertEquals(IntentCategory.SOCIAL, cooperate.category());
    }

    @Test
    void aPetAtHeelOnlyGoesOverToAPartnerAroundItsOwnersFeet() {
        assertTrue(socialize.canRun(partnerAt(10).build()).ok());
        assertEquals("intent.chiikawa.fail.out_of_reach", socialize.canRun(partnerAt(14).build()).reasonKey());
        assertTrue(socialize.canContinue(partnerAt(14).build()).ok(), "a partner that moved a little is still there");
        assertEquals("intent.chiikawa.fail.out_of_leash", socialize.canContinue(partnerAt(20).build()).reasonKey());
    }

    @Test
    void withoutAnIdeaThereIsNobodyToGoTo() {
        assertEquals("intent.chiikawa.fail.no_partner", socialize.canRun(TestContext.at(OWNER, AT_HEEL).build()).reasonKey());
    }

    @Test
    void aPetOnlyPlaysAlongWhenSomebodyIsComingOver() {
        assertTrue(cooperate.canRun(TestContext.at(OWNER, AT_HEEL).playingAlong().build()).ok());
        assertEquals("intent.chiikawa.fail.not_asked", cooperate.canRun(TestContext.at(OWNER, AT_HEEL).build()).reasonKey());
    }

    @Test
    void aSceneComesAfterAnyErrandButBeforePotteringAboutAndAnsweringComesFirst() {
        float scene = socialize.score(partnerAt(1).build());
        float answer = cooperate.score(TestContext.at(OWNER, AT_HEEL).playingAlong().build());

        assertTrue(scene < 0.4F, "picking up an item comes first");
        assertTrue(scene > 0.05F, "wandering comes after");
        assertTrue(answer > scene, "a pet being visited answers rather than setting off itself");
        assertTrue(answer < 0.6F, "work still takes a pet away");
    }

    private static TestContext partnerAt(int blocksEast) {
        return TestContext.at(OWNER, AT_HEEL).socialPartner(GlobalPos.of(OVERWORLD, OWNER.pos().east(blocksEast)));
    }
}
