package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings.Rests;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSpec;
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
 * slip, work and playing music 50; a photo 40; picking things up and calling on a friend 30; pottering
 * about and staying put 10. Within a priority the score keeps the order the pet has always had.
 *
 * <p>The numbers in each tuning are the ones the behaviors have always had. An errand with no limit of
 * its own today (following, staying, wandering and fighting end when they stop being wanted) is given
 * a day; one that walks to something and does one thing to it, half a minute.
 */
public final class ErrandSettingsData {
    /** A day: for an errand that ends when it stops being wanted, long enough that this never decides. */
    private static final int A_DAY = 24000;
    /** Half a minute: to walk to something and do one thing to it. */
    private static final int HALF_A_MINUTE = 600;

    private static final RestSpec NONE = RestSpec.NONE;

    private ErrandSettingsData() {
    }

    /** @return settings by errand id */
    public static Map<ResourceLocation, ErrandSettings<?>> all() {
        // A crop or a chest a pet could not get to is left alone for ten seconds.
        Rests blockWork = Rests.of(NONE, RestSpec.ticksPerSubject(200), NONE);
        ErrandTunings.BlockWork blockWorkTuning = new ErrandTunings.BlockWork(0.8F, 3.0);
        ErrandTunings.Footwork footwork = new ErrandTunings.Footwork(1.0F, 1.2F, 10.0, 3.0, 2);
        // However it ends, the same two leave the scene alone afterwards.
        Rests scene = Rests.of(RestSpec.fromPerSubject(RestSources.SCENE_COOLDOWN),
            RestSpec.fromPerSubject(RestSources.SCENE_COOLDOWN), RestSpec.fromPerSubject(RestSources.SCENE_COOLDOWN));
        return Map.ofEntries(
            // Following is worth more the further the owner is, up to max_score; the score here is the least.
            entry(ErrandIds.FOLLOW_OWNER, errand(50, 0.5F, A_DAY, giveUp(20), new ErrandTunings.FollowOwner(1.0F, 1.0F))),
            entry(ErrandIds.STAY, errand(10, 1.0F, A_DAY, giveUp(20), ErrandTunings.Untuned.NONE)),
            entry(ErrandIds.WANDER, errand(10, 0.05F, A_DAY, giveUp(20), new ErrandTunings.Wander(0.6F, 4, 10, 100, 0.6F))),
            entry(ErrandIds.PICK_UP_ITEM, errand(30, 0.4F, HALF_A_MINUTE,
                Rests.of(NONE, RestSpec.ticksPerSubject(40), NONE), new ErrandTunings.PickUp(0.7F, 1.5))),
            entry(ErrandIds.HARVEST, errand(50, 0.6F, HALF_A_MINUTE, blockWork, blockWorkTuning)),
            // The farmer's strict order: harvest, then plant, then deliver.
            entry(ErrandIds.PLANT, errand(50, 0.6F, HALF_A_MINUTE, blockWork, blockWorkTuning)
                .yieldingTo(ErrandIds.HARVEST)),
            entry(ErrandIds.DELIVER, errand(50, 0.6F, 3000, blockWork, new ErrandTunings.Deliver(0.8F, 2, 3.0))
                .yieldingTo(ErrandIds.HARVEST, ErrandIds.PLANT)),
            entry(ErrandIds.WEED, errand(50, 0.5F, HALF_A_MINUTE, blockWork, blockWorkTuning)),
            entry(ErrandIds.PICK_MUSHROOM, errand(50, 0.5F, HALF_A_MINUTE, blockWork, blockWorkTuning)),
            entry(ErrandIds.MELEE, errand(80, 0.8F, A_DAY, giveUp(20), footwork).startingWith(VoiceMoment.HUNT)),
            entry(ErrandIds.RANGED, errand(80, 0.8F, A_DAY, giveUp(20), new ErrandTunings.Ranged(footwork, 15.0, 20, 20))
                .startingWith(VoiceMoment.HUNT)),
            // A song's longest, as the music config bounds it, and a little over.
            entry(ErrandIds.PLAY_MUSIC, errand(50, 0.6F, 12040,
                Rests.of(NONE, RestSpec.from(RestSources.MUSIC_RETRY), NONE), new ErrandTunings.PlayMusic(50))),
            // A pet that gave up on the board, or was taken off the walk to it, leaves it alone for half a minute.
            entry(ErrandIds.TAKE_TASK, errand(50, 0.7F, 1200,
                Rests.of(NONE, RestSpec.ticks(600), RestSpec.ticks(600)), new ErrandTunings.TakeTask(0.7F, 2, 3.0))),
            entry(ErrandIds.SHOP, errand(50, 0.65F, 1200,
                Rests.of(RestSpec.ticks(600), RestSpec.ticks(600), NONE), new ErrandTunings.Shop(0.7F, 2, 3.0))),
            entry(ErrandIds.GIFT_OWNER, errand(80, 0.85F, 2400,
                Rests.of(RestSpec.from(RestSources.GIFTING_COOLDOWN), RestSpec.ticks(600), NONE),
                new ErrandTunings.GiftOwner(1.0F, 1, 2.0))),
            entry(ErrandIds.SOCIALIZE, errand(30, 0.35F, 1200, scene, new ErrandTunings.Socialize(0.6F, 1))),
            entry(ErrandIds.COOPERATE, errand(80, 0.5F, 1200, scene, ErrandTunings.Untuned.NONE)),
            entry(ErrandIds.TAKE_EXAM, errand(80, 0.72F, 2400,
                Rests.of(NONE, RestSpec.ticks(200), NONE), new ErrandTunings.TakeExam(0.7F, 0.8, 20))),
            entry(ErrandIds.CHECK_RESULTS, errand(80, 0.72F, 1200,
                Rests.of(NONE, RestSpec.ticks(1200), NONE), new ErrandTunings.CheckResults(0.7F, 1.2, 60))),
            entry(ErrandIds.ANSWER_WHISTLE, errand(50, 0.55F, 12000, giveUp(200), new ErrandTunings.AnswerWhistle(0.7F, 3, 8.0))),
            // Once the shutter has gone the pet waits for the picture; when it is told it was stored the rest is
            // the camera's, and when it was lost only as long as it takes to try again.
            entry(ErrandIds.TAKE_PHOTO, errand(40, 0.45F, 1200,
                new Rests(RestSpec.from(RestSources.PHOTO_UPLOAD_WAIT), RestSpec.from(RestSources.CAMERA_RETRY), NONE,
                    Map.of("stored", RestSpec.from(RestSources.CAMERA_COOLDOWN),
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
