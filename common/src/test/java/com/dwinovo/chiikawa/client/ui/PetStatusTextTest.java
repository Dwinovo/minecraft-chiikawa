package com.dwinovo.chiikawa.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.network.PetPayloads.PetOutlookPayload;
import com.dwinovo.chiikawa.qualification.LicenceView;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

/** What the pet screen says of why a pet is not at work. */
class PetStatusTextTest {
    private static final Identifier WANDER = Identifier.fromNamespaceAndPath("chiikawa", "wander");

    @Test
    void whatThePetIsShortOfIsSaidInPlaceOfTheReasonItGaveWhichMayBeAboutSomethingElse() {
        PetOutlookPayload outlook = new PetOutlookPayload(1, Optional.of(WANDER), Optional.of("intent.chiikawa.fail.no_farmland"),
            Optional.of("tag.item.chiikawa.entity_plant_crops"), Optional.empty());

        Component words = PetStatusText.notWorking(outlook).orElseThrow();

        TranslatableContents contents = (TranslatableContents) words.getContents();
        assertEquals("screen.chiikawa.pet.outlook.lacks", contents.getKey());
        assertEquals(1, contents.getArgs().length);
        assertEquals("tag.item.chiikawa.entity_plant_crops", ((TranslatableContents) ((Component) contents.getArgs()[0]).getContents()).getKey());
    }

    @Test
    void withNothingShortTheReasonIsSaidAsItIsAndAPetAtWorkHasNone() {
        PetOutlookPayload seeking = new PetOutlookPayload(1, Optional.empty(), Optional.of("intent.chiikawa.fail.no_crop"), Optional.empty(), Optional.empty());
        PetOutlookPayload atWork = new PetOutlookPayload(1, Optional.of(WANDER), Optional.empty(), Optional.empty(), Optional.empty());

        assertEquals("intent.chiikawa.fail.no_crop", ((TranslatableContents) PetStatusText.notWorking(seeking).orElseThrow().getContents()).getKey());
        assertEquals(Optional.empty(), PetStatusText.notWorking(atWork));
    }

    @Test
    void whatItWouldDoNextIsNamedAsTheErrandIsEverywhere() {
        assertEquals("intent.chiikawa.wander", ((TranslatableContents) PetStatusText.next(WANDER).getContents()).getKey());
    }

    @Test
    void aTimerReadsAsMinutesAndSecondsWithTheSecondsToTwoPlaces() {
        assertEquals("3:20", PetStatusText.clock(200 * 20));
        assertEquals("0:05", PetStatusText.clock(5 * 20));
        assertEquals("12:00", PetStatusText.clock(720 * 20));
        assertEquals("0:00", PetStatusText.clock(19));
    }

    private static LicenceView licence(String id, boolean practised, Optional<Identifier> job) {
        return new LicenceView(Identifier.fromNamespaceAndPath("chiikawa", id), Identifier.withDefaultNamespace("book"),
            2, 5, practised, false, false, job);
    }

    @Test
    void aLicenceWhoseNextStepIsTheOwnersIsTheOneShownBeforeTheOneForTheWork() {
        Identifier farmer = Identifier.fromNamespaceAndPath("chiikawa", "farmer");
        LicenceView forTheWork = licence("weeding", false, Optional.of(farmer));
        LicenceView ready = licence("music", true, Optional.empty());

        assertEquals(Optional.of(ready), PetStatusText.licenceShown(List.of(forTheWork, ready), farmer));
        assertEquals(Optional.of(forTheWork), PetStatusText.licenceShown(List.of(forTheWork), farmer));
    }

    @Test
    void withNoLicenceReadyAndNoneForTheWorkNoneIsShown() {
        Identifier farmer = Identifier.fromNamespaceAndPath("chiikawa", "farmer");
        Identifier archer = Identifier.fromNamespaceAndPath("chiikawa", "archer");

        assertEquals(Optional.empty(), PetStatusText.licenceShown(List.of(licence("shooting", false, Optional.of(archer))), farmer));
        assertEquals(Optional.empty(), PetStatusText.licenceShown(List.of(), farmer));
    }
}
