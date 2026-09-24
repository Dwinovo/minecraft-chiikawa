package com.dwinovo.chiikawa.entity;

import com.dwinovo.chiikawa.entity.brain.PetTargeting;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
    }

    /** The file's own layout, read and written as one tag. */
    private static final Codec<PetRoster> CODEC = CompoundTag.CODEC.xmap(PetRoster::load, PetRoster::save);
    private static final SavedDataType<PetRoster> TYPE = new SavedDataType<>(FILE, PetRoster::new, CODEC, null);

    private final Map<UUID, Map<UUID, Entry>> byOwner = new HashMap<>();

    public static PetRoster of(ServerLevel level) {
        // The overworld's storage, so one roster covers a server rather than one per level.
        return level.getServer().overworld().getDataStorage()
            .computeIfAbsent(TYPE);
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

    static PetRoster load(CompoundTag tag) {
        PetRoster roster = new PetRoster();
        ListTag owners = tag.getListOrEmpty("Owners");
        for (int i = 0; i < owners.size(); i++) {
            CompoundTag ownerTag = owners.getCompoundOrEmpty(i);
            UUID owner = ownerTag.read("Owner", UUIDUtil.CODEC).orElseThrow();
            Map<UUID, Entry> pets = new HashMap<>();
            ListTag petTags = ownerTag.getListOrEmpty("Pets");
            for (int p = 0; p < petTags.size(); p++) {
                CompoundTag petTag = petTags.getCompoundOrEmpty(p);
                UUID pet = petTag.read("Pet", UUIDUtil.CODEC).orElseThrow();
                pets.put(pet, new Entry(pet, petTag.getStringOr("Name", ""),
                    ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                        ResourceLocation.parse(petTag.getStringOr("Dimension", ""))),
                    petTag.read("Pos", BlockPos.CODEC).orElse(BlockPos.ZERO)));
            }
            if (!pets.isEmpty()) {
                roster.byOwner.put(owner, pets);
            }
        }
        return roster;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag owners = new ListTag();
        byOwner.forEach((owner, pets) -> {
            CompoundTag ownerTag = new CompoundTag();
            ownerTag.store("Owner", UUIDUtil.CODEC, owner);
            ListTag petTags = new ListTag();
            for (Entry entry : pets.values()) {
                CompoundTag petTag = new CompoundTag();
                petTag.store("Pet", UUIDUtil.CODEC, entry.pet());
                petTag.putString("Name", entry.name());
                petTag.putString("Dimension", entry.dimension().location().toString());
                petTag.store("Pos", BlockPos.CODEC, entry.pos());
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
