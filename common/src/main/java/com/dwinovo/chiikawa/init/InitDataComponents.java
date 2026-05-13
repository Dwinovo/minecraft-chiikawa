package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.music.MusicBoxSelection;
import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class InitDataComponents {
    public static final Supplier<DataComponentType<MusicBoxSelection>> MUSIC_BOX_SELECTION =
        Services.REGISTRY.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "music_box_selection"),
            () -> DataComponentType.<MusicBoxSelection>builder()
                .persistent(MusicBoxSelection.CODEC)
                .networkSynchronized(MusicBoxSelection.STREAM_CODEC)
                .cacheEncoding()
                .build()
        );

    private InitDataComponents() {
    }

    public static void init() {
    }
}
