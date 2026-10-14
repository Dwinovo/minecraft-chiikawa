package com.dwinovo.chiikawa.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The world's {@link WorldSettings}, kept with the save so they go wherever the world goes.
 * One per server rather than one per level, in the overworld's storage like the pet roster.
 */
public class WorldSettingsStore extends SavedData {
    /** The file this lives in, beside the world's other saved data. */
    private static final String FILE = "chiikawa_settings";

    /**
     * The settings as they are saved. A file written before a field existed keeps that field as it
     * was by default.
     */
    static final Codec<WorldSettingsStore> CODEC = RecordCodecBuilder.<WorldSettings>create(instance -> instance.group(
        Codec.FLOAT.optionalFieldOf("WildSpawn", WorldSettings.DEFAULT.wildSpawn()).forGetter(WorldSettings::wildSpawn),
        Codec.FLOAT.optionalFieldOf("Chatter", WorldSettings.DEFAULT.chatter()).forGetter(WorldSettings::chatter)
    ).apply(instance, WorldSettings::new)).xmap(WorldSettingsStore::load, WorldSettingsStore::get);
    private static final SavedDataType<WorldSettingsStore> TYPE = new SavedDataType<>(Identifier.withDefaultNamespace(FILE), WorldSettingsStore::new, CODEC, null);

    private WorldSettings settings = WorldSettings.DEFAULT;

    public static WorldSettingsStore of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
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

    private static WorldSettingsStore load(WorldSettings settings) {
        WorldSettingsStore store = new WorldSettingsStore();
        store.settings = settings;
        return store;
    }
}
