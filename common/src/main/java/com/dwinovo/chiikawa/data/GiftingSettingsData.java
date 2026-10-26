package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.gifting.GiftOccasion;
import com.dwinovo.chiikawa.gifting.GiftingSettings;
import com.dwinovo.chiikawa.gifting.GiftingSettingsLoader;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitTag;
import com.mojang.datafixers.util.Either;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/**
 * The generated gifting settings (0.1.4 design, section 7): a pet thinks of a present only because something happened,
 * each occasion with a chance of its own, and only a day after the last it gave; an owner is given three a day, all their
 * pets together. A present it has in mind it forgets after a day.
 *
 * <p>What a pet gives that it has not set aside or does not like is a flower found on the road, which anyone gives, and
 * what its kind is known for: Furuhonya's books and Rakko's sweets.
 */
public final class GiftingSettingsData {
    /** A game day. */
    private static final int A_DAY = 24000;
    private static final int PRESENTS_A_DAY = 3;
    /** A pet that was treated thinks of a present a minute later, and not as it is fed. */
    private static final int A_MINUTE = 1200;

    private GiftingSettingsData() {
    }

    /** @return the settings by file id: the one file there is */
    public static Map<ResourceLocation, GiftingSettings> all() {
        return Map.of(GiftingSettingsLoader.FILE, new GiftingSettings(A_DAY, PRESENTS_A_DAY, A_DAY,
            Map.of(
                GiftOccasion.FOUND_SOMETHING, new GiftingSettings.OccasionSettings(0.5F, 0),
                GiftOccasion.GOT_PAID, new GiftingSettings.OccasionSettings(0.2F, 0),
                GiftOccasion.PASSED_EXAM, new GiftingSettings.OccasionSettings(0.5F, 0),
                GiftOccasion.WAS_TREATED, new GiftingSettings.OccasionSettings(0.3F, A_MINUTE),
                GiftOccasion.FILM_FULL, new GiftingSettings.OccasionSettings(1.0F, 0)),
            List.of(
                new GiftingSettings.Gift(Either.right(InitTag.GIFT_FLOWERS), 3, List.of()),
                new GiftingSettings.Gift(Either.left(Items.BOOK), 4, List.of(BuiltInRegistries.ENTITY_TYPE.getKey(InitEntity.FURUHONYA_PET.get()))),
                new GiftingSettings.Gift(Either.right(InitTag.SWEETS), 4, List.of(BuiltInRegistries.ENTITY_TYPE.getKey(InitEntity.RAKKO_PET.get())))),
            InitTag.NEVER_GIFTED));
    }
}
