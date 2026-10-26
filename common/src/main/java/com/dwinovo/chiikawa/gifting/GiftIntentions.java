package com.dwinovo.chiikawa.gifting;

import com.dwinovo.chiikawa.Constants;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * The present a pet has in mind, if it has one: kept by the pet and saved with it, in its own NBT and not in its
 * brain, whose memories a save of an older version must not have an unknown one of. A pet thinks of one present at
 * a time, because something happened that made it think of one, see {@link GiftOccasion}.
 *
 * <p>Only the pet's mind notes one, as it is told of the occasion, see {@code PetMind#keep}, and only the step that
 * takes the present out of the bag consumes it, see {@code TakePresent}. What the pet makes of it in between is
 * read: it is ready to give it when its time has come and it has not forgotten it by then.
 */
public final class GiftIntentions {
    private static final String TAG = "GiftIntention";
    private static final String OCCASION = "Occasion";
    private static final String KEEPSAKE = "Keepsake";
    private static final String READY_AT = "ReadyAt";
    private static final String FADES_AT = "FadesAt";

    private Optional<GiftIntention> held = Optional.empty();

    /** @return the present the pet has in mind, forgotten or not */
    public Optional<GiftIntention> held() {
        return held;
    }

    /** @return the present the pet has in mind that it may set off with at this game time */
    public Optional<GiftIntention> ready(long now) {
        return held().filter(intention -> intention.isReady(now));
    }

    /**
     * The pet thinks of a present because of an occasion, if it is to: not while it has one in mind that it has not
     * forgotten, not if the occasion never puts one there, not if it would be resting from giving one when it is
     * ready to, and then by the chance the settings give the occasion.
     *
     * @param occasion what happened
     * @param keepsake the thing the pet sets aside for its owner because of it, if it does
     * @param now the game time
     * @param restLeft how long the pet is resting from giving a present for, from now
     * @param random the pet's own, for the chance
     * @param settings how pets give presents
     * @return whether the pet now has the present in mind
     */
    public boolean note(GiftOccasion occasion, Optional<ItemStack> keepsake, long now, long restLeft, RandomSource random,
            GiftingSettings settings) {
        Optional<GiftingSettings.OccasionSettings> how = settings.occasion(occasion);
        if (how.isEmpty() || held.filter(intention -> !intention.hasFaded(now)).isPresent()) {
            return false;
        }
        long readyAt = now + how.get().delayTicks();
        if (restLeft > readyAt - now || random.nextFloat() >= how.get().chance()) {
            return false;
        }
        held = Optional.of(new GiftIntention(occasion, keepsake.map(ItemStack::copy), readyAt, readyAt + settings.fadeTicks()));
        return true;
    }

    /** The pet has taken the present out of its bag to give: it has it in mind no longer. */
    public void consume() {
        held = Optional.empty();
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        held.ifPresent(intention -> {
            CompoundTag saved = new CompoundTag();
            saved.putString(OCCASION, intention.occasion().getSerializedName());
            intention.keepsake().ifPresent(keepsake -> saved.put(KEEPSAKE, keepsake.save(registries)));
            saved.putLong(READY_AT, intention.readyAt());
            saved.putLong(FADES_AT, intention.fadesAt());
            tag.put(TAG, saved);
        });
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        held = Optional.empty();
        if (!tag.contains(TAG, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag saved = tag.getCompound(TAG);
        GiftOccasion occasion = GiftOccasion.CODEC.byName(saved.getString(OCCASION));
        if (occasion == null) {
            Constants.LOG.warn("[chiikawa-gifting] a pet's present in mind is for an occasion nobody knows: {}", saved.getString(OCCASION));
            return;
        }
        held = Optional.of(new GiftIntention(occasion,
            saved.contains(KEEPSAKE, Tag.TAG_COMPOUND) ? ItemStack.parse(registries, saved.getCompound(KEEPSAKE)) : Optional.empty(),
            saved.getLong(READY_AT), saved.getLong(FADES_AT)));
    }
}
