package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.ErrandSettingsData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Errands;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionLog;
import com.dwinovo.chiikawa.entity.brain.errand.impl.LegacyErrand;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestContext;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestLedger;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSpec;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import com.dwinovo.chiikawa.entity.brain.errand.view.LivePetView;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What every native errand does, so that none can be made native without it: the same three cases for
 * each, made from its scenario in {@link ErrandScenarios}. What it offers is asked without changing the pet or
 * drawing from its random; it ends within its time limit and rests as its data says; and cut short at the
 * start, in the middle and at the last step, it ends interrupted and leaves nothing of itself behind.
 *
 * <p>The cases are made when the game registers them, one set for each errand that has a scenario. The
 * errands that are not native yet are listed in {@link #LEGACY}, which shrinks with each one that goes
 * native and is empty when the last has.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ErrandContractGameTests {
    private static final String BATCH = "chiikawa_errand_contract";
    private static final String TEMPLATE = Constants.MOD_ID + ":floor16";
    private static final long SEED = 42L;
    /** How long a pet is given to notice what it is to do and set off, before its time limit runs. */
    private static final int SETTLING = 120;
    /** How far past its time limit an errand may be found to have ended. */
    private static final int GRACE = 20;
    /** How long an interrupt is allowed to take: the tick it is told, and the one after. */
    private static final int INTERRUPT_TICKS = 2;
    /** How long into a step the pet is cut short for the middle of a routine. */
    private static final int MIDDLE_TICKS = 10;
    /** Long enough for the longest errand to be cut short for the third time: a paper written in full before the last cut. */
    private static final int INTERRUPT_CASE_TICKS = 1200;
    private static final int PURITY_CASE_TICKS = 200;

    /** The errands still carried out by an old activity, which have no cases here yet. */
    private static final Set<ResourceLocation> LEGACY = Set.of(ErrandIds.FOLLOW_OWNER, ErrandIds.STAY, ErrandIds.WANDER,
        ErrandIds.HARVEST, ErrandIds.PLANT, ErrandIds.DELIVER, ErrandIds.WEED, ErrandIds.PICK_MUSHROOM, ErrandIds.MELEE,
        ErrandIds.RANGED, ErrandIds.PLAY_MUSIC, ErrandIds.TAKE_TASK, ErrandIds.SOCIALIZE, ErrandIds.COOPERATE,
        ErrandIds.ANSWER_WHISTLE);

    /**
     * Where in an errand's routine a pet is cut short. The first is as soon as the pet is seen on the errand at all,
     * which for an errand whose first step is an action of one tick is already past it.
     */
    private enum Point {
        FIRST,
        MIDDLE,
        LAST;

        boolean reached(Running running, long now, int steps) {
            return switch (this) {
                case FIRST -> true;
                case MIDDLE -> running.stepIndex() == (steps - 1) / 2 && now - running.startedAt() >= MIDDLE_TICKS;
                case LAST -> running.stepIndex() == steps - 1;
            };
        }
    }

    /** What the pet was seen to be on, so that how it ended can be told against how it was going. */
    private static final class Watch {
        private Running seen;
    }

    /**
     * What the pet is, to the last of what an errand could change by asking: its memories, its bag, its synced data and
     * where it stands.
     */
    private record Snapshot(Map<MemoryModuleType<?>, ?> memories, List<String> bag,
                            List<SynchedEntityData.DataValue<?>> data, Vec3 position) {
        static Snapshot of(AbstractPet pet) {
            List<String> bag = new ArrayList<>();
            for (int slot = 0; slot < pet.getBackpack().getContainerSize(); slot++) {
                bag.add(pet.getBackpack().getItem(slot).toString());
            }
            return new Snapshot(new HashMap<>(pet.getBrain().getMemories()), bag, pet.getEntityData().getNonDefaultValues(),
                pet.position());
        }
    }

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** Three cases for each errand that has a scenario. */
    @GameTestGenerator
    public static Collection<TestFunction> contract_cases() {
        List<TestFunction> cases = new ArrayList<>();
        for (ResourceLocation errand : new TreeSet<>(ErrandScenarios.ids())) {
            int limit = ErrandSettingsData.all().get(errand).maxTicks();
            cases.add(testOf("offer_is_pure", errand, PURITY_CASE_TICKS, helper -> offerIsPure(helper, errand)));
            cases.add(testOf("ends_in_time", errand, SETTLING + limit + GRACE, helper -> endsInTime(helper, errand)));
            cases.add(testOf("interrupted_leaves_nothing", errand, INTERRUPT_CASE_TICKS,
                helper -> interruptedLeavesNothing(helper, errand)));
        }
        return cases;
    }

    /** The errands with a scenario are the native ones, and with the legacy ones they are all the errands there are. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 20)
    public static void every_errand_is_nativeswith_a_scenario_or_legacy_without(GameTestHelper helper) {
        Set<ResourceLocation> all = new TreeSet<>();
        Set<ResourceLocation> natives = new TreeSet<>();
        Set<ResourceLocation> legacy = new TreeSet<>();
        for (ErrandType<?, ?> errand : Errands.all()) {
            all.add(errand.id());
            (errand instanceof LegacyErrand<?, ?> ? legacy : natives).add(errand.id());
        }
        helper.assertTrue(natives.equals(new TreeSet<>(ErrandScenarios.ids())),
            "the native errands are " + natives + " and the ones with a scenario " + ErrandScenarios.ids());
        helper.assertTrue(legacy.equals(new TreeSet<>(LEGACY)), "the legacy errands are " + legacy + ", not " + LEGACY);
        Set<ResourceLocation> together = new TreeSet<>(natives);
        together.addAll(LEGACY);
        helper.assertTrue(together.equals(all), "native and legacy together are " + together + ", not all of " + all);
        helper.succeed();
    }

    /**
     * A case of its own batch, so that it runs alone: a pet in the yard next to it, such as another case's, is a friend
     * for a photographer to take a photo of and a rival for a pet to pick up an item from.
     */
    private static TestFunction testOf(String what, ResourceLocation errand, int timeoutTicks,
            Consumer<GameTestHelper> body) {
        return new TestFunction(BATCH + "_" + what + "_" + errand.getPath(),
            ErrandContractGameTests.class.getSimpleName().toLowerCase() + "." + what + "_" + errand.getPath(), TEMPLATE,
            timeoutTicks, 0L, true, helper -> {
                settleWorld(helper.getLevel(), Difficulty.NORMAL, NOON);
                body.accept(helper);
            });
    }

    /**
     * Asked what it offers, twice, the errand answers the same, leaves the pet exactly as it was and draws
     * nothing from its random.
     */
    private static void offerIsPure(GameTestHelper helper, ResourceLocation id) {
        ErrandScenarios.Scenario scenario = ErrandScenarios.setUp(id, helper);
        AbstractPet pet = scenario.pet();
        boolean[] done = {false};
        helper.onEachTick(() -> {
            if (done[0] || ask(id, pet).isEmpty()) {
                return;
            }
            done[0] = true;
            Snapshot before = Snapshot.of(pet);
            pet.getRandom().setSeed(SEED);
            Offers<?> first = ask(id, pet);
            Offers<?> second = ask(id, pet);
            long drawn = pet.getRandom().nextLong();
            pet.getRandom().setSeed(SEED);
            long untouched = pet.getRandom().nextLong();

            helper.assertTrue(first.equals(second), id + " answered " + first + " and then " + second);
            helper.assertTrue(before.equals(Snapshot.of(pet)), id + " changed the pet it was asked about");
            helper.assertTrue(drawn == untouched, id + " drew from the pet's random");
            helper.succeed();
        });
    }

    /**
     * Once it is under way the errand ends within its time limit, and the pet rests from it for as long as
     * its data says for the way it ended.
     */
    private static void endsInTime(GameTestHelper helper, ResourceLocation id) {
        ErrandScenarios.Scenario scenario = ErrandScenarios.setUp(id, helper);
        AbstractPet pet = scenario.pet();
        Watch watch = new Watch();
        boolean[] done = {false};
        helper.runAtTickTime(SETTLING, () -> helper.assertTrue(watch.seen != null || done[0], "the pet never set off on " + id));
        helper.onEachTick(() -> {
            if (done[0]) {
                return;
            }
            runningOn(pet, id).ifPresent(running -> watch.seen = running);
            Optional<DecisionLog.Ended> ended = endedSince(pet, id, -1);
            if (ended.isEmpty()) {
                return;
            }
            done[0] = true;
            Running ran = watch.seen;
            helper.assertTrue(ran != null, id + " ended without having been seen to run");
            helper.assertTrue(ended.get().ranTicks() <= ran.settings().maxTicks() + GRACE,
                id + " ran " + ended.get().ranTicks() + " ticks, past its limit of " + ran.settings().maxTicks());
            assertRested(helper, pet, scenario, ran, ended.get());
            helper.succeed();
        });
    }

    /**
     * Cut short at the start, in the middle and at the last step, the errand ends interrupted within two
     * ticks, the pet rests from it as its data says, and it holds nothing of it. One pet goes through all
     * three, since an interrupted errand is one it may set off on again.
     */
    private static void interruptedLeavesNothing(GameTestHelper helper, ResourceLocation id) {
        ErrandScenarios.Scenario scenario = ErrandScenarios.setUp(id, helper);
        AbstractPet pet = scenario.pet();
        Point[] points = Point.values();
        Watch watch = new Watch();
        int[] next = {0};
        long[] told = {-1};
        helper.onEachTick(() -> {
            if (next[0] == points.length) {
                return;
            }
            long now = helper.getLevel().getGameTime();
            Optional<Running> running = runningOn(pet, id);
            running.ifPresent(found -> watch.seen = found);
            if (told[0] < 0) {
                if (running.isPresent() && points[next[0]].reached(running.get(), now, scenario.steps())) {
                    PetMind.post(pet, new Message.Moved("test"));
                    told[0] = now;
                }
                return;
            }
            Optional<DecisionLog.Ended> ended = endedSince(pet, id, told[0]);
            if (ended.isEmpty()) {
                helper.assertTrue(now - told[0] < INTERRUPT_TICKS, id + " was not ended " + INTERRUPT_TICKS
                    + " ticks after it was cut short at " + points[next[0]]);
                return;
            }
            String at = " (cut short at " + points[next[0]] + ")";
            helper.assertTrue(ended.get().outcome() instanceof Outcome.Interrupted, id + " ended " + ended.get().outcome() + at);
            assertRested(helper, pet, scenario, watch.seen, ended.get());
            Residue.none(helper, pet, scenario, watch.seen);
            told[0] = -1;
            if (++next[0] == points.length) {
                helper.succeed();
            }
        });
    }

    /**
     * That the pet rests from the errand for exactly what its data says for the way it ended, and for what its steps
     * reported on the way if that is more: a rest begun in the middle of the errand has run down by the time it ends,
     * by no more than the errand ran.
     */
    private static void assertRested(GameTestHelper helper, AbstractPet pet, ErrandScenarios.Scenario scenario, Running ran,
            DecisionLog.Ended ended) {
        ResourceLocation id = ran.errand();
        ErrandSettings.Rests rests = ran.settings().rests();
        RestSpec spec = switch (ended.outcome()) {
            case Outcome.Done done -> rests.done();
            case Outcome.GaveUp gaveUp -> rests.gaveUp();
            case Outcome.Interrupted interrupted -> rests.interrupted();
        };
        long ticks = ticksOf(spec, ran);
        helper.assertTrue(ticks > 0 || !(ended.outcome() instanceof Outcome.GaveUp), id + " gives no rest after giving up");
        long reported = scenario.reported().apply(ran).map(report -> ticksOf(rests.reports().get(report), ran)).orElse(0L);

        long now = helper.getLevel().getGameTime();
        RestLedger ledger = new RestLedger(errand -> Errands.get(errand).restStore().of(pet.getBrain(), errand), ErrandSettings::of);
        long left = ran.plan().subject().map(subject -> ledger.remaining(id, subject, now)).orElseGet(() -> ledger.remaining(id, now));
        // A rest kept in a memory has run down by one in the tick it was written in, since the brain ticks after the mind.
        if (reported > ticks) {
            long latest = reported - (now - ended.gameTime());
            helper.assertTrue(left <= latest && left >= latest - ended.ranTicks() - 1,
                id + " " + ended.outcome() + " leaves " + left + " ticks of rest, not the " + reported + " it reported less up to "
                    + ended.ranTicks() + " ticks");
            return;
        }
        long expected = Math.max(0L, ticks - (now - ended.gameTime()));
        helper.assertTrue(left == expected || left == expected - 1,
            id + " " + ended.outcome() + " leaves " + left + " ticks of rest, not " + expected);
    }

    private static long ticksOf(RestSpec spec, Running ran) {
        return spec.amount().map(fixed -> (long) fixed,
            source -> RestSources.resolver(source).map(resolver -> resolver.ticks(new RestContext(ran.plan().restKey()))).orElse(0L));
    }

    private static Optional<Running> runningOn(AbstractPet pet, ResourceLocation id) {
        return pet.getMind().errands().running().filter(running -> running.errand().equals(id));
    }

    /** @return the first time the errand ended after {@code since}, if it has */
    private static Optional<DecisionLog.Ended> endedSince(AbstractPet pet, ResourceLocation id, long since) {
        return pet.getMind().log().entries().stream()
            .filter(entry -> entry instanceof DecisionLog.Ended ended && ended.errand().equals(id) && ended.gameTime() > since)
            .map(DecisionLog.Ended.class::cast).findFirst();
    }

    /** @return what the errand offers the pet as it is now, from a view made for the question */
    private static Offers<?> ask(ResourceLocation id, AbstractPet pet) {
        return askOf(Errands.get(id), ErrandSettings.of(id).orElseThrow(), LivePetView.of(pet));
    }

    @SuppressWarnings("unchecked")
    private static <P extends Plan, T> Offers<P> askOf(ErrandType<P, T> type, ErrandSettings<?> settings, PetView view) {
        return type.offers(view, (T) settings.tuning());
    }
}
