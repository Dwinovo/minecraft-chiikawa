package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.platform.Services;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class InitSounds {
    private static final List<SoundEntry> ENTRIES = new ArrayList<>();

    public static final Supplier<SoundEvent> CHIIKAWA_INJURED = register("chiikawa/injured");
    public static final Supplier<SoundEvent> CHIIKAWA_TAME = register("chiikawa/tame");

    public static final Supplier<SoundEvent> HACHIWARE_INJURED = register("hachiware/injured");
    public static final Supplier<SoundEvent> HACHIWARE_TAME = register("hachiware/tame");

    public static final Supplier<SoundEvent> KURIMANJU_TAME = register("kurimanju/tame");

    public static final Supplier<SoundEvent> MOMONGA_INJURED = register("momonga/injured");
    public static final Supplier<SoundEvent> MOMONGA_TAME = register("momonga/tame");

    public static final Supplier<SoundEvent> RAKKO_TAME = register("rakko/tame");

    public static final Supplier<SoundEvent> SHISA_TAME = register("shisa/tame");

    public static final Supplier<SoundEvent> USAGI_AMBIENT = register("usagi/ambient");
    public static final Supplier<SoundEvent> USAGI_ATTACK = register("usagi/attack");
    public static final Supplier<SoundEvent> USAGI_INJURED = register("usagi/injured");
    public static final Supplier<SoundEvent> USAGI_TAME = register("usagi/tame");

    private InitSounds() {
    }

    public static void init() {
    }

    public static List<SoundEntry> entries() {
        return List.copyOf(ENTRIES);
    }

    private static Supplier<SoundEvent> register(String path) {
        Supplier<SoundEvent> holder = Services.REGISTRY.register(
            BuiltInRegistries.SOUND_EVENT,
            new ResourceLocation(Constants.MOD_ID, path),
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Constants.MOD_ID, path))
        );
        ENTRIES.add(new SoundEntry(path, holder));
        return holder;
    }

    public record SoundEntry(String path, Supplier<SoundEvent> holder) {
    }
}
