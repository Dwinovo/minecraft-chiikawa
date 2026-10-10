package com.dwinovo.chiikawa.camera;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.photo.PhotoData;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.photo.PhotoUploads;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;

/**
 * What happens to a pet when the photo it took is back, or is not coming.
 *
 * <p>Back: the shot goes on the roll in its bag that has some room left, or on a roll begun from
 * fresh film, and it rests from photos for the camera's cooldown. Not coming: nothing is put on
 * any roll, and it rests only a little before it tries again. Either way the pet is looked up
 * afresh, since a minute is long enough for it to have wandered off into a chunk nobody has
 * loaded; a pet that is gone is a photo nobody records.
 *
 * <p>A roll that is full is not the pet's to keep: it is a present like any other thing in its
 * bag, and the pet's own idea of when to give one is what sees to it.
 */
public final class PhotoDelivery implements PhotoUploads.Listener {
    private final ServerLevel level;
    private final UUID pet;
    private final CameraSettings camera;
    private final PhotoSubject kind;
    private final String name;
    private final Optional<ResourceLocation> biome;

    /**
     * @param pet who took it
     * @param camera how
     * @param kind what of
     * @param name who is in it, if somebody is
     * @param biome where it was taken
     */
    public PhotoDelivery(AbstractPet pet, CameraSettings camera, PhotoSubject kind, String name,
            Optional<ResourceLocation> biome) {
        this.level = (ServerLevel) pet.level();
        this.pet = pet.getUUID();
        this.camera = camera;
        this.kind = kind;
        this.name = name;
        this.biome = biome;
    }

    @Override
    public void stored(UUID photo) {
        photographer().ifPresent(pet -> {
            PhotoData shot = PhotoData.taken(photo, kind, name, level, pet.getDisplayName().getString());
            // The shot is put on a roll only now that it is kept; if the bag was emptied of film in
            // the meantime there is no roll to put it on, and it is a photo nobody has.
            if (!FilmRolls.record(pet.getBackpack(), PhotoScout.filmOf(camera), camera.exposures(), shot,
                    pet::spawnAtLocation)) {
                failed(photo);
                return;
            }
            pet.getBrain().setMemoryWithExpiry(InitMemory.PHOTO_COOLDOWN.get(), Unit.INSTANCE, camera.cooldownTicks());
            biome.ifPresent(place -> {
                List<ResourceLocation> places = new ArrayList<>(PhotoScout.photographed(pet));
                if (!places.contains(place)) {
                    places.add(place);
                    pet.getBrain().setMemory(InitMemory.PHOTOGRAPHED_BIOMES.get(), places);
                }
            });
        });
    }

    @Override
    public void failed(UUID photo) {
        photographer().ifPresent(pet ->
            pet.getBrain().setMemoryWithExpiry(InitMemory.PHOTO_COOLDOWN.get(), Unit.INSTANCE, camera.retryTicks()));
    }

    private Optional<AbstractPet> photographer() {
        Entity found = level.getEntity(pet);
        return found instanceof AbstractPet photographer && photographer.isAlive()
            ? Optional.of(photographer) : Optional.empty();
    }
}
