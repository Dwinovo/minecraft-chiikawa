package com.dwinovo.chiikawa.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.PetRoster.Entry;
import java.util.List;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The roster is written by hand into NBT, and everything it is for happens after a
 * restart: a pet nobody has loaded since is exactly the pet a bell is rung for.
 */
class PetRosterTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USAGI = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID HACHIWARE = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void whatIsWrittenDownComesBackWordForWord() {
        Entry inTheNether = new Entry(USAGI, "Usagi", Level.NETHER, new BlockPos(1234, 70, -4321));
        Entry atHome = new Entry(HACHIWARE, "Hachiware", Level.OVERWORLD, new BlockPos(-8, 64, 3));
        PetRoster roster = new PetRoster();
        roster.put(OWNER, inTheNether);
        roster.put(OWNER, atHome);

        List<Entry> read = reloaded(roster).pets(OWNER);

        assertEquals(2, read.size());
        assertTrue(read.contains(inTheNether), () -> "the nether entry came back as " + read);
        assertTrue(read.contains(atHome), () -> "the overworld entry came back as " + read);
    }

    @Test
    void anotherOwnersPetsAreNotYours() {
        UUID stranger = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        PetRoster roster = new PetRoster();
        roster.put(OWNER, new Entry(USAGI, "Usagi", Level.OVERWORLD, BlockPos.ZERO));

        assertEquals(List.of(), reloaded(roster).pets(stranger));
    }

    @Test
    void notingTheSamePetAgainMovesItRatherThanAddingIt() {
        PetRoster roster = new PetRoster();
        roster.put(OWNER, new Entry(USAGI, "Usagi", Level.OVERWORLD, BlockPos.ZERO));
        Entry moved = new Entry(USAGI, "Usagi", Level.OVERWORLD, new BlockPos(200, 64, 200));
        roster.put(OWNER, moved);

        assertEquals(List.of(moved), reloaded(roster).pets(OWNER));
    }

    @Test
    void forgettingThePetForgetsTheOwnerWithIt() {
        PetRoster roster = new PetRoster();
        roster.put(OWNER, new Entry(USAGI, "Usagi", Level.OVERWORLD, BlockPos.ZERO));

        roster.forget(OWNER, USAGI);

        assertEquals(List.of(), roster.pets(OWNER));
        assertEquals(List.of(), roster.owners(), "an owner with no pets left is still on the books");
        assertEquals(List.of(), reloaded(roster).pets(OWNER));
    }

    @Test
    void anEmptyRosterIsStillAValidOne() {
        assertEquals(List.of(), reloaded(new PetRoster()).owners());
    }

    /** Through NBT and back, which is the only trip that counts. */
    private static PetRoster reloaded(PetRoster roster) {
        return PetRoster.load(roster.save());
    }
}
