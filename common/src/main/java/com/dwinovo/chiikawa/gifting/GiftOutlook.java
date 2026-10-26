package com.dwinovo.chiikawa.gifting;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.entity.job.JobWork;
import com.dwinovo.chiikawa.entity.job.WorkItems;
import com.dwinovo.chiikawa.init.InitRegistry;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Whether a pet could set off with a present for its owner now, and with what: the present it has in mind is ready, its
 * owner may be given one more today, and it has something to give. What the gift errand is offered.
 *
 * @param ready whether the pet has a present in mind that it may set off with, see {@link GiftIntentions#ready}
 * @param leftToday how many more presents its owner may be given today, all their pets together
 * @param picks what it may give, see {@link GiftChoice}; none while it is not ready or its owner may be given no more
 */
public record GiftOutlook(boolean ready, int leftToday, List<GiftChoice.Pick> picks) {
    /** A pet with no present in mind, or no owner to give it to. */
    public static final GiftOutlook NONE = new GiftOutlook(false, 0, List.of());

    public GiftOutlook {
        picks = List.copyOf(picks);
    }

    /** @return what the pet could give now; nothing without settings, an owner in the pet's level or a present in mind */
    public static GiftOutlook of(AbstractPet pet) {
        Optional<GiftingSettings> settings = GiftingSettings.current();
        if (settings.isEmpty() || !(pet.level() instanceof ServerLevel level) || !(pet.getOwner() instanceof Player owner)) {
            return NONE;
        }
        Optional<GiftIntention> intention = pet.giftIntentions().ready(level.getGameTime());
        if (intention.isEmpty()) {
            return NONE;
        }
        int left = GiftLedger.of(level).leftToday(level, owner.getUUID());
        List<GiftChoice.Pick> picks = left <= 0 ? List.of() : GiftChoice.of(intention.get(), pet.getBackpack(),
            BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType()), PetPersonalities.of(pet.getType()), settings.get(),
            GiftChoice.never(settings.get(), WorkItems.of(InitRegistry.getCapabilityFromId(pet.getPetJobId()), JobWork.errandsOf(pet))));
        return new GiftOutlook(true, left, picks);
    }
}
