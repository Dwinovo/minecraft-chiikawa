package com.dwinovo.chiikawa.spawn;

import com.dwinovo.chiikawa.settings.WorldSettingsStore;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Whether a wild pet may spawn here: whatever lets an animal spawn, and then the world's
 * wild spawn setting, rolled once for every attempt.
 *
 * <p>Rolled at each attempt rather than folded into the spawn weights because the weights
 * are written into the biomes when the server starts, before the world's settings can be
 * read; a roll takes effect the moment the setting changes. It also means the setting can
 * only turn spawning down, which is what it is for.
 */
public final class WildSpawning {
    private WildSpawning() {
    }

    /** The spawn predicate both loaders register for every pet, in place of the animals' own. */
    public static <T extends Animal> boolean canSpawn(EntityType<T> type, ServerLevelAccessor level,
            MobSpawnType reason, BlockPos pos, RandomSource random) {
        return Animal.checkAnimalSpawnRules(type, level, reason, pos, random)
            && allows(WorldSettingsStore.of(level.getLevel()).get().wildSpawn(), reason, random);
    }

    /**
     * Whether the setting lets a spawn for this reason through. Only the game's own spawning
     * is thinned out: a spawn egg, a command, a pet born to its parents or placed by a
     * structure is somebody's decision, not the wild's, and goes ahead.
     *
     * @param wildSpawn the setting, from 0 to 1
     */
    public static boolean allows(float wildSpawn, MobSpawnType reason, RandomSource random) {
        if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) {
            return true;
        }
        if (wildSpawn >= 1.0F) {
            return true;
        }
        return wildSpawn > 0.0F && random.nextFloat() < wildSpawn;
    }
}
