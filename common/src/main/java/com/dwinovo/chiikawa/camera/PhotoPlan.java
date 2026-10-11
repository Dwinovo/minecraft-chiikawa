package com.dwinovo.chiikawa.camera;

import com.dwinovo.chiikawa.photo.PhotoSubject;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.phys.Vec3;

/**
 * What a pet has decided to take a photo of, as plain data: chosen when the pet decides what to do,
 * so the selector can ask whether it may go there like any other errand, and kept in the pet's
 * memory for the behavior to carry out.
 *
 * @param kind the kind of photo
 * @param subject who is in it, when somebody is
 * @param name who is in it, by name, for the photo's caption; nothing for a view
 * @param facing which way a pet that has nobody to turn to faces: the sun when it is low, or as it was looking
 * @param where where the pet has to go to take it: to the subject, and nowhere for a view, which is taken
 *              from where it stands
 */
public record PhotoPlan(PhotoSubject kind, Optional<UUID> subject, String name, Vec3 facing, Optional<GlobalPos> where) {
    /** The same plan with its subject somewhere else: somebody moves between one look and the next. */
    public PhotoPlan at(GlobalPos position) {
        return new PhotoPlan(kind, subject, name, facing, Optional.of(position));
    }
}
