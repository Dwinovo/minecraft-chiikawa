package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.entity.brain.constraint.AnchorSettings;
import com.dwinovo.chiikawa.entity.brain.constraint.AnchorSettingsLoader;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * The generated anchor settings: a pet at heel starts walking back at seven blocks and is fetched
 * at twenty, one that roams free keeps to thirteen blocks of its home for an errand and twenty to
 * go on with it, and a wild one has the run of twenty-four and thirty-two.
 */
public final class AnchorSettingsData {
    private AnchorSettingsData() {
    }

    /** @return the settings by file id: the one file there is */
    public static Map<ResourceLocation, AnchorSettings> all() {
        return Map.of(AnchorSettingsLoader.FILE, new AnchorSettings(
            new AnchorSettings.Follow(7.0, 2, 12.0, 16.0, 20.0),
            new AnchorSettings.Area(13.0, 20.0),
            new AnchorSettings.Area(24.0, 32.0)));
    }
}
