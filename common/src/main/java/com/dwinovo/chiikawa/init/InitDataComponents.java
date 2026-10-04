package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.music.MusicBoxSelection;
import com.dwinovo.chiikawa.music.PlaybackMode;
import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class InitDataComponents {
    public static final Supplier<DataComponentType<MusicBoxSelection>> MUSIC_BOX_SELECTION =
        Services.REGISTRY.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            new ResourceLocation(Constants.MOD_ID, "music_box_selection"),
            () -> DataComponentType.<MusicBoxSelection>builder()
                .persistent(MusicBoxSelection.CODEC)
                .networkSynchronized(MusicBoxSelection.STREAM_CODEC)
                .cacheEncoding()
                .build()
        );

    /**
     * What the pet does when a song ends; once when the box has none. Kept apart from the
     * selection so it can be set before any song is chosen and survives choosing another.
     */
    public static final Supplier<DataComponentType<PlaybackMode>> MUSIC_BOX_MODE =
        Services.REGISTRY.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            new ResourceLocation(Constants.MOD_ID, "music_box_mode"),
            () -> DataComponentType.<PlaybackMode>builder()
                .persistent(PlaybackMode.CODEC)
                .networkSynchronized(PlaybackMode.STREAM_CODEC)
                .build()
        );

    /** The order a whistle candy blows; follow when the stack has none. */
    public static final Supplier<DataComponentType<PetDirective>> WHISTLE_MODE =
        Services.REGISTRY.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            new ResourceLocation(Constants.MOD_ID, "whistle_mode"),
            () -> DataComponentType.<PetDirective>builder()
                .persistent(PetDirective.CODEC)
                .networkSynchronized(PetDirective.STREAM_CODEC)
                .build()
        );

    private InitDataComponents() {
    }

    public static void init() {
    }
}
