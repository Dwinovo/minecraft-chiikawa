package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings.Rests;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.overlay.Overlays;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSpec;
import com.dwinovo.chiikawa.entity.brain.errand.step.Meeting;
import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import com.dwinovo.chiikawa.gifting.Presents;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The generated errand settings, by the id of the errand: how much each matters, how a pet goes about
 * it and how long it leaves it alone afterwards.
 *
 * <p>Fights, the present in a pet's paws, the exam and the invitation of a friend are priority 80; the
 * slip, work and playing music 50; a photo 40; picking things up, taking a present out of the bag and calling on a
 * friend 30; pottering about and staying put 10. Within a priority the score settles which comes first.
 *
 * <p>An errand with no limit of its own (following and fighting end when they stop being wanted) is given a
 * day; one that walks to something and does one thing to it, half a minute; one that is a habit at a time, a little
 * over the longest habit.
 */
public final class ErrandSettingsData {
    /** A day: for an errand that ends when it stops being wanted, long enough that this never decides. */
    private static final int A_DAY = 24000;
    /**
     * The longest a habit of a pet with nothing to do can last, which is what a personality's habits are held to, and
     * fifteen seconds over: sitting or wandering is a habit at a time, and the way back to the area it keeps to is walked
     * in less.
     */
    private static final int LONGEST_HABIT = IdleHabits.LONGEST_TICKS + 300;
    /** Half a minute: to walk to something and do one thing to it. */
    private static final int HALF_A_MINUTE = 600;

    private static final RestSpec NONE = RestSpec.NONE;

    private ErrandSettingsData() {
    }

    /** @return settings by errand id */
    public static Map<ResourceLocation, ErrandSettings<?>> all() {
        // A crop or a chest a pet could not get to is left alone for ten seconds; it gives up on it as soon as the
        // brain's mover says it has no full path to it.
        Rests blockWork = Rests.of(NONE, RestSpec.ticksPerSubject(200), NONE);
        // A crop reaped with the tool wears it; a weed or a mushroom picked by hand does not.
        ErrandTunings.HarvestBlock reaping = new ErrandTunings.HarvestBlock(0.8F, 3.0, 0, 1);
        ErrandTunings.HarvestBlock byHand = new ErrandTunings.HarvestBlock(0.8F, 3.0, 0, 0);
        ErrandTunings.Footwork footwork = new ErrandTunings.Footwork(1.0F, 1.2F, 10.0, 3.0, 2);
        // The scene beginning is a report, and the pair rest the scene's cooldown from then on however it ends, so that
        // finishing and being cut short need rest nothing of their own. An invitation that is declined or not answered, or
        // a walk that never gets there, never began it: those rest the pair only a few seconds, so that they are not asked
        // again in the very next breath.
        Rests scene = new Rests(NONE, RestSpec.ticksPerSubject(100), NONE,
            Map.of(Meeting.PLAYED, RestSpec.fromPerSubject(RestSources.SCENE_COOLDOWN)));
        return Map.ofEntries(
            // Following is worth more the further the owner is, up to max_score; the score here is the least.
            entry(ErrandIds.FOLLOW_OWNER, errand(50, 0.5F, A_DAY, giveUp(20), new ErrandTunings.FollowOwner(1.0F, 1.0F))),
            // A habit of a pet with nothing to do is one short step, so the pet picks again between habits.
            entry(ErrandIds.STAY, errand(10, 1.0F, LONGEST_HABIT, giveUp(20), ErrandTunings.Untuned.NONE)),
            entry(ErrandIds.WANDER, errand(10, 0.05F, LONGEST_HABIT, giveUp(20), new ErrandTunings.Wander(0.6F, 4, 10, 100, 0.6F))
                .withOverlays(Overlays.IDLE_CHATTER)),
            entry(ErrandIds.PICK_UP_ITEM, errand(30, 0.4F, HALF_A_MINUTE,
                Rests.of(NONE, RestSpec.ticksPerSubject(40), NONE), new ErrandTunings.PickUp(0.7F, 1.5, 400, 0))),
            entry(WorkData.HARVEST, errand(50, 0.6F, HALF_A_MINUTE, blockWork, reaping)),
            // The farmer's strict order: harvest, then plant, then deliver.
            entry(WorkData.PLANT, errand(50, 0.6F, HALF_A_MINUTE, blockWork, new ErrandTunings.PlantBlock(0.8F, 3.0, 0))
                .yieldingTo(WorkData.HARVEST)),
            entry(WorkData.DELIVER, errand(50, 0.6F, 3000, blockWork, new ErrandTunings.Deliver(0.8F, 2, 3.0, 0))
                .yieldingTo(WorkData.HARVEST, WorkData.PLANT)),
            entry(WorkData.WEED, errand(50, 0.5F, HALF_A_MINUTE, blockWork, byHand)),
            entry(WorkData.PICK_MUSHROOM, errand(50, 0.5F, HALF_A_MINUTE, blockWork, byHand)),
            entry(WorkData.MELEE, errand(80, 0.8F, A_DAY, giveUp(20), footwork).startingWith(VoiceMoment.HUNT)),
            entry(WorkData.RANGED, errand(80, 0.8F, A_DAY, giveUp(20), new ErrandTunings.Ranged(footwork, 15.0, 15, 20))
                .startingWith(VoiceMoment.HUNT)),
            // A song's longest, as the music config bounds it, and a little over.
            entry(WorkData.PLAY_MUSIC, errand(50, 0.6F, 12040,
                Rests.of(NONE, RestSpec.from(RestSources.MUSIC_RETRY), NONE), new ErrandTunings.PlayMusic(50))),
            // A pet that gave up on the board, or was taken off the walk to it, leaves it alone for half a minute.
            entry(ErrandIds.TAKE_TASK, errand(50, 0.7F, 1200,
                Rests.of(NONE, RestSpec.ticks(600), RestSpec.ticks(600)), new ErrandTunings.TakeTask(0.7F, 2, 3.0))),
            entry(ErrandIds.SHOP, errand(50, 0.65F, 1200,
                Rests.of(RestSpec.ticks(600), RestSpec.ticks(600), NONE), new ErrandTunings.Shop(0.7F, 2, 3.0))),
            // The pet takes the present out of its bag once something has put one in its mind, and is then carrying it. It
            // does nothing else with its time, so it is done in the tick it begins. It rests from giving a present for as
            // long as the settings say, in the memory both gift errands share, from the moment it is given.
            entry(ErrandIds.OFFER_GIFT, errand(30, 0.4F, 100, giveUp(600), ErrandTunings.Untuned.NONE)),
            // The walk to the owner, or the wait by their bed, is what the time limit is for. A fight at the same priority goes first.
            // A bedside gift is left when the owner wakes from the night's sleep, just after dawn as the day's clock reads it.
            entry(ErrandIds.GIFT_OWNER, errand(80, 0.85F, 2400,
                new Rests(NONE, RestSpec.ticks(600), NONE, Map.of(Presents.GIVEN, RestSpec.from(RestSources.GIFTING_COOLDOWN))),
                new ErrandTunings.GiftOwner(1.0F, 1, 2.0, 0.77F, 0.8F, 100)).yieldingTo(WorkData.MELEE, WorkData.RANGED)),
            entry(ErrandIds.SOCIALIZE, errand(30, 0.35F, 1200, scene, new ErrandTunings.Socialize(0.6F, 1, 5))),
            entry(ErrandIds.COOPERATE, errand(80, 0.5F, 1200, scene, ErrandTunings.Untuned.NONE)),
            entry(ErrandIds.TAKE_EXAM, errand(80, 0.72F, 2400,
                Rests.of(NONE, RestSpec.ticks(200), NONE), new ErrandTunings.TakeExam(0.7F, 0.8, 20))),
            entry(ErrandIds.CHECK_RESULTS, errand(80, 0.72F, 1200,
                Rests.of(NONE, RestSpec.ticks(1200), NONE), new ErrandTunings.CheckResults(0.7F, 1.2, 60))),
            entry(ErrandIds.ANSWER_WHISTLE, errand(50, 0.55F, 12000, giveUp(200), new ErrandTunings.AnswerWhistle(0.7F, 3, 3.5, 8.0))),
            // The shutter itself reports the wait for the picture, so that every way the errand ends after it
            // already rests for it, and none before it does; when the pet is told the picture was stored the
            // rest is the camera's, and when it was lost only as long as it takes to try again.
            entry(ErrandIds.TAKE_PHOTO, errand(40, 0.45F, 1200,
                new Rests(NONE, RestSpec.from(RestSources.CAMERA_RETRY), NONE,
                    Map.of("taken", RestSpec.from(RestSources.PHOTO_UPLOAD_WAIT),
                        "stored", RestSpec.from(RestSources.CAMERA_COOLDOWN),
                        "failed", RestSpec.from(RestSources.CAMERA_RETRY))),
                new ErrandTunings.TakePhoto(0.7F, 4, 20, 60, 16.0, 400, 100))));
    }

    private static <T> ErrandSettings<T> errand(int priority, float score, int maxTicks, Rests rests, T tuning) {
        return new ErrandSettings<>(priority, score, maxTicks, rests, List.of(), List.of(), Optional.empty(), tuning);
    }

    /** @return rests of none unless it gave up, and then this many ticks */
    private static Rests giveUp(int ticks) {
        return Rests.of(NONE, RestSpec.ticks(ticks), NONE);
    }

    private static Map.Entry<ResourceLocation, ErrandSettings<?>> entry(ResourceLocation id, ErrandSettings<?> settings) {
        return Map.entry(id, settings);
    }
}
