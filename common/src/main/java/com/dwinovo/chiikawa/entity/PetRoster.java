package com.dwinovo.chiikawa.entity;

import com.dwinovo.chiikawa.entity.brain.PetTargeting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Where each owner's pets were last seen. Kept on the server rather than read off the
 * world, because the pets worth asking about are exactly the ones nobody has loaded: an
 * entity in an unloaded chunk is not in memory to be looked at, and its owner still wants
 * to know it is three thousand blocks away in the nether rather than gone.
 *
 * <p>Written while a pet ticks and once more as its chunk is put away, so an entry is the
 * last thing anyone saw, which is all a bell needs to go and fetch it.
 */
public class PetRoster extends SavedData {
    /** The file this lives in, beside the world's other saved data. */
    private static final String FILE = "chiikawa_pet_roster";

    /**
     * One pet, as last seen.
     *
     * @param name what to call it in a message to its owner
     * @param dimension which level it was in
     * @param pos where it stood
     */
    public record Entry(UUID pet, String name, ResourceKey<Level> dimension, BlockPos pos) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("Pet").forGetter(Entry::pet),
            Codec.STRING.fieldOf("Name").forGetter(Entry::name),
            Level.RESOURCE_KEY_CODEC.fieldOf("Dimension").forGetter(Entry::dimension),
            BlockPos.CODEC.fieldOf("Pos").forGetter(Entry::pos)
        ).apply(instance, Entry::new));
    }

    /** One owner's pets, the way the file lists them. */
    private record Owner(UUID owner, List<Entry> pets) {
        private static final Codec<Owner> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("Owner").forGetter(Owner::owner),
            Entry.CODEC.listOf().fieldOf("Pets").forGetter(Owner::pets)
        ).apply(instance, Owner::new));
    }

    /** The roster as it is saved: every owner, with where each of their pets was last seen. */
    static final Codec<PetRoster> CODEC = Owner.CODEC.listOf().optionalFieldOf("Owners", List.of()).codec()
        .xmap(PetRoster::load, PetRoster::save);
    private static final SavedDataType<PetRoster> TYPE = new SavedDataType<>(FILE, PetRoster::new, CODEC, null);

    private final Map<UUID, Map<UUID, Entry>> byOwner = new HashMap<>();

    public static PetRoster of(ServerLevel level) {
        // The overworld's storage, so one roster covers a server rather than one per level.
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Remembers where this pet is now. */
    public void note(AbstractPet pet) {
        UUID owner = PetTargeting.ownerId(pet);
        if (owner == null) {
            return;
        }
        put(owner, new Entry(pet.getUUID(), pet.getDisplayName().getString(),
            pet.level().dimension(), pet.blockPosition()));
    }

    /** Writes one entry down; the seam a case can reach without a live pet. */
    void put(UUID owner, Entry entry) {
        byOwner.computeIfAbsent(owner, key -> new HashMap<>()).put(entry.pet(), entry);
        setDirty();
    }

    /** Forgets a pet that has died, or that turned out not to be where it was noted. */
    public void forget(UUID owner, UUID pet) {
        Map<UUID, Entry> pets = byOwner.get(owner);
        if (pets != null && pets.remove(pet) != null) {
            if (pets.isEmpty()) {
                byOwner.remove(owner);
            }
            setDirty();
        }
    }

    /** @return everything known about this owner's pets, in no particular order */
    public List<Entry> pets(UUID owner) {
        return List.copyOf(byOwner.getOrDefault(owner, Map.of()).values());
    }

    private static PetRoster load(List<Owner> owners) {
        PetRoster roster = new PetRoster();
        for (Owner owner : owners) {
            Map<UUID, Entry> pets = new HashMap<>();
            for (Entry entry : owner.pets()) {
                pets.put(entry.pet(), entry);
            }
            if (!pets.isEmpty()) {
                roster.byOwner.put(owner.owner(), pets);
            }
        }
        return roster;
    }

    private List<Owner> save() {
        List<Owner> owners = new ArrayList<>();
        byOwner.forEach((owner, pets) -> owners.add(new Owner(owner, List.copyOf(pets.values()))));
        return owners;
    }

    /** @return the owners this roster knows anything about, for tests and commands */
    public List<UUID> owners() {
        return new ArrayList<>(byOwner.keySet());
    }
}
