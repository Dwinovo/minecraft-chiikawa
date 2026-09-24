package com.dwinovo.chiikawa.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

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
    }

    private final Map<UUID, Map<UUID, Entry>> byOwner = new HashMap<>();

    public static PetRoster of(ServerLevel level) {
        // The overworld's storage, so one roster covers a server rather than one per level.
        return level.getServer().overworld().getDataStorage()
            .computeIfAbsent(new SavedData.Factory<>(PetRoster::new, PetRoster::load, null), FILE);
    }

    /** Remembers where this pet is now. */
    public void note(AbstractPet pet) {
        UUID owner = pet.getOwnerUUID();
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

    static PetRoster load(CompoundTag tag) {
        PetRoster roster = new PetRoster();
        ListTag owners = tag.getList("Owners", Tag.TAG_COMPOUND);
        for (int i = 0; i < owners.size(); i++) {
            CompoundTag ownerTag = owners.getCompound(i);
            UUID owner = ownerTag.getUUID("Owner");
            Map<UUID, Entry> pets = new HashMap<>();
            ListTag petTags = ownerTag.getList("Pets", Tag.TAG_COMPOUND);
            for (int p = 0; p < petTags.size(); p++) {
                CompoundTag petTag = petTags.getCompound(p);
                UUID pet = petTag.getUUID("Pet");
                pets.put(pet, new Entry(pet, petTag.getString("Name"),
                    ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                        new ResourceLocation(petTag.getString("Dimension"))),
                    NbtUtils.readBlockPos(petTag.getCompound("Pos"))));
            }
            if (!pets.isEmpty()) {
                roster.byOwner.put(owner, pets);
            }
        }
        return roster;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag owners = new ListTag();
        byOwner.forEach((owner, pets) -> {
            CompoundTag ownerTag = new CompoundTag();
            ownerTag.putUUID("Owner", owner);
            ListTag petTags = new ListTag();
            for (Entry entry : pets.values()) {
                CompoundTag petTag = new CompoundTag();
                petTag.putUUID("Pet", entry.pet());
                petTag.putString("Name", entry.name());
                petTag.putString("Dimension", entry.dimension().location().toString());
                petTag.put("Pos", NbtUtils.writeBlockPos(entry.pos()));
                petTags.add(petTag);
            }
            ownerTag.put("Pets", petTags);
            owners.add(ownerTag);
        });
        tag.put("Owners", owners);
        return tag;
    }

    /** @return the owners this roster knows anything about, for tests and commands */
    public List<UUID> owners() {
        return new ArrayList<>(byOwner.keySet());
    }
}
