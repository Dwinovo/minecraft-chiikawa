package com.dwinovo.chiikawa.entity.brain.sensor;

import static com.dwinovo.chiikawa.entity.brain.combat.Ground.HUNT;
import static com.dwinovo.chiikawa.entity.brain.combat.Ground.NEARBY;
import static com.dwinovo.chiikawa.entity.brain.combat.Ground.OWNER;
import static com.dwinovo.chiikawa.entity.brain.combat.Ground.SELF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.combat.Ground;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Which grounds the owner's orders let a pet fight on, read as a table. */
class FoeCandidatesTest {
    private static final PetOwnership OWNED = new PetOwnership.Owned(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final PetOwnership WILD = PetOwnership.WILD;

    /** One row of the table. */
    private record Row(PetDirective directive, PetOwnership ownership, Set<Ground> grounds) {
        @Override
        public String toString() {
            return ownership == WILD ? "wild" : directive.toString();
        }
    }

    private static Set<Ground> grounds(Ground... grounds) {
        return grounds.length == 0 ? EnumSet.noneOf(Ground.class) : EnumSet.copyOf(List.of(grounds));
    }

    private static List<Row> table() {
        List<Row> rows = new ArrayList<>();
        // At heel it guards what is about it and what comes by; the hunt is for a pet let loose.
        rows.add(new Row(PetDirective.FOLLOW, OWNED, grounds(SELF, OWNER, NEARBY)));
        // A pet told to sit sits through all of it.
        rows.add(new Row(PetDirective.STAY, OWNED, grounds()));
        // Let loose, what comes by is a hunt.
        rows.add(new Row(PetDirective.FREE, OWNED, grounds(SELF, OWNER, HUNT)));
        // A wild pet has no directive to go by: it guards its patch.
        for (PetDirective directive : PetDirective.values()) {
            rows.add(new Row(directive, WILD, grounds(SELF, OWNER, NEARBY)));
        }
        return rows;
    }

    @Test
    void theOwnersOrdersGiveThePetItsGrounds() {
        for (Row row : table()) {
            assertEquals(row.grounds(), FoeCandidates.of(row.directive(), row.ownership(), true), row::toString);
        }
    }

    @Test
    void aPetWhoseJobHasNoFightInItFightsOnlyForItselfAndItsOwner() {
        Set<Ground> forItself = grounds(SELF, OWNER);
        for (Row row : table()) {
            Set<Ground> expected = EnumSet.copyOf(row.grounds());
            expected.retainAll(forItself);

            assertEquals(expected, FoeCandidates.of(row.directive(), row.ownership(), false), row::toString);
        }
    }

    @Test
    void aHuntIsForAPetLetLooseAndNeverAGuardsGround() {
        for (Row row : table()) {
            boolean free = row.ownership() == OWNED && row.directive() == PetDirective.FREE;
            assertFalse(row.grounds().contains(HUNT) && !free, "a hunt for " + row);
            assertFalse(row.grounds().contains(NEARBY) && free, "a guard's patch for " + row);
        }
    }

    @Test
    void whatTheTableAnswersCannotBeChangedByTheCaller() {
        Set<Ground> grounds = FoeCandidates.of(PetDirective.FOLLOW, OWNED, true);

        assertThrows(UnsupportedOperationException.class, () -> grounds.add(HUNT));
    }
}
