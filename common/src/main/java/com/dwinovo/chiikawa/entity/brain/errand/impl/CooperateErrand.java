package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.InvitationDesk;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.Meeting;
import com.dwinovo.chiikawa.entity.brain.errand.step.PlayPart;
import com.dwinovo.chiikawa.entity.brain.errand.step.WaitForGuest;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.social.PetInteraction;
import com.dwinovo.chiikawa.social.PetInteractions;
import com.dwinovo.chiikawa.social.SocialCooldowns;
import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The other side of a scene: having said yes to another pet that asked, stays where it is, turns to it while it
 * comes over and plays its part once it arrives.
 *
 * <p>A pet is only asked while it is pottering about, and saying yes makes it decide again at once. Its priority is
 * above work's, so something that needs doing does not take it away once it has said yes, and the one coming over does
 * not wait for nobody: it waits as long as it said it would, and no longer, and the pet that is waited for goes back to
 * what it was doing when that is over.
 */
public final class CooperateErrand implements ErrandType<CooperateErrand.Hosting, ErrandTunings.Untuned> {
    /**
     * Waiting for the pet that asked, and playing along with it.
     *
     * @param engagement what the pet said yes to
     */
    public record Hosting(InvitationDesk.Engagement engagement) implements Plan {
        @Override
        public Optional<String> subject() {
            return Optional.of(SocialCooldowns.subject(engagement.scene(), engagement.initiator()));
        }

        @Override
        public Optional<ResourceLocation> restKey() {
            return Optional.of(engagement.scene());
        }
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.COOPERATE;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.SOCIAL;
    }

    @Override
    public Codec<ErrandTunings.Untuned> tuningCodec() {
        return ErrandTunings.Untuned.CODEC;
    }

    @Override
    public Offers<Hosting> offers(PetView view, ErrandTunings.Untuned tuning) {
        return view.engagement().map(engagement -> Offers.of(Offer.of(new Hosting(engagement))))
            .orElseGet(() -> Offers.none(Check.fail("not_asked")));
    }

    /** Goes on while the pet that asked is still waited for, or the scene it came for is under way. */
    @Override
    public Check stillWanted(Hosting plan, PetView view, ErrandTunings.Untuned tuning) {
        return view.engagement().filter(held -> held.initiator().equals(plan.engagement().initiator())).isPresent()
            ? Check.OK : Check.fail("not_asked");
    }

    /** The view only offers an invitation to a scene there is, and the mind says yes to no other. */
    @Override
    public Routine routine(Hosting plan, ErrandSettings<ErrandTunings.Untuned> settings) {
        InvitationDesk.Engagement engagement = plan.engagement();
        PetInteraction scene = PetInteractions.all().get(engagement.scene());
        Meeting meeting = Meeting.hosting(engagement.initiator());
        return Routine.of(
            Routine.step(() -> new WaitForGuest(meeting)),
            Routine.step(() -> new PlayPart(meeting, scene.partners().get(engagement.partnerRank()), scene.durationTicks())))
            .releasing(meeting::end);
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.sceneCooldowns();
    }

    @Override
    public Map<ResourceLocation, RestSources.Resolver> restSources() {
        return Scenes.restSources();
    }
}
