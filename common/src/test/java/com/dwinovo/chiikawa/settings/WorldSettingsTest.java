package com.dwinovo.chiikawa.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

class WorldSettingsTest {
    @Test
    void aWorldStartsWithNothingScaled() {
        assertEquals(1.0F, WorldSettings.DEFAULT.wildSpawn());
        assertEquals(1.0F, WorldSettings.DEFAULT.chatter());
    }

    @Test
    void valuesAreBroughtIntoRange() {
        WorldSettings settings = new WorldSettings(3.0F, -1.0F);

        assertEquals(1.0F, settings.wildSpawn(), "wild spawning can only be turned down");
        assertEquals(0.0F, settings.chatter());
        assertEquals(WorldSettings.MAX_CHATTER, new WorldSettings(0.5F, 40.0F).chatter());
        assertEquals(0.0F, new WorldSettings(-0.2F, 1.0F).wildSpawn());
    }

    @Test
    void aNumberThatIsNotOneBecomesTheDefault() {
        assertEquals(WorldSettings.DEFAULT, new WorldSettings(Float.NaN, Float.NaN));
    }

    @Test
    void whatIsSentIsWhatIsRead() {
        ByteBuf buffer = Unpooled.buffer();

        WorldSettings.STREAM_CODEC.encode(buffer, new WorldSettings(0.25F, 1.5F));

        assertEquals(new WorldSettings(0.25F, 1.5F), WorldSettings.STREAM_CODEC.decode(buffer));
    }

    @Test
    void whatIsSentOutOfRangeIsClampedWhereItIsRead() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeFloat(9.0F);
        buffer.writeFloat(9.0F);

        assertEquals(new WorldSettings(1.0F, WorldSettings.MAX_CHATTER), WorldSettings.STREAM_CODEC.decode(buffer));
    }

    @Test
    void whatIsSavedIsWhatIsLoaded() {
        WorldSettingsStore store = new WorldSettingsStore();
        store.set(new WorldSettings(0.4F, 0.0F));

        WorldSettingsStore loaded = WorldSettingsStore.CODEC
            .parse(NbtOps.INSTANCE, WorldSettingsStore.CODEC.encodeStart(NbtOps.INSTANCE, store).getOrThrow()).getOrThrow();

        assertEquals(new WorldSettings(0.4F, 0.0F), loaded.get());
    }

    @Test
    void aFileFromBeforeASettingExistedKeepsItAtTheDefault() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Chatter", 0.5F);

        WorldSettingsStore loaded = WorldSettingsStore.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();

        assertEquals(new WorldSettings(1.0F, 0.5F), loaded.get());
    }

    @Test
    void changingNothingLeavesTheStoreClean() {
        WorldSettingsStore store = new WorldSettingsStore();

        store.set(WorldSettings.DEFAULT);

        assertFalse(store.isDirty());
    }
}
