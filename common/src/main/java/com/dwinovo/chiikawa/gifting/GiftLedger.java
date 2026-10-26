package com.dwinovo.chiikawa.gifting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * How many presents each owner was given today, all their pets together, kept with the world so that it holds across
 * a restart and across the owner's pets, loaded or not. A day is a game day, counted from the overworld's clock whatever
 * level the pet is in. Only {@link Presents#give} writes it.
 */
public class GiftLedger extends SavedData {
    /** The file this lives in, beside the world's other saved data. */
    private static final String FILE = "chiikawa_gifts";
    private static final long DAY_TICKS = 24000L;

    /** An owner's count: of which day, and how many presents. */
    private record Count(long day, int given) {
    }

    private final Map<UUID, Count> byOwner = new HashMap<>();

    public static GiftLedger of(ServerLevel level) {
        // The overworld's storage, so one ledger covers a server rather than one per level.
        return level.getServer().overworld().getDataStorage()
            .computeIfAbsent(new SavedData.Factory<>(GiftLedger::new, GiftLedger::load, null), FILE);
    }

    /** @return the game day it is: the overworld's clock over the length of a day */
    public static long today(ServerLevel level) {
        return level.getServer().overworld().getDayTime() / DAY_TICKS;
    }

    /**
     * @param level any level of the server
     * @param owner the owner
     * @return how many more presents the owner may be given today; none without gifting settings, which allow no presents
     */
    public int leftToday(ServerLevel level, UUID owner) {
        return left(owner, today(level), GiftingSettings.current().map(GiftingSettings::ownerDailyLimit).orElse(0));
    }

    /**
     * @param owner the owner
     * @param day a game day
     * @param limit the most presents an owner is given in a day
     * @return how many more presents the owner may be given that day
     */
    public int left(UUID owner, long day, int limit) {
        Count count = byOwner.get(owner);
        return Math.max(0, limit - (count != null && count.day == day ? count.given : 0));
    }

    /** Writes down a present given to the owner on that day. */
    void record(UUID owner, long day) {
        Count count = byOwner.get(owner);
        byOwner.put(owner, count != null && count.day == day ? new Count(day, count.given + 1) : new Count(day, 1));
        setDirty();
    }

    static GiftLedger load(CompoundTag tag, HolderLookup.Provider registries) {
        GiftLedger ledger = new GiftLedger();
        ListTag owners = tag.getList("Owners", Tag.TAG_COMPOUND);
        for (int i = 0; i < owners.size(); i++) {
            CompoundTag owner = owners.getCompound(i);
            ledger.byOwner.put(owner.getUUID("Owner"), new Count(owner.getLong("Day"), owner.getInt("Given")));
        }
        return ledger;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag owners = new ListTag();
        byOwner.forEach((owner, count) -> {
            CompoundTag saved = new CompoundTag();
            saved.putUUID("Owner", owner);
            saved.putLong("Day", count.day);
            saved.putInt("Given", count.given);
            owners.add(saved);
        });
        tag.put("Owners", owners);
        return tag;
    }
}
