package com.dwinovo.chiikawa.entity.brain.errand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.init.InitTag;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** What a pet's screen pictures what it is short of by. */
class LackKeyTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void anItemIsPicturedByItself() {
        ResourceLocation musicBox = ResourceLocation.fromNamespaceAndPath("chiikawa", "music_box");

        assertEquals(Optional.of(musicBox), LackKey.item(musicBox).icon());
    }

    @Test
    void aTagBeforeTheTagsAreBoundHasNoItemToPictureItByAndIsNotAnError() {
        assertEquals(Optional.empty(), LackKey.tag(InitTag.ENTITY_PLANT_CROPS).icon());
    }
}
