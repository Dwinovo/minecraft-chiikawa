package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.camera.CameraSettings;
import com.dwinovo.chiikawa.init.InitItems;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * The generated camera settings, by the id of the camera item. A pet takes a photo only
 * with its owner within sixty-four blocks, so the owner's game has the place in view, and
 * no more than one in half a day, or one in half a minute after one that did not come
 * back; a photo is a 320 by 240 picture through a 70 degree lens, taken after two seconds
 * of aiming, and posed half the time; a roll holds eight. Friends are what it likes best to take, its owner next, and a view only now
 * and then.
 */
public final class CameraSettingsData {
    private CameraSettingsData() {
    }

    /** @return settings by item id */
    public static Map<ResourceLocation, CameraSettings> all() {
        return Map.of(BuiltInRegistries.ITEM.getKey(InitItems.CAMERA.get()),
            new CameraSettings(BuiltInRegistries.ITEM.getKey(InitItems.FILM.get()), 64.0, 12000, 600, 320, 240, 70,
                new CameraSettings.Subjects(3, 5, 1), 40, 8, 0.5F));
    }
}
