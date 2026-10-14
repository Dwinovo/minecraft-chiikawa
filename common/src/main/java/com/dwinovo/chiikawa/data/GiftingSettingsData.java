package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.gifting.GiftingSettings;
import com.dwinovo.chiikawa.gifting.GiftingSettingsLoader;
import com.dwinovo.chiikawa.init.InitItems;
import com.mojang.datafixers.util.Either;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * The generated gifting settings: a pet thinks of a present for its owner at most once a quarter
 * of a game day, and a roll of film, which it only has once it is full, is the one thing it is five times
 * as keen to give as anything else.
 */
public final class GiftingSettingsData {
    private GiftingSettingsData() {
    }

    /** @return the settings by file id: the one file there is */
    public static Map<Identifier, GiftingSettings> all() {
        return Map.of(GiftingSettingsLoader.FILE, new GiftingSettings(6000,
            List.of(new GiftingSettings.Weighted(Either.right(InitItems.FILM.get()), 5))));
    }
}
