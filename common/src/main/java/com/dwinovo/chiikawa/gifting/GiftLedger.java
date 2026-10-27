package com.dwinovo.chiikawa.gifting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

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

    /** One owner's count, the way the file lists it. */
    private record Owner(UUID owner, long day, int given) {
        private static final Codec<Owner> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("Owner").forGetter(Owner::owner),
            Codec.LONG.optionalFieldOf("Day", 0L).forGetter(Owner::day),
            Codec.INT.optionalFieldOf("Given", 0).forGetter(Owner::given)
        ).apply(instance, Owner::new));
    }

    /** The ledger as it is saved: every owner with the day and the presents they were given on it. */
    static final Codec<GiftLedger> CODEC = Owner.CODEC.listOf().optionalFieldOf("Owners", List.of()).codec()
        .xmap(GiftLedger::load, GiftLedger::save);
    private static final SavedDataType<GiftLedger> TYPE = new SavedDataType<>(FILE, GiftLedger::new, CODEC, null);

    private final Map<UUID, Count> byOwner = new HashMap<>();

    public static GiftLedger of(ServerLevel level) {
        // The overworld's storage, so one ledger covers a server rather than one per level.
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
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

    private static GiftLedger load(List<Owner> owners) {
        GiftLedger ledger = new GiftLedger();
        for (Owner owner : owners) {
            ledger.byOwner.put(owner.owner(), new Count(owner.day(), owner.given()));
        }
        return ledger;
    }

    private List<Owner> save() {
        List<Owner> owners = new ArrayList<>();
        byOwner.forEach((owner, count) -> owners.add(new Owner(owner, count.day, count.given)));
        return owners;
    }
}
