package com.dwinovo.chiikawa.spawn;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.testing.FixedRandom;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.MobSpawnType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WildSpawningTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void aFullSettingLetsEverySpawnThroughWithoutRolling() {
        // A random source with nothing in it throws if anything is drawn from it.
        assertTrue(WildSpawning.allows(1.0F, MobSpawnType.NATURAL, FixedRandom.floats()));
        assertTrue(WildSpawning.allows(1.0F, MobSpawnType.CHUNK_GENERATION, FixedRandom.floats()));
    }

    @Test
    void aSettingOfNothingLetsNoNaturalSpawnThroughWithoutRolling() {
        assertFalse(WildSpawning.allows(0.0F, MobSpawnType.NATURAL, FixedRandom.floats()));
        assertFalse(WildSpawning.allows(0.0F, MobSpawnType.CHUNK_GENERATION, FixedRandom.floats()));
    }

    @Test
    void inBetweenIsARollBelowTheSetting() {
        assertTrue(WildSpawning.allows(0.5F, MobSpawnType.NATURAL, FixedRandom.floats(0.49F)));
        assertFalse(WildSpawning.allows(0.5F, MobSpawnType.NATURAL, FixedRandom.floats(0.5F)));
        assertFalse(WildSpawning.allows(0.5F, MobSpawnType.CHUNK_GENERATION, FixedRandom.floats(0.9F)));
    }

    @Test
    void whatSomebodyDecidedIsNeverThinnedOut() {
        for (MobSpawnType reason : MobSpawnType.values()) {
            if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) {
                assertTrue(WildSpawning.allows(0.0F, reason, FixedRandom.floats()), reason + " was stopped");
            }
        }
    }
}
