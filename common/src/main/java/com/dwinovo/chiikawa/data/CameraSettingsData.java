package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.camera.CameraSettings;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * The generated camera settings, by the id of the camera item. A pet takes a photo only
 * with its owner within sixty-four blocks, so the owner's game has the place in view, and
 * no more than one in half a day; a photo is a 320 by 240 picture through a 70 degree lens.
 * Friends are what it likes best to take, its owner next, and a view only now and then.
 */
public final class CameraSettingsData {
    /** The camera, which is not an item yet: its id is all a settings file is named by. */
    public static final ResourceLocation CAMERA = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "camera");
    /** The film it takes, likewise. */
    private static final ResourceLocation FILM = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "film");

    private CameraSettingsData() {
    }

    /** @return settings by item id */
    public static Map<ResourceLocation, CameraSettings> all() {
        return Map.of(CAMERA, new CameraSettings(FILM, 64.0, 12000, 320, 240, 70,
            new CameraSettings.Subjects(3, 5, 1), 40));
    }
}
