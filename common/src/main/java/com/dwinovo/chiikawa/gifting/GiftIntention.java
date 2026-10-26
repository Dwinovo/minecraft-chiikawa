package com.dwinovo.chiikawa.gifting;

import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/**
 * A present a pet has in mind for its owner: what put it there, what it set aside for them if it did, and when it
 * means to give it. A pet holds at most one, see {@link GiftIntentions}.
 *
 * @param occasion what put it in the pet's mind
 * @param keepsake the thing the pet set aside for its owner when it thought of it, a roll of film that is full or a
 *                 flower it found, which is what it gives if it still has it; nothing if it did not set one aside
 * @param readyAt the game time the pet may set off with it
 * @param fadesAt the game time the pet has forgotten it, if it has not given it by then
 */
public record GiftIntention(GiftOccasion occasion, Optional<ItemStack> keepsake, long readyAt, long fadesAt) {
    /** @return whether the pet may set off with it at this game time: it is time, and it has not been forgotten */
    public boolean isReady(long now) {
        return now >= readyAt && now < fadesAt;
    }

    /** @return whether the pet has forgotten it by this game time */
    public boolean hasFaded(long now) {
        return now >= fadesAt;
    }
}
