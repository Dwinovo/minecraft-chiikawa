package com.dwinovo.chiikawa.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.world.inventory.DataSlot;
import org.junit.jupiter.api.Test;

/** An entity's id gets to the client whole, as a data slot is sent: sixteen bits at a time. */
class EntityIdSlotsTest {
    /** What the client ends up with, the slots having been through the packet that carries them. */
    private static int arrives(int id) {
        EntityIdSlots server = new EntityIdSlots();
        server.set(id);
        List<DataSlot> serverSlots = new ArrayList<>();
        server.addTo(serverSlots::add);
        EntityIdSlots client = new EntityIdSlots();
        List<DataSlot> clientSlots = new ArrayList<>();
        client.addTo(clientSlots::add);
        for (int i = 0; i < serverSlots.size(); i++) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            ClientboundContainerSetDataPacket.STREAM_CODEC.encode(buffer,
                new ClientboundContainerSetDataPacket(1, i, serverSlots.get(i).get()));
            ClientboundContainerSetDataPacket packet = ClientboundContainerSetDataPacket.STREAM_CODEC.decode(buffer);
            clientSlots.get(packet.getId()).set(packet.getValue());
        }
        return client.get();
    }

    @Test
    void idsPastWhatAShortHoldsArriveWhole() {
        for (int id : new int[] {0, 1, 255, 32767, 32768, 40685, 65535, 65536, 70000, 1_000_000, Integer.MAX_VALUE}) {
            assertEquals(id, arrives(id), "entity " + id);
        }
    }

    @Test
    void noPetIsMinusOneOnBothSides() {
        assertEquals(-1, arrives(-1));
    }

    @Test
    void oneSlotAloneWouldHaveLostIt() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ClientboundContainerSetDataPacket.STREAM_CODEC.encode(buffer, new ClientboundContainerSetDataPacket(1, 0, 40685));

        assertNotEquals(40685, ClientboundContainerSetDataPacket.STREAM_CODEC.decode(buffer).getValue(),
            "the packet carries sixteen bits of a data slot, so this guards the reason for two");
    }
}
