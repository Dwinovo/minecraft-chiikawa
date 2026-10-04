package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.whistle.WhistleSettings;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * The generated whistle settings, by the id of the item that is blown. The candy carries
 * a long way, a pet takes its time over the order by up to half a second, and a wild one
 * stays curious for ten seconds. The pitch is a flute's, high.
 */
public final class WhistleSettingsData {
    private WhistleSettingsData() {
    }

    /** @return settings by item id */
    public static Map<ResourceLocation, WhistleSettings> all() {
        return Map.of(BuiltInRegistries.ITEM.getKey(InitItems.WHISTLE_CANDY.get()),
            new WhistleSettings(32.0, 10, 40, 10, 200,
                ResourceLocation.withDefaultNamespace("block.note_block.flute"), 2.0F));
    }
}
