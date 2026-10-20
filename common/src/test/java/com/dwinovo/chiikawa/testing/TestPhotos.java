package com.dwinovo.chiikawa.testing;

import com.dwinovo.chiikawa.camera.CameraSettings;
import com.dwinovo.chiikawa.camera.PhotoOutlook;
import com.dwinovo.chiikawa.camera.PhotoPlan;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** A camera and the moments for it, for a test of what a pet could take a photo of, without a world. */
public final class TestPhotos {
    /** The camera item the plans are for. */
    public static final ResourceLocation CAMERA_ITEM = ResourceLocation.fromNamespaceAndPath("chiikawa", "camera");
    /** The owner three, a friend five and a view one, as the generated camera has them. */
    public static final CameraSettings CAMERA = new CameraSettings(ResourceLocation.fromNamespaceAndPath("chiikawa", "film"),
        64.0, 12000, 600, 320, 240, 70, new CameraSettings.Subjects(3, 5, 1), 40, 8, 0.5F);

    private TestPhotos() {
    }

    /** @return a moment of this kind, of this creature, or of a view when there is none */
    public static PhotoPlan moment(PhotoSubject kind, Optional<UUID> subject) {
        return new PhotoPlan(CAMERA_ITEM, kind, subject, "", Vec3.ZERO);
    }

    /** @return a moment of a view */
    public static PhotoPlan view() {
        return moment(PhotoSubject.SCENERY, Optional.empty());
    }

    /** @return the photo of somebody */
    public static PhotoPlan of(PhotoSubject kind, UUID subject) {
        return moment(kind, Optional.of(subject));
    }

    /** @return what a pet has that is ready to take these photos: a camera, film, an owner near and calm */
    public static PhotoOutlook ready(PhotoPlan... moments) {
        return new PhotoOutlook(true, Optional.of(CAMERA), true, true, true, List.of(moments));
    }
}
