package com.dwinovo.chiikawa.camera;

import com.dwinovo.chiikawa.photo.PhotoSubject;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * One photo a pet could take, as plain data: of what, of whom, and with which camera. Made by {@link PhotoScout}
 * for every good moment there is as the pet decides what to do, so the pet's mind can ask whether it may go there
 * like any other errand, and held by the errand that carries the one it chose out.
 *
 * @param camera the id of the camera item it is taken with, which says how the camera behaves and how long the pet
 *               rests after it
 * @param kind the kind of photo
 * @param subject who is in it, when somebody is
 * @param name who is in it, by name, for the photo's caption; nothing for a view
 * @param facing which way a pet that has nobody to turn to faces: the sun when it is low, or as it was looking
 */
public record PhotoPlan(ResourceLocation camera, PhotoSubject kind, Optional<UUID> subject, String name, Vec3 facing) {
}
