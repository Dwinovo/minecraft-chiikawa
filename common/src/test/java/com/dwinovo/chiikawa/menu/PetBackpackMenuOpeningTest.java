package com.dwinovo.chiikawa.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

/**
 * The pet's id goes to the client with the menu's opening data, a whole int, as the horse screen's
 * packet carries the horse's. A data slot would have sent it as a short.
 */
class PetBackpackMenuOpeningTest {
    private static int arrives(int id) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        PetBackpackMenu.writeOpeningData(buffer, id);
        return PetBackpackMenu.readOpeningData(buffer);
    }

    @Test
    void idsPastWhatAShortHoldsArriveWhole() {
        for (int id : new int[] {0, 1, 255, 32767, 32768, 40685, 65535, 65536, 70000, 1_000_000, Integer.MAX_VALUE}) {
            assertEquals(id, arrives(id), "entity " + id);
        }
    }

    @Test
    void noPetIsAnIdOfItsOwn() {
        assertEquals(PetBackpackMenu.NO_PET, arrives(PetBackpackMenu.NO_PET));
    }
}
