package com.dwinovo.chiikawa.entity.brain.sensor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;

/**
 * The {@code FARM_SIGHTS} memory: the blocks of work the farmer sensor found after the one it chose for each kind, in
 * the order it found them, nearest first. The chosen one is in the kind's own memory, see {@link FarmTarget#memory()}.
 * What the sensor sees is a field and not one crop, so a pet that has just reaped a crop still knows the next one
 * before the sensor looks again, and an errand that yields to its work is not let through in the meantime. Immutable,
 * like every memory value; not saved.
 */
public final class FarmSights {
    public static final FarmSights NONE = new FarmSights(Map.of());

    private final Map<FarmTarget, List<BlockPos>> after;

    private FarmSights(Map<FarmTarget, List<BlockPos>> after) {
        this.after = after;
    }

    /** @return the blocks after the chosen one for this kind, nearest first, none if the sensor found no more */
    public List<BlockPos> after(FarmTarget target) {
        return after.getOrDefault(target, List.of());
    }

    /** @return these sights with the blocks after the chosen one for this kind replaced */
    public FarmSights with(FarmTarget target, List<BlockPos> blocks) {
        Map<FarmTarget, List<BlockPos>> next = new EnumMap<>(FarmTarget.class);
        next.putAll(after);
        if (blocks.isEmpty()) {
            next.remove(target);
        } else {
            next.put(target, List.copyOf(blocks));
        }
        return new FarmSights(next);
    }
}
