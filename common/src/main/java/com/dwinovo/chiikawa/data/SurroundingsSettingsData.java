package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.entity.brain.sensor.SurroundingsSettings;
import com.dwinovo.chiikawa.entity.brain.sensor.SurroundingsSettingsLoader;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * The generated surroundings settings (0.1.4 design, section 6): a pet sees eight blocks round it, as far as it takes to
 * get out of the rain or to notice a creeper about to go off; an owner who has been out of sight for two minutes is
 * greeted on coming back, for ten seconds after.
 */
public final class SurroundingsSettingsData {
    private static final int RADIUS = 8;
    /** Two minutes. */
    private static final int AWAY = 2400;
    /** Ten seconds. */
    private static final int RETURNED = 200;

    private SurroundingsSettingsData() {
    }

    /** @return the settings by file id: the one file there is */
    public static Map<ResourceLocation, SurroundingsSettings> all() {
        return Map.of(SurroundingsSettingsLoader.FILE, new SurroundingsSettings(RADIUS, AWAY, RETURNED));
    }
}
