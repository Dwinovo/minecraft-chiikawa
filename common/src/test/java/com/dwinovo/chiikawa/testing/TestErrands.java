package com.dwinovo.chiikawa.testing;

import com.dwinovo.chiikawa.data.ErrandSettingsData;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * The errand settings a game has once its data packs are loaded, for a test that runs the selector
 * without a server: the ones the mod ships, as the data generator writes them.
 */
public final class TestErrands {
    private TestErrands() {
    }

    /** Makes the shipped settings the ones in force. */
    public static void load() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ErrandSettings.replaceAll(ErrandSettingsData.all());
    }
}
