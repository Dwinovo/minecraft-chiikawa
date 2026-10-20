package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.owned;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionLog;
import com.dwinovo.chiikawa.init.InitEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What the pet's mind does that the unit tests of its rules cannot reach, because it is the pet itself:
 * looking at it changes nothing, and an errand that runs out of time shows nothing for it.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SelectorGameTests {
    private static final String BATCH = "chiikawa_selector";
    private static final int STAND = 2;
    private static final long SEED = 20261020L;
    private static final String TIMEOUT_BATCH = "chiikawa_selector_timeout";
    /** Shorter than any habit, to cross in a case: the limit that data gives a sitting pet is a little over the longest habit there can be. */
    private static final int SHORT_STAY = 5;
    private static final int SETTLED = 5;
    private static final int AFTER_THE_LIMIT = 30;
    /** Long enough for two of the longest habits any pet has (160 ticks) and then some. */
    private static final int HABITS_TICKS = 400;

    /** What the data said before this batch shortened a limit. */
    private static Map<ResourceLocation, ErrandSettings<?>> shipped = Map.of();

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = TIMEOUT_BATCH)
    public static void shortenTheStay(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
        shipped = ErrandSettings.all();
        Map<ResourceLocation, ErrandSettings<?>> shortened = new HashMap<>(shipped);
        shortened.put(ErrandIds.STAY, withLimit(shipped.get(ErrandIds.STAY), SHORT_STAY));
        ErrandSettings.replaceAll(shortened);
    }

    @AfterBatch(batch = TIMEOUT_BATCH)
    public static void putTheStayBack(ServerLevel level) {
        ErrandSettings.replaceAll(shipped);
    }

    private static <T> ErrandSettings<T> withLimit(ErrandSettings<T> settings, int maxTicks) {
        return new ErrandSettings<>(settings.priority(), settings.score(), maxTicks, settings.rests(), settings.yieldsTo(),
            settings.overlays(), settings.startVoice(), settings.tuning());
    }

    /** The debug command's look at the candidates takes nothing from the pet's random, so the pet acts as if nobody had looked. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 20)
    public static void looking_at_a_pets_candidates_draws_nothing_from_its_random(GameTestHelper helper) {
        AbstractPet pet = holding(ownedPet(helper, new BlockPos(3, STAND, 3)), Items.WOODEN_HOE);

        pet.getRandom().setSeed(SEED);
        pet.getMind().look(helper.getLevel());
        long afterLooking = pet.getRandom().nextLong();
        pet.getRandom().setSeed(SEED);
        long untouched = pet.getRandom().nextLong();

        helper.assertTrue(afterLooking == untouched, "looking at the candidates drew from the pet's random");
        helper.succeed();
    }

    /**
     * A sitting pet is on the stay errand, one habit at a time, and the limit of the errand is only the safety bound that
     * data gives it. When the limit passes the errand ends and, once its short rest is over, the next decision starts it
     * again, and the player sees nothing of that: the pet does not stand up, move, or take up a pose, and is told nothing
     * about its directive. The limit is made shorter than any habit here, so that every errand runs into it. Many pets of
     * every kind, since what the brain does with nothing to do is up to chance.
     */
    @GameTest(template = "floor16", batch = TIMEOUT_BATCH, timeoutTicks = SETTLED + SHORT_STAY + AFTER_THE_LIMIT + 20)
    public static void a_sitting_pet_past_its_time_limit_shows_nothing(GameTestHelper helper) {
        List<AbstractPet> pets = sittingPets(helper);
        List<Vec3> seats = new ArrayList<>();
        long rest = shipped.get(ErrandIds.STAY).rests().gaveUp().amount().left().orElseThrow();

        helper.runAtTickTime(SETTLED - 2, () -> pets.forEach(pet -> helper.assertTrue(pet.getIntent().filter(ErrandIds.STAY::equals).isPresent(),
            "a pet told to sit is not on the stay errand")));
        helper.runAtTickTime(SETTLED, () -> pets.forEach(pet -> seats.add(pet.position())));
        helper.onEachTick(() -> showsNothing(helper, pets, seats, "when its errand ran out of time"));
        helper.runAtTickTime(SETTLED + SHORT_STAY + AFTER_THE_LIMIT, () -> {
            for (AbstractPet pet : pets) {
                List<DecisionLog.Entry> log = pet.getMind().log().entries();
                int ended = -1;
                for (int i = 0; i < log.size() && ended < 0; i++) {
                    if (log.get(i) instanceof DecisionLog.Ended end && end.errand().equals(ErrandIds.STAY)
                            && end.outcome() instanceof Outcome.GaveUp) {
                        ended = i;
                    }
                }
                helper.assertTrue(ended >= 0, "the errand never ran out of time");
                DecisionLog.Entry restart = log.subList(ended + 1, log.size()).stream()
                    .filter(entry -> entry instanceof DecisionLog.Switched switched && ErrandIds.STAY.equals(switched.to()))
                    .findFirst().orElse(null);
                helper.assertTrue(restart != null && restart.gameTime() - log.get(ended).gameTime() <= rest + 2,
                    "a sitting pet was not back on the stay errand once its rest of " + rest + " ticks was over");
            }
            helper.succeed();
        });
    }

    /**
     * Between one habit and the next a sitting pet is on no errand for a tick, and nothing of that shows: it does not stand
     * up, move, or take up a pose in the whole of a stretch long enough for several habits, and every habit that ends by
     * itself is followed by the next at once. Many pets of every kind, so that every sort of habit comes up.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SETTLED + HABITS_TICKS + 20)
    public static void a_sitting_pet_between_its_habits_shows_nothing(GameTestHelper helper) {
        List<AbstractPet> pets = sittingPets(helper);
        List<Vec3> seats = new ArrayList<>();

        helper.runAtTickTime(SETTLED, () -> pets.forEach(pet -> seats.add(pet.position())));
        helper.onEachTick(() -> showsNothing(helper, pets, seats, "between its habits"));
        helper.runAtTickTime(SETTLED + HABITS_TICKS, () -> {
            for (AbstractPet pet : pets) {
                List<DecisionLog.Entry> log = pet.getMind().log().entries();
                int habits = 0;
                for (int i = 0; i < log.size(); i++) {
                    if (!(log.get(i) instanceof DecisionLog.Ended end && end.errand().equals(ErrandIds.STAY))) {
                        continue;
                    }
                    helper.assertTrue(end.outcome() instanceof Outcome.Done, "a habit of a sitting pet ended " + end.outcome());
                    habits++;
                    DecisionLog.Entry next = log.subList(i + 1, log.size()).stream()
                        .filter(entry -> entry instanceof DecisionLog.Switched).findFirst().orElse(null);
                    helper.assertTrue(next == null || next instanceof DecisionLog.Switched switched
                            && ErrandIds.STAY.equals(switched.to()) && switched.gameTime() - end.gameTime() <= 1,
                        "a sitting pet did not go on to its next habit at once: " + next);
                }
                helper.assertTrue(habits >= 2, "a sitting pet had " + habits + " habits in " + HABITS_TICKS + " ticks");
            }
            helper.succeed();
        });
    }

    /** Pets of every kind, two of each, told to sit. */
    private static List<AbstractPet> sittingPets(GameTestHelper helper) {
        List<EntityType<? extends AbstractPet>> kinds = List.of(InitEntity.USAGI_PET.get(), InitEntity.HACHIWARE_PET.get(),
            InitEntity.CHIIKAWA_PET.get(), InitEntity.SHISA_PET.get(), InitEntity.MOMONGA_PET.get(),
            InitEntity.KURIMANJU_PET.get(), InitEntity.RAKKO_PET.get(), InitEntity.FURUHONYA_PET.get());
        List<AbstractPet> pets = new ArrayList<>();
        for (int i = 0; i < 2 * kinds.size(); i++) {
            AbstractPet pet = owned(helper, kinds.get(i % kinds.size()), new BlockPos(2 + 4 * (i % 4), STAND, 2 + 4 * (i / 4)));
            pet.setPetDirective(PetDirective.STAY);
            pets.add(pet);
        }
        return pets;
    }

    /** Fails the case if any of the pets, once it has a seat, has left it, taken up a pose or a scene, or been told to get up. */
    private static void showsNothing(GameTestHelper helper, List<AbstractPet> pets, List<Vec3> seats, String when) {
        if (seats.isEmpty()) {
            return;
        }
        for (int i = 0; i < pets.size(); i++) {
            AbstractPet pet = pets.get(i);
            helper.assertTrue(pet.position().distanceToSqr(seats.get(i)) < 1.0E-6,
                "a sitting pet moved from where it sat " + when + ": " + pet.getType());
            helper.assertTrue(pet.getActivity() == PetActivity.NONE, "a sitting pet took up a pose " + when);
            helper.assertTrue(pet.getPerformance().isEmpty(), "a sitting pet started a scene " + when);
            helper.assertTrue(pet.getPetDirective() == PetDirective.STAY, "a sitting pet was told to get up " + when);
        }
    }
}
