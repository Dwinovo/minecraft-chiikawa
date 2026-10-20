package com.dwinovo.chiikawa.entity.brain.intent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.ErrandSettingsData;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.testing.TestContext;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class SocialIntentsTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos OWNER = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor AT_HEEL =
        TestAnchors.following(OWNER);

    private final SocializeIntent socialize = new SocializeIntent();
    private final CooperateIntent cooperate = new CooperateIntent();

    @Test
    void bothSidesOfASceneAreSocial() {
        assertEquals(IntentCategory.SOCIAL, socialize.category());
        assertEquals(IntentCategory.SOCIAL, cooperate.category());
    }

    @Test
    void aPetGoesOverToThePartnerItHasThoughtOf() {
        assertTrue(socialize.canRun(partnerAt(10).build()).ok());
        assertEquals(Optional.of(GlobalPos.of(OVERWORLD, OWNER.pos().east(10))), socialize.destination(partnerAt(10).build()));
    }

    @Test
    void thePartnerOfASceneStaysWhereItIs() {
        assertEquals(Optional.empty(), cooperate.destination(TestContext.at(OWNER, AT_HEEL).playingAlong().build()));
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
        ErrandSettings<?> scene = ErrandSettingsData.all().get(ErrandIds.SOCIALIZE);
        ErrandSettings<?> answer = ErrandSettingsData.all().get(ErrandIds.COOPERATE);
        ErrandSettings<?> pickUp = ErrandSettingsData.all().get(ErrandIds.PICK_UP_ITEM);
        ErrandSettings<?> wander = ErrandSettingsData.all().get(ErrandIds.WANDER);

        assertEquals(pickUp.priority(), scene.priority());
        assertTrue(scene.score() < pickUp.score(), "picking up an item comes first");
        assertTrue(scene.priority() > wander.priority(), "wandering comes after");
        assertTrue(answer.priority() > scene.priority(), "a pet being visited answers rather than setting off itself");
    }

    private static TestContext partnerAt(int blocksEast) {
        return TestContext.at(OWNER, AT_HEEL).socialPartner(GlobalPos.of(OVERWORLD, OWNER.pos().east(blocksEast)));
    }
}
