package com.dwinovo.chiikawa.entity.brain;

import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * How long a memory has left. The game answers for a memory that is not there as for one that never
 * expires; to a pet that is resting from something, a memory that is not there is a rest that is over.
 */
public final class MemoryTime {
    private MemoryTime() {
    }

    /**
     * @param brain the brain
     * @param memory the memory
     * @return the ticks it has left, 0 if the brain does not hold it
     */
    public static long left(Brain<?> brain, MemoryModuleType<?> memory) {
        return brain.hasMemoryValue(memory) ? brain.getTimeUntilExpiry(memory) : 0L;
    }
}
