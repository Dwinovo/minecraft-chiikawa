package com.dwinovo.chiikawa.settings;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The world's {@link WorldSettings}, kept with the save so they go wherever the world goes.
 * One per server rather than one per level, in the overworld's storage like the pet roster.
 */
public class WorldSettingsStore extends SavedData {
    /** The file this lives in, beside the world's other saved data. */
    private static final String FILE = "chiikawa_settings";

    private WorldSettings settings = WorldSettings.DEFAULT;

    public static WorldSettingsStore of(MinecraftServer server) {
        return server.overworld().getDataStorage()
            .computeIfAbsent(new SavedData.Factory<>(WorldSettingsStore::new, WorldSettingsStore::load, null), FILE);
    }

    public static WorldSettingsStore of(ServerLevel level) {
        return of(level.getServer());
    }

    public WorldSettings get() {
        return settings;
    }

    public void set(WorldSettings next) {
        if (!settings.equals(next)) {
            settings = next;
            setDirty();
        }
    }

    static WorldSettingsStore load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldSettingsStore store = new WorldSettingsStore();
        // A file written before a field existed keeps that field as it was by default.
        store.settings = new WorldSettings(
            tag.contains("WildSpawn") ? tag.getFloat("WildSpawn") : WorldSettings.DEFAULT.wildSpawn(),
            tag.contains("Chatter") ? tag.getFloat("Chatter") : WorldSettings.DEFAULT.chatter());
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putFloat("WildSpawn", settings.wildSpawn());
        tag.putFloat("Chatter", settings.chatter());
        return tag;
    }
}
