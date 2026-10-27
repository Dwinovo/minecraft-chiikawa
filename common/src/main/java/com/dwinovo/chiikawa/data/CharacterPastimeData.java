package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.PetCharacters;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings.Rests;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Destination;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Pastime;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.PastimeCue;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Stage;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkActions;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.social.Beat;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import com.mojang.datafixers.util.Either;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;

/**
 * The generated pastimes of one character each (0.1.4 design, the first batch of section 11): the thing that is most
 * itself, as the series draws it, which no other pet does. They are works with a cast of one, written as the general
 * pastimes are, see {@link PastimeData}, which has them with the rest wherever the pastimes are asked for.
 *
 * <p>Chiikawa shrinks away from a storm or a creeper about to go off and cries until its owner comes; Usagi has its fits and
 * never cries; Momonga copies a pet it sees and asks its owner to say how cute it is, and sulks if nobody does; Kurimanju has
 * a drink as the day ends and a word and a coffee for an owner who has finished a job of work; Rakko stares at something
 * sweet and practises with its sword; Shisa welcomes its owner back with a bow. What each says is its voice's, see
 * {@link PetVoiceData}; a pastime only names the moment.
 *
 * <p>Priority 10, where a pet's idle time is. Each is done for a reason of its own but two, Usagi's fits and Rakko's practice,
 * which are done for none and are rare because they rest a long time after: a pastime rests for its cooldown from when it was
 * done.
 */
public final class CharacterPastimeData {
    /** Chiikawa, frightened by a storm or a creeper about to go off. */
    public static final ResourceLocation COWER = id("cower");
    /** Usagi's fits: a yell and a scamper. */
    public static final ResourceLocation FRENZY = id("frenzy");
    /** Momonga, copying a pet and asking its owner for praise. */
    public static final ResourceLocation ASK_PRAISE = id("ask_praise");
    /** Kurimanju, with a drink as the day ends. */
    public static final ResourceLocation EVENING_DRINK = id("evening_drink");
    /** Kurimanju, bringing its owner a coffee after a long job of work. */
    public static final ResourceLocation COFFEE_FOR_A_WORKER = id("coffee_for_a_worker");
    /** Rakko, drawn to something sweet. */
    public static final ResourceLocation SWEETS_STARE = id("sweets_stare");
    /** Rakko, with its sword, on its own. */
    public static final ResourceLocation SWORD_PRACTICE = id("sword_practice");
    /** Shisa, welcoming its owner back. */
    public static final ResourceLocation GREET_OWNER = id("greet_owner");

    /** The pastimes, in the order a pet weighs them. */
    private static final List<ResourceLocation> ALL = List.of(COWER, FRENZY, ASK_PRAISE, EVENING_DRINK, COFFEE_FOR_A_WORKER,
        SWEETS_STARE, SWORD_PRACTICE, GREET_OWNER);

    /** Chiikawa cries for half a minute at the most, and not again for two minutes. */
    private static final int COWERING_TICKS = 600;
    private static final int COWERING_COOLDOWN = 2400;
    /** It is comforted once its owner is this near, and not before it has shown it is afraid. */
    private static final double COMFORT_RANGE = 2.0;
    private static final int COMFORT_AFTER = 40;
    /** The fright is said again now and then: more than the voice's own wait between two lines, or it is not heard. */
    private static final UniformInt CRIES = UniformInt.of(200, 300);
    /** A fit lasts three seconds, and the next is ten minutes off. */
    private static final int FRENZY_TICKS = 60;
    private static final int FRENZY_COOLDOWN = 12000;
    private static final UniformInt SCAMPERS = UniformInt.of(15, 30);
    /** Momonga asks for twenty seconds, copies for three, and wants praise every four minutes. */
    private static final int ASKING_TICKS = 400;
    private static final int COPYING_TICKS = 60;
    private static final int ASKING_COOLDOWN = 4800;
    private static final UniformInt PLEAS = UniformInt.of(100, 160);
    /** A pet has to be this near for Momonga to copy it, and the owner this near to be asked. */
    private static final double PET_RANGE = 8.0;
    private static final double ASKED_RANGE = 16.0;
    /** The drink lasts six seconds, and there is one an evening, which is a day apart. */
    private static final int DRINKING_TICKS = 120;
    private static final int DRINKING_COOLDOWN = 24000;
    /** A coffee is handed over in three seconds, at most every ten minutes. */
    private static final int HANDING_TICKS = 60;
    private static final int HANDING_COOLDOWN = 12000;
    /** Rakko stares at something sweet for six seconds, and practises for eight; it is drawn to sweets every five minutes, practises every fifteen. */
    private static final int STARING_TICKS = 120;
    private static final int STARING_COOLDOWN = 6000;
    private static final int PRACTISING_TICKS = 160;
    private static final int PRACTISING_COOLDOWN = 18000;
    /** Shisa bows for three seconds, and welcomes its owner again after a minute. */
    private static final int GREETING_TICKS = 60;
    private static final int GREETING_COOLDOWN = 1200;
    /** How far the owner may be for Shisa to come and greet them, and Kurimanju to come with a coffee. */
    private static final double GREETED_RANGE = 16.0;
    private static final double HANDED_RANGE = 16.0;
    /** How long each may take, the walk to where it is done included. */
    private static final int COWERING_LIMIT = 700;
    private static final int FRENZY_LIMIT = 200;
    private static final int ASKING_LIMIT = 1500;
    private static final int DRINKING_LIMIT = 300;
    private static final int HANDING_LIMIT = 600;
    private static final int STARING_LIMIT = 600;
    private static final int PRACTISING_LIMIT = 300;
    private static final int GREETING_LIMIT = 600;
    private static final float WALK_SPEED = 0.8F;
    /** How near a pet has to be to what it walks to for the walk to be done: next to the owner, in the block of a thing lying about. */
    private static final int ARRIVE = 2;
    private static final double OWNER_REACH = 2.5;
    private static final double PLACE_REACH = 1.0;

    private CharacterPastimeData() {
    }

    /** @return the id of every pastime here, in the order a pet weighs them */
    public static List<ResourceLocation> ids() {
        return ALL;
    }

    /** @return the pastimes, as works, by the id of the errand each is */
    public static Map<ResourceLocation, Work<?, ?>> works() {
        Map<ResourceLocation, Work<?, ?>> works = new HashMap<>();

        // A storm or a creeper that is about to go off: it hunches and cries, and is better once its owner is by it.
        Beat fright = new Beat(Optional.empty(), Optional.of(PetReaction.HURT), Optional.of(VoiceMoment.SCARED));
        Beat cry = new Beat(Optional.empty(), Optional.empty(), Optional.of(VoiceMoment.SCARED));
        works.put(COWER, pastime(WorkActions.PERFORM,
            List.of(new PastimeCue.ScaredBy(Set.of(PastimeCue.Fright.THUNDER, PastimeCue.Fright.CREEPER))),
            PetCharacters.CHIIKAWA, new Stage(Optional.empty(), Optional.of("cry,hurt"), Optional.of(fright),
                Optional.of(new Beat.Recurring(CRIES, cry)), Optional.empty(),
                Optional.of(new Stage.Until(new PastimeCue.OwnerIs(PastimeCue.OwnerState.NEAR, COMFORT_RANGE), COMFORT_AFTER)),
                COWERING_TICKS)));

        // Out of the blue, with no reason to give: it yells its nonsense and scampers on the spot. It is not the sort to cry.
        Beat scamper = new Beat(Optional.of("jump"), Optional.empty(), Optional.empty());
        works.put(FRENZY, pastime(WorkActions.PERFORM, List.of(), PetCharacters.USAGI, new Stage(Optional.empty(),
            Optional.of("frenzy"), Optional.of(new Beat(Optional.empty(), Optional.of(PetReaction.HAPPY), Optional.of(VoiceMoment.FRENZY))),
            Optional.of(new Beat.Recurring(SCAMPERS, scamper)), Optional.empty(), Optional.empty(), FRENZY_TICKS)));

        // It sees another pet, copies it, goes to its owner and asks to be told how cute it is, and the owner's click, or a
        // friend's, is the telling. It asks once in words and goes on being cute while it waits, so that its words at the end are
        // not lost to the quiet a pet keeps after speaking. Nobody does, and it throws a fit.
        works.put(ASK_PRAISE, pastime(WorkActions.ASK_PRAISE, List.of(
                new PastimeCue.Sees(Either.left(InitTag.PETS), PET_RANGE), new PastimeCue.OwnerIs(PastimeCue.OwnerState.NEAR, ASKED_RANGE)),
            PetCharacters.MOMONGA, new Stage(Optional.empty(), Optional.of("ask_praise"),
                Optional.of(new Beat(Optional.empty(), Optional.empty(), Optional.of(VoiceMoment.PRAISE_ME))),
                Optional.of(new Beat.Recurring(PLEAS, new Beat(Optional.of("cute"), Optional.empty(), Optional.empty()))),
                Optional.of(new Beat(Optional.of("tantrum"), Optional.of(PetReaction.HURT), Optional.of(VoiceMoment.TURNED_DOWN))),
                Optional.empty(), ASKING_TICKS)));

        // As the day ends it sits down with a drink, and lets out a breath.
        works.put(EVENING_DRINK, pastime(WorkActions.PERFORM, List.of(new PastimeCue.InPhase(Set.of(DayPhase.EVENING))),
            PetCharacters.KURIMANJU, new Stage(Optional.empty(), Optional.of("drink,sit"), Optional.empty(), Optional.empty(),
                Optional.of(new Beat(Optional.of("sigh"), Optional.empty(), Optional.of(VoiceMoment.SIGH))), Optional.empty(),
                DRINKING_TICKS)));

        // Its owner has just finished a long job of work: a kind word, and the hand-over, without the coffee, which is a thing
        // for a later version to give.
        works.put(COFFEE_FOR_A_WORKER, pastime(WorkActions.VISIT,
            List.of(new PastimeCue.OwnerIs(PastimeCue.OwnerState.FINISHED, HANDED_RANGE)),
            PetCharacters.KURIMANJU, new Stage(Optional.of(new Destination.ToOwner()), Optional.of("hand_over"),
                Optional.of(new Beat(Optional.empty(), Optional.empty(), Optional.of(VoiceMoment.OFFER_COFFEE))), Optional.empty(),
                Optional.empty(), Optional.empty(), HANDING_TICKS)));

        // Something sweet on the ground or in a hand: it goes over and stares.
        works.put(SWEETS_STARE, pastime(WorkActions.VISIT, List.of(new PastimeCue.Sees(Either.right(InitTag.SWEETS), PET_RANGE)),
            PetCharacters.RAKKO, new Stage(Optional.of(new Destination.ToItem(InitTag.SWEETS)), Optional.of("stare,sit"),
                Optional.of(new Beat(Optional.empty(), Optional.empty(), Optional.of(VoiceMoment.SWEETS))), Optional.empty(),
                Optional.empty(), Optional.empty(), STARING_TICKS)));

        // On its own, with no one to see, swings its sword: to be stronger.
        works.put(SWORD_PRACTICE, pastime(WorkActions.PERFORM, List.of(), PetCharacters.RAKKO, new Stage(Optional.empty(),
            Optional.of("practice,sword_attack"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            PRACTISING_TICKS)));

        // Its owner is back after a while away: it comes over and bows.
        works.put(GREET_OWNER, pastime(WorkActions.VISIT, List.of(new PastimeCue.OwnerIs(PastimeCue.OwnerState.RETURNED, GREETED_RANGE)),
            PetCharacters.SHISA, new Stage(Optional.of(new Destination.ToOwner()), Optional.empty(),
                Optional.of(new Beat(Optional.of("bow"), Optional.of(PetReaction.HAPPY), Optional.of(VoiceMoment.GREET))), Optional.empty(),
                Optional.empty(), Optional.empty(), GREETING_TICKS)));
        return works;
    }

    /** @return the settings of the pastimes, by the id of the errand each is */
    public static Map<ResourceLocation, ErrandSettings<?>> settings() {
        Rests rests = PastimeData.rests();
        Map<ResourceLocation, ErrandSettings<?>> settings = new HashMap<>();
        // Fear comes before everything else a pet does with its time, and the welcome and the coffee are for the moment.
        settings.put(COWER, PastimeData.settings(0.9F, COWERING_LIMIT, rests, new ErrandTunings.Perform(COWERING_COOLDOWN)));
        settings.put(FRENZY, PastimeData.settings(0.2F, FRENZY_LIMIT, rests, new ErrandTunings.Perform(FRENZY_COOLDOWN)));
        settings.put(ASK_PRAISE, PastimeData.settings(0.4F, ASKING_LIMIT, rests,
            new ErrandTunings.AskPraise(ASKING_COOLDOWN, WALK_SPEED, ARRIVE, OWNER_REACH, COPYING_TICKS, "cute")));
        settings.put(EVENING_DRINK, PastimeData.settings(0.6F, DRINKING_LIMIT, rests, new ErrandTunings.Perform(DRINKING_COOLDOWN)));
        settings.put(COFFEE_FOR_A_WORKER, PastimeData.settings(0.7F, HANDING_LIMIT, rests,
            new ErrandTunings.Visit(HANDING_COOLDOWN, WALK_SPEED, ARRIVE, OWNER_REACH, PLACE_REACH)));
        settings.put(SWEETS_STARE, PastimeData.settings(0.6F, STARING_LIMIT, rests,
            new ErrandTunings.Visit(STARING_COOLDOWN, WALK_SPEED, ARRIVE, OWNER_REACH, PLACE_REACH)));
        settings.put(SWORD_PRACTICE, PastimeData.settings(0.2F, PRACTISING_LIMIT, rests, new ErrandTunings.Perform(PRACTISING_COOLDOWN)));
        settings.put(GREET_OWNER, PastimeData.settings(0.8F, GREETING_LIMIT, rests,
            new ErrandTunings.Visit(GREETING_COOLDOWN, WALK_SPEED, ARRIVE, OWNER_REACH, PLACE_REACH)));
        return settings;
    }

    /** @return the work of a pastime that one character has a part in */
    private static <P extends Plan, T> Work<?, ?> pastime(WorkAction<P, T> action,
            List<PastimeCue> when, ResourceLocation character, Stage stage) {
        return new Work<>(action, ErrandCategory.PASTIME, Optional.empty(), List.of(), Optional.empty(),
            Optional.of(new Pastime(when, Map.of(character, stage))));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
