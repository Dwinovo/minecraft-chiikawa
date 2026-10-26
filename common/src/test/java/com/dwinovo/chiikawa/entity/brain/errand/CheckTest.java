package com.dwinovo.chiikawa.entity.brain.errand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.ItemTags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** What an errand answers when it cannot start, and what a pet that lacks something says it lacks. */
class CheckTest {
    private static final ResourceLocation MUSIC_BOX = ResourceLocation.fromNamespaceAndPath("chiikawa", "music_box");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void aPlainFailureNamesItsReasonAndLacksNothing() {
        Check check = Check.fail("no_crop");

        assertFalse(check.ok());
        assertEquals("intent.chiikawa.fail.no_crop", check.reasonKey());
        assertEquals("no_crop", check.reason());
        assertEquals(Optional.empty(), check.lack());
    }

    @Test
    void aLackIsAFailureWithTheSameReasonKeyAndWhatIsShort() {
        Check check = Check.lack("no_arrows", LackKey.tag(ItemTags.ARROWS));

        assertFalse(check.ok());
        assertEquals(Check.fail("no_arrows").reasonKey(), check.reasonKey());
        assertEquals(Optional.of(LackKey.tag(ItemTags.ARROWS)), check.lack());
    }

    @Test
    void aCheckThatPassesLacksNothing() {
        assertTrue(Check.OK.ok());
        assertEquals(Optional.empty(), Check.OK.lack());
    }

    @Test
    void whatIsShortIsNamedByTheKeyOfItsNameInTheGamesWords() {
        assertEquals("tag.item.minecraft.arrows", LackKey.tag(ItemTags.ARROWS).translationKey());
        assertEquals("tag.item.chiikawa.entity_plant_crops", new LackKey.OfTag(
            ResourceLocation.fromNamespaceAndPath("chiikawa", "entity_plant_crops")).translationKey());
        // An item the registry does not know yet is named the way the game names an item.
        assertEquals("item.chiikawa.music_box", LackKey.item(MUSIC_BOX).translationKey());
    }
}
