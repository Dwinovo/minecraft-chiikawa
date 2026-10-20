package com.dwinovo.chiikawa.camera;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.photo.PhotoUploads;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Tells a pet that the photo it took is back, or is not coming. Writes nothing itself: what becomes of the
 * photo, and how long the pet rests after it, is the pet's own mind's to say when it reads what it is told
 * (see {@link PhotoResults}). The pet is looked up afresh, since a minute is long enough for it to have
 * wandered off into a chunk nobody has loaded; a pet that is gone is a photo nobody records.
 *
 * <p>Told on the server thread, as the photo system's uploads are (the platforms hand the payloads over on it),
 * which is where the pet's mind applies what a photo makes durable as it is told.
 */
public final class PhotoDelivery implements PhotoUploads.Listener {
    private final ServerLevel level;
    private final UUID pet;
    private final ResourceLocation camera;
    private final PhotoSubject kind;
    private final String name;
    private final Optional<ResourceLocation> biome;

    /**
     * @param pet who took it
     * @param camera the item it was taken with
     * @param kind what of
     * @param name who is in it, if somebody is
     * @param biome where it was taken
     */
    public PhotoDelivery(AbstractPet pet, ResourceLocation camera, PhotoSubject kind, String name,
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
        photographer().ifPresent(found -> PetMind.post(found, new Message.PhotoResult.Stored(photo, kind, name, biome, camera)));
    }

    @Override
    public void failed(UUID photo) {
        photographer().ifPresent(found -> PetMind.post(found, new Message.PhotoResult.Failed(photo, camera)));
    }

    private Optional<AbstractPet> photographer() {
        Entity found = level.getEntity(pet);
        return found instanceof AbstractPet photographer && photographer.isAlive()
            ? Optional.of(photographer) : Optional.empty();
    }
}
