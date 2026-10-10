package com.dwinovo.chiikawa.camera;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitDataComponents;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.photo.PhotoData;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.photo.PhotoUploads;
import com.dwinovo.chiikawa.shop.Payment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What happens to a pet when the photo it took is back, or is not coming.
 *
 * <p>Back: a roll of film is used up, the pet has a photo to give its owner, and it rests
 * from photos for the camera's cooldown. Not coming: nothing is used up, and it rests only
 * a little before it tries again. Either way the pet is looked up afresh, since a minute is
 * long enough for it to have wandered off into a chunk nobody has loaded; a pet that is
 * gone is a photo nobody comes to hand over.
 *
 * <p>The photo goes the way a present does: into the pet's paws to be taken to its owner if
 * they are empty, and otherwise into its bag, or on the ground at its feet if that is full.
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
            Item film = PhotoScout.filmOf(camera);
            // The film is used up as the photo is kept, not as the shutter goes; if the bag
            // was emptied of it in the meantime there is nothing to use up and no photo.
            if (!Payment.take(pet.getBackpack(), 1, stack -> stack.is(film))) {
                return;
            }
            ItemStack item = new ItemStack(InitItems.PHOTO.get());
            item.set(InitDataComponents.PHOTO.get(),
                PhotoData.taken(photo, kind, name, level, pet.getDisplayName().getString()));
            give(pet, item);
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

    private static void give(AbstractPet pet, ItemStack photo) {
        if (pet.getPendingGift().isEmpty()) {
            pet.setPendingGift(photo);
            return;
        }
        ItemStack left = pet.getBackpack().addItem(photo);
        if (!left.isEmpty()) {
            pet.spawnAtLocation(left);
        }
    }
}
