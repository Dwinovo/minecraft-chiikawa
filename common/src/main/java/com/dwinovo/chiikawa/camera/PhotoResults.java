package com.dwinovo.chiikawa.camera;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.gifting.GiftOccasion;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.item.FilmItem;
import com.dwinovo.chiikawa.photo.PhotoData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * What happens to a pet when the photo it took is back, or is not coming.
 *
 * <p>Back: the shot goes on the roll in its bag that has some room left, or on a roll begun from fresh
 * film, and the biome it was taken in is one the pet has photographed. Not coming: nothing is put on any
 * roll. How long the pet rests after either is the rest ledger's, by the report this gives.
 *
 * <p>A roll that is full is not the pet's to keep: it is something to show its owner, and the pet's mind is told so,
 * see {@code GiftOccasion}.
 */
public final class PhotoResults {
    /** The report of a shutter that has gone, as the errand's settings name it: the picture is now being waited for. */
    public static final String TAKEN = "taken";
    /** The report of a photo that was kept, as the errand's settings name it. */
    public static final String STORED = "stored";
    /** The report of a photo that was not, likewise. */
    public static final String FAILED = "failed";

    private PhotoResults() {
    }

    /**
     * Carries out what a pet is told of its photo.
     *
     * @return the report to rest by: a photo that came back but has no roll to go on is one that did not
     */
    public static String received(AbstractPet pet, Message.PhotoResult result) {
        return switch (result) {
            case Message.PhotoResult.Stored stored -> keep(pet, stored) ? STORED : FAILED;
            case Message.PhotoResult.Failed failed -> FAILED;
        };
    }

    /**
     * The shot is put on a roll only now that it is kept; if the bag was emptied of film in the meantime
     * there is no roll to put it on, and it is a photo nobody has.
     */
    private static boolean keep(AbstractPet pet, Message.PhotoResult.Stored stored) {
        Optional<CameraSettings> camera = CameraSettings.of(stored.camera());
        if (camera.isEmpty()) {
            return false;
        }
        PhotoData shot = PhotoData.taken(stored.photo(), stored.kind(), stored.name(), pet.level(),
            pet.getDisplayName().getString());
        Optional<ItemStack> roll = FilmRolls.record(pet.getBackpack(), PhotoScout.filmOf(camera.get()), camera.get().exposures(),
            shot, pet::spawnAtLocation);
        if (roll.isEmpty()) {
            return false;
        }
        if (FilmItem.rollOf(roll.get()).full()) {
            // The roll is finished, which is something to show the owner.
            PetMind.post(pet, new Message.Occasion(GiftOccasion.FILM_FULL, Optional.of(roll.get().copy())));
        }
        stored.biome().ifPresent(place -> {
            List<ResourceLocation> places = new ArrayList<>(PhotoScout.photographed(pet));
            if (!places.contains(place)) {
                places.add(place);
                pet.getBrain().setMemory(InitMemory.PHOTOGRAPHED_BIOMES.get(), places);
            }
        });
        return true;
    }
}
