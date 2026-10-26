package com.dwinovo.chiikawa.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.network.PetPayloads.PetOutlookPayload;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** What the pet screen says of why a pet is not at work. */
class PetStatusTextTest {
    private static final ResourceLocation WANDER = ResourceLocation.fromNamespaceAndPath("chiikawa", "wander");

    @Test
    void whatThePetIsShortOfIsSaidInPlaceOfTheReasonItGaveWhichMayBeAboutSomethingElse() {
        PetOutlookPayload outlook = new PetOutlookPayload(1, Optional.of(WANDER), Optional.of("intent.chiikawa.fail.no_farmland"),
            Optional.of("tag.item.chiikawa.entity_plant_crops"));

        Component words = PetStatusText.notWorking(outlook).orElseThrow();

        TranslatableContents contents = (TranslatableContents) words.getContents();
        assertEquals("screen.chiikawa.pet.outlook.lacks", contents.getKey());
        assertEquals(1, contents.getArgs().length);
        assertEquals("tag.item.chiikawa.entity_plant_crops", ((TranslatableContents) ((Component) contents.getArgs()[0]).getContents()).getKey());
    }

    @Test
    void withNothingShortTheReasonIsSaidAsItIsAndAPetAtWorkHasNone() {
        PetOutlookPayload seeking = new PetOutlookPayload(1, Optional.empty(), Optional.of("intent.chiikawa.fail.no_crop"), Optional.empty());
        PetOutlookPayload atWork = new PetOutlookPayload(1, Optional.of(WANDER), Optional.empty(), Optional.empty());

        assertEquals("intent.chiikawa.fail.no_crop", ((TranslatableContents) PetStatusText.notWorking(seeking).orElseThrow().getContents()).getKey());
        assertEquals(Optional.empty(), PetStatusText.notWorking(atWork));
    }

    @Test
    void whatItWouldDoNextIsNamedAsTheErrandIsEverywhere() {
        assertEquals("intent.chiikawa.wander", ((TranslatableContents) PetStatusText.next(WANDER).getContents()).getKey());
    }
}
