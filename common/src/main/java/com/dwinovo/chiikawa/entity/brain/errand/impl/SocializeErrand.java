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
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.Approach;
import com.dwinovo.chiikawa.entity.brain.errand.step.AwaitReply;
import com.dwinovo.chiikawa.entity.brain.errand.step.Invite;
import com.dwinovo.chiikawa.entity.brain.errand.step.Meeting;
import com.dwinovo.chiikawa.entity.brain.errand.step.PlayPart;
import com.dwinovo.chiikawa.entity.brain.errand.step.StartScene;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.view.SocialIdea;
import com.dwinovo.chiikawa.social.PetInteraction;
import com.dwinovo.chiikawa.social.SocialCooldowns;
import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Goes over to another pet and plays a little scene with it, one the pet has thought of on
 * looking around (see {@code PetSocialSensor}).
 *
 * <p>Ranked above pottering about and below any of the pet's errands and work, so a scene
 * fills idle time and never gets in the way of a job. Like the labor board, it starts only
 * on a partner within the anchor's reach and keeps going while the partner stays within
 * the leash: a pet at heel only plays with pets around its owner's feet.
 *
 * <p>A partner that is pottering about is asked first, with the priority of this errand, and plays along only if it
 * has nothing that matters as much; it stops where it is and waits, and nobody else can have it meanwhile. A partner
 * that is playing music is not asked: the pet goes and makes up its audience. Either way the pet walks over, what is
 * handed over in the scene comes out of its own backpack as the two meet, and each plays its part for as long as the
 * scene lasts. Once the scene has begun, however it ends, the two leave that scene alone for its cooldown, each by its
 * own rest; a pet that is turned down, or never answered, is not asked again for a few seconds only.
 */
public final class SocializeErrand implements ErrandType<SocializeErrand.Visit, ErrandTunings.Socialize> {
    /**
     * Going over to the pet a scene is to be played with; the pet is found again each time it is asked where it is.
     *
     * @param idea the scene and the pet
     */
    public record Visit(SocialIdea idea) implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return view.whereIs(idea.partner());
        }

        @Override
        public Optional<String> subject() {
            return Optional.of(SocialCooldowns.subject(idea.scene(), idea.partner()));
        }

        @Override
        public Optional<ResourceLocation> restKey() {
            return Optional.of(idea.scene());
        }
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.SOCIALIZE;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.SOCIAL;
    }

    @Override
    public Codec<ErrandTunings.Socialize> tuningCodec() {
        return ErrandTunings.Socialize.CODEC;
    }

    @Override
    public Offers<Visit> offers(PetView view, ErrandTunings.Socialize tuning) {
        return view.socialIdea().map(idea -> Offers.of(Offer.of(new Visit(idea))))
            .orElseGet(() -> Offers.none(Check.fail("no_partner")));
    }

    @Override
    public Routine routine(Visit plan, ErrandSettings<ErrandTunings.Socialize> settings) {
        SocialIdea idea = plan.idea();
        PetInteraction scene = idea.interaction();
        ErrandTunings.Socialize tuning = settings.tuning();
        Meeting meeting = Meeting.visiting(idea.partner(), scene.partnerState());
        Routine.Part approach = Routine.step(() -> new Approach(meeting, tuning.speed(), scene.approachDistance(),
            tuning.arriveMargin(), scene.reservationTicks()));
        Routine.Part play = Routine.step(() -> new PlayPart(meeting, idea.self(), scene.durationTicks()));
        Routine routine = switch (scene.partnerState()) {
            case IDLE -> Routine.of(
                Routine.step(() -> new Invite(meeting, idea.scene(), idea.partnerRank(), settings.priority(),
                    scene.reservationTicks())),
                Routine.step(() -> new AwaitReply(meeting, tuning.replyTicks())),
                approach,
                Routine.step(() -> new StartScene(meeting, scene)),
                play);
            case PLAYING_MUSIC -> Routine.of(approach, play);
        };
        return routine.releasing(meeting::end);
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
