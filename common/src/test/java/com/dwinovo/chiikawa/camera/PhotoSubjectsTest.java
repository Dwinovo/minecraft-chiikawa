package com.dwinovo.chiikawa.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.testing.FixedRandom;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PhotoSubjectsTest {
    /** The owner three, a friend five and a view one, as the generated camera has them. */
    private static final CameraSettings.Subjects WEIGHTS = new CameraSettings.Subjects(3, 5, 1);
    private static final Set<PhotoSubject> ALL = EnumSet.allOf(PhotoSubject.class);

    @Test
    void whenNothingIsWorthAPhotoNothingIsPicked() {
        // A random source with nothing in it throws if anything is drawn from it.
        assertEquals(Optional.empty(), PhotoSubjects.choose(Set.of(), WEIGHTS, FixedRandom.ints()));
    }

    @Test
    void theOnlyGoodMomentIsTheOneThatIsPicked() {
        for (PhotoSubject only : PhotoSubject.values()) {
            for (int roll = 0; roll < 9; roll++) {
                assertEquals(Optional.of(only), PhotoSubjects.choose(Set.of(only), WEIGHTS, FixedRandom.ints(roll)));
            }
        }
    }

    @Test
    void theKindsAreDrawnInProportionToTheirWeights() {
        // Nine in all: the owner takes the first three of them, a friend the next five, a view the last.
        assertEquals(Optional.of(PhotoSubject.OWNER), PhotoSubjects.choose(ALL, WEIGHTS, FixedRandom.ints(0)));
        assertEquals(Optional.of(PhotoSubject.OWNER), PhotoSubjects.choose(ALL, WEIGHTS, FixedRandom.ints(2)));
        assertEquals(Optional.of(PhotoSubject.FRIEND), PhotoSubjects.choose(ALL, WEIGHTS, FixedRandom.ints(3)));
        assertEquals(Optional.of(PhotoSubject.FRIEND), PhotoSubjects.choose(ALL, WEIGHTS, FixedRandom.ints(7)));
        assertEquals(Optional.of(PhotoSubject.SCENERY), PhotoSubjects.choose(ALL, WEIGHTS, FixedRandom.ints(8)));
    }

    @Test
    void aKindThatThereIsNoMomentForIsLeftOutOfTheDraw() {
        // Without a friend there are four in all: the owner's three, then the view.
        Set<PhotoSubject> withoutAFriend = EnumSet.of(PhotoSubject.OWNER, PhotoSubject.SCENERY);

        assertEquals(Optional.of(PhotoSubject.OWNER), PhotoSubjects.choose(withoutAFriend, WEIGHTS, FixedRandom.ints(2)));
        assertEquals(Optional.of(PhotoSubject.SCENERY), PhotoSubjects.choose(withoutAFriend, WEIGHTS, FixedRandom.ints(3)));
    }

    @Test
    void aKindThePetDoesNotFancyIsNeverPickedHoweverGoodTheMoment() {
        CameraSettings.Subjects noViews = new CameraSettings.Subjects(3, 5, 0);

        assertEquals(Optional.empty(), PhotoSubjects.choose(Set.of(PhotoSubject.SCENERY), noViews, FixedRandom.ints(0)));
        assertEquals(Optional.of(PhotoSubject.FRIEND), PhotoSubjects.choose(ALL, noViews, FixedRandom.ints(7)));
    }
}
