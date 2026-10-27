package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.PetCharacters;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings.Rests;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Destination;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Pastime;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.PastimeCue;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Stage;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSpec;
import com.dwinovo.chiikawa.entity.brain.errand.view.Weather;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkActions;
import com.dwinovo.chiikawa.social.Beat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;

/**
 * The generated pastimes (0.1.4 design, section 6 and the first batch of section 11): the little things a pet does for a
 * reason, between the things that matter, each as its character does them. A pastime is a work with a cast, so it is written
 * beside the works, see {@link WorkData}, and its settings are written beside theirs, see {@link ErrandSettingsData}: both
 * from here, so that what a pastime is and how it goes are said in one place.
 *
 * <p>These are the pastimes every character has a part in. Those of one character are its own, and written with it.
 * Priority 10, where a pet's idle time is: a pet does one when the reason is there and nothing that matters more is, and
 * it takes the place of wandering. Each rests for its cooldown from when it was done, which the world's setting for how
 * often pets pass the time scales.
 */
public final class PastimeData {
    /** Getting out of the rain: under a roof or a tree, or, for Chiikawa, close to the owner. */
    public static final ResourceLocation RAIN_SHELTER = id("rain_shelter");
    /** Usagi, out in the rain and enjoying it. */
    public static final ResourceLocation RAIN_PLAY = id("rain_play");
    /** Coming to see the owner at work, and cheering. */
    public static final ResourceLocation WATCH_OWNER_WORK = id("watch_owner_work");

    /** The pastimes, in the order a pet weighs them. */
    private static final List<ResourceLocation> ALL = List.of(RAIN_SHELTER, RAIN_PLAY, WATCH_OWNER_WORK);

    /** A pet sits out of the rain for half a minute, and plays in it for a quarter; it watches its owner at work for ten seconds. */
    private static final int SHELTERING_TICKS = 600;
    private static final int PLAYING_TICKS = 300;
    private static final int WATCHING_TICKS = 200;
    /** How long a pastime may take, the walk to where it is done included: the time it lasts and as long again. */
    private static final int SHELTERING_LIMIT = 1200;
    private static final int PLAYING_LIMIT = 600;
    private static final int WATCHING_LIMIT = 600;
    /** How long a pet leaves a pastime alone once it has done it, in a world that has pastimes as often as this says: a minute for the rain, two for watching. */
    private static final int RAIN_COOLDOWN = 1200;
    private static final int WATCHING_COOLDOWN = 2400;
    /** How far off the owner may be for a pet to come over and watch. */
    private static final double WATCHING_RANGE = 16.0;
    private static final float WALK_SPEED = 0.8F;
    /** How near a pet has to be to a place to have got there: in the block, which is as near as the middle of it to a corner. */
    private static final double PLACE_REACH = 1.0;
    /** How long a pet that cannot get to where a pastime is done leaves it alone: ten seconds. */
    private static final int GAVE_UP_REST = 200;

    private PastimeData() {
    }

    /** @return the id of every pastime here, in the order a pet weighs them */
    public static List<ResourceLocation> ids() {
        return ALL;
    }

    /** @return the pastimes, as works, by the id of the errand each is */
    public static Map<ResourceLocation, Work<?, ?>> works() {
        Map<ResourceLocation, Work<?, ?>> works = new HashMap<>();

        // It rains on the pet where it stands. Chiikawa goes to the owner and huddles by them, as it does when it is afraid; the
        // rest go and sit where it does not rain. Usagi does not, and plays in it instead, see RAIN_PLAY.
        Map<ResourceLocation, Stage> sheltering = new HashMap<>();
        for (ResourceLocation character : PetCharacters.all()) {
            if (character.equals(PetCharacters.USAGI)) {
                continue;
            }
            boolean huddles = character.equals(PetCharacters.CHIIKAWA);
            sheltering.put(character, new Stage(Optional.of(huddles ? new Destination.ToOwner() : new Destination.ToShelter()),
                Optional.of(huddles ? "huddle,sit" : "sit"), Optional.empty(), Optional.empty(), Optional.empty(), SHELTERING_TICKS));
        }
        works.put(RAIN_SHELTER, new Work<>(WorkActions.VISIT, ErrandCategory.PASTIME, Optional.empty(), List.of(), Optional.empty(),
            Optional.of(new Pastime(List.of(new PastimeCue.WeatherIs(Weather.RAIN), new PastimeCue.UnderSky()), sheltering))));

        // Usagi jumps about in it, and, as Usagi never cries, there is nothing in the cast to make it.
        Beat jump = new Beat(Optional.of("jump"), Optional.empty(), Optional.empty());
        works.put(RAIN_PLAY, new Work<>(WorkActions.PERFORM, ErrandCategory.PASTIME, Optional.empty(), List.of(), Optional.empty(),
            Optional.of(new Pastime(List.of(new PastimeCue.WeatherIs(Weather.RAIN), new PastimeCue.UnderSky()),
                Map.of(PetCharacters.USAGI, new Stage(Optional.empty(), Optional.empty(),
                    Optional.of(new Beat(Optional.empty(), Optional.of(PetReaction.HAPPY), Optional.empty())),
                    Optional.of(new Beat.Recurring(UniformInt.of(20, 50), jump)), Optional.empty(), PLAYING_TICKS))))));

        // Everyone comes over to the owner at work and claps, as often as the owner swings.
        Map<ResourceLocation, Stage> watching = new HashMap<>();
        Beat clap = new Beat(Optional.of("clap"), Optional.empty(), Optional.empty());
        for (ResourceLocation character : PetCharacters.all()) {
            watching.put(character, new Stage(Optional.of(new Destination.ToOwner()), Optional.empty(), Optional.of(clap),
                Optional.of(new Beat.Recurring(UniformInt.of(40, 80), clap)), Optional.empty(), WATCHING_TICKS));
        }
        works.put(WATCH_OWNER_WORK, new Work<>(WorkActions.VISIT, ErrandCategory.PASTIME, Optional.empty(), List.of(),
            Optional.empty(), Optional.of(new Pastime(
                List.of(new PastimeCue.OwnerIs(PastimeCue.OwnerState.WORKING, WATCHING_RANGE)), watching))));
        return works;
    }

    /** @return the settings of the pastimes, by the id of the errand each is */
    public static Map<ResourceLocation, ErrandSettings<?>> settings() {
        // Done, it rests for its cooldown, which the file of the pastime holds and the world's setting scales; given up, for
        // ten seconds, so that a place that cannot be reached is not tried again in the same breath.
        Rests rests = Rests.of(RestSpec.from(RestSources.PASTIME_COOLDOWN), RestSpec.ticks(GAVE_UP_REST), RestSpec.NONE);
        return Map.of(
            RAIN_SHELTER, settings(0.5F, SHELTERING_LIMIT, rests, new ErrandTunings.Visit(RAIN_COOLDOWN, WALK_SPEED, 1, 1.5, PLACE_REACH)),
            RAIN_PLAY, settings(0.5F, PLAYING_LIMIT, rests, new ErrandTunings.Perform(RAIN_COOLDOWN)),
            WATCH_OWNER_WORK, settings(0.3F, WATCHING_LIMIT, rests,
                new ErrandTunings.Visit(WATCHING_COOLDOWN, WALK_SPEED, 2, 3.0, PLACE_REACH)));
    }

    private static <T> ErrandSettings<T> settings(float score, int maxTicks, Rests rests, T tuning) {
        return new ErrandSettings<>(10, score, maxTicks, rests, List.of(), List.of(), Optional.empty(), tuning);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
