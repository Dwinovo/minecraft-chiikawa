package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.MIDNIGHT;
import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.PastimeData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.run.PetIcon;
import com.dwinovo.chiikawa.entity.brain.errand.view.LivePetView;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.settings.WorldSettings;
import com.dwinovo.chiikawa.settings.WorldSettingsStore;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The rhythm of a pet's day: that a free pet sleeps through the night near home, on a bed nobody is in or on the ground, and a
 * pet at heel sleeps when its owner does; that it never lies in a bed a player is in, nor takes one from them; that it wakes
 * with the morning; and the pastimes every character has, which a pet passes the time with for a reason and which a world can
 * turn off.
 *
 * <p>Each case has a batch of its own: the cases are about a pet looking for a bed, a roof or an owner, and the one in the
 * next yard along is a bed, a roof and an owner it would find.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RhythmGameTests {
    private static final String BED_BATCH = "chiikawa_rhythm_bed";
    private static final String OWNERS_BED_BATCH = "chiikawa_rhythm_owners_bed";
    private static final String HEEL_BATCH = "chiikawa_rhythm_heel";
    private static final String MORNING_BATCH = "chiikawa_rhythm_morning";
    private static final String SHELTER_BATCH = "chiikawa_rhythm_shelter";
    private static final String HUDDLE_BATCH = "chiikawa_rhythm_huddle";
    private static final String PLAY_BATCH = "chiikawa_rhythm_play";
    private static final String NOTHING_BATCH = "chiikawa_rhythm_nothing";
    private static final String WATCH_BATCH = "chiikawa_rhythm_watch";
    private static final String YAWN_BATCH = "chiikawa_rhythm_yawn";

    private static final int STAND = 2;
    private static final int WALK_TICKS = 1200;
    private static final int LYING_TICKS = 200;
    /** The time of day a morning begins at, a little after the night ends. */
    private static final long MORNING = 23500L;
    /** Dusk: when the day is ending and the night has not come. */
    private static final long DUSK = 12500L;
    /** How long the sky keeps raining, in ticks: longer than a case. */
    private static final int RAIN_TICKS = 24000;
    /** How high above the floor a roof is: out of reach of a pet, low enough for the rain to be kept off the ground under it. */
    private static final int ROOF = 3;
    /** How many blocks a side the roof is. */
    private static final int ROOF_SIDE = 3;
    /** The world's setting as the game had it, put back after a case that changed it. */
    private static WorldSettings shipped = WorldSettings.DEFAULT;

    private RhythmGameTests() {
    }

    @BeforeBatch(batch = BED_BATCH)
    public static void duskForABed(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, DUSK);
    }

    @BeforeBatch(batch = OWNERS_BED_BATCH)
    public static void nightForAnOwnersBed(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, MIDNIGHT);
    }

    @BeforeBatch(batch = HEEL_BATCH)
    public static void nightAtHeel(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, MIDNIGHT);
    }

    @BeforeBatch(batch = MORNING_BATCH)
    public static void nightBeforeMorning(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, MIDNIGHT);
    }

    @BeforeBatch(batch = SHELTER_BATCH)
    public static void rainToShelterFrom(ServerLevel level) {
        rain(level);
    }

    @BeforeBatch(batch = HUDDLE_BATCH)
    public static void rainToHuddleIn(ServerLevel level) {
        rain(level);
    }

    @BeforeBatch(batch = PLAY_BATCH)
    public static void rainToPlayIn(ServerLevel level) {
        rain(level);
    }

    @BeforeBatch(batch = NOTHING_BATCH)
    public static void rainWithNothingToDo(ServerLevel level) {
        rain(level);
        shipped = WorldSettingsStore.of(level).get();
    }

    @BeforeBatch(batch = WATCH_BATCH)
    public static void dayForWatching(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = YAWN_BATCH)
    public static void duskForYawning(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, DUSK);
    }

    @AfterBatch(batch = BED_BATCH)
    public static void dayAfterTheBed(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @AfterBatch(batch = OWNERS_BED_BATCH)
    public static void dayAfterTheOwnersBed(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @AfterBatch(batch = HEEL_BATCH)
    public static void dayAfterTheHeel(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @AfterBatch(batch = MORNING_BATCH)
    public static void dayAfterTheMorning(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @AfterBatch(batch = SHELTER_BATCH)
    public static void dryAfterTheShelter(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @AfterBatch(batch = HUDDLE_BATCH)
    public static void dryAfterTheHuddle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @AfterBatch(batch = PLAY_BATCH)
    public static void dryAfterThePlay(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @AfterBatch(batch = NOTHING_BATCH)
    public static void putTheSettingBack(ServerLevel level) {
        WorldSettingsStore.of(level).set(shipped);
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @AfterBatch(batch = YAWN_BATCH)
    public static void dayAfterTheYawn(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** It rains for as long as a case lasts, by day. */
    private static void rain(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
        level.setWeatherParameters(0, RAIN_TICKS, true, false);
    }

    /**
     * A pet that is free, at night, lies down on a bed that nobody is in and no pet is on, a few blocks off, and not beside
     * it: it is set down on the bed, and the bed is as it was. It is only lying there; nothing has put it to sleep in it.
     */
    @GameTest(template = "floor16", batch = BED_BATCH, timeoutTicks = WALK_TICKS)
    public static void a_free_pet_sleeps_on_a_free_bed_at_night(GameTestHelper helper) {
        BlockPos head = new BlockPos(10, STAND, 10);
        putBed(helper, head);
        AbstractPet pet = GameTestKit.owned(helper, InitEntity.SHISA_PET.get(), new BlockPos(4, STAND, 4));

        helper.startSequence()
            // The day is ending, and the pet has found the bed by the time the night comes.
            .thenWaitUntil(() -> helper.assertTrue(pet.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_BED),
                "the pet never found the bed as the day ended"))
            .thenExecute(() -> helper.getLevel().setDayTime(MIDNIGHT))
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(ErrandIds.SLEEP) && onTheBed(helper, pet, head),
                "the pet never lay down on the bed: it is " + pet.getMind().errands().errand() + " at " + pet.position()
                    + ", the bed it knows of is " + pet.getBrain().getMemory(MemoryModuleType.NEAREST_BED)))
            .thenExecuteAfter(LYING_TICKS, () -> {
                helper.assertTrue(onTheBed(helper, pet, head) && pet.getMind().errands().isOn(ErrandIds.SLEEP),
                    "the pet left the bed it was sleeping on");
                helper.assertTrue(pet.getPerformance().startsWith("sleep"), "the pet is not shown asleep: " + pet.getPerformance());
                helper.assertTrue(pet.getIcon() == PetIcon.SLEEPY, "a sleeping pet wears " + pet.getIcon());
                helper.assertFalse(pet.isSleeping(), "the pet was put to sleep in the bed, as a player is");
                helper.assertFalse(helper.getBlockState(head).getValue(BedBlock.OCCUPIED), "the bed was taken by the pet");
            })
            .thenSucceed();
    }

    /**
     * The only bed there is has the owner in it. The pet sleeps on the ground by it and never gets into it, nor into the
     * foot of it; the bed stays the owner's, and the owner can get out of it and into it again, which a pet that had taken it
     * would have stopped.
     */
    @GameTest(template = "floor16", batch = OWNERS_BED_BATCH, timeoutTicks = WALK_TICKS)
    public static void a_pet_never_occupies_its_owners_bed(GameTestHelper helper) {
        BlockPos head = new BlockPos(10, STAND, 10);
        ServerPlayer owner = sleeper(helper, head);
        AbstractPet pet = ownedBy(helper, InitEntity.SHISA_PET.get(), owner, PetDirective.FREE, new BlockPos(4, STAND, 4));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(ErrandIds.SLEEP) && pet.getPerformance().startsWith("sleep"),
                "the pet never lay down: it is " + pet.getMind().errands().errand() + " at " + pet.position()))
            .thenExecuteFor(LYING_TICKS, () -> {
                helper.assertFalse(onTheBed(helper, pet, head), "the pet got into its owner's bed at " + pet.position());
                helper.assertTrue(owner.isSleeping() && helper.getBlockState(head).getValue(BedBlock.OCCUPIED),
                    "the owner's bed is not the owner's any more");
                helper.assertFalse(pet.isSleeping(), "the pet was put to sleep in the owner's bed");
            })
            .thenExecute(() -> {
                owner.stopSleepInBed(false, true);
                helper.assertFalse(helper.getBlockState(head).getValue(BedBlock.OCCUPIED), "the bed is still taken after the owner got out of it");
                owner.startSleeping(helper.absolutePos(head));
                helper.assertTrue(owner.isSleeping() && helper.getBlockState(head).getValue(BedBlock.OCCUPIED),
                    "the owner cannot get back into the bed");
            })
            .thenSucceed();
    }

    /**
     * A pet at heel sleeps while its owner does, close by, and gets up with them: it has no night of its own to sleep through.
     */
    @GameTest(template = "floor16", batch = HEEL_BATCH, timeoutTicks = WALK_TICKS)
    public static void a_following_pet_sleeps_when_its_owner_does(GameTestHelper helper) {
        BlockPos head = new BlockPos(10, STAND, 10);
        ServerPlayer owner = sleeper(helper, head);
        AbstractPet pet = ownedBy(helper, InitEntity.SHISA_PET.get(), owner, PetDirective.FOLLOW, new BlockPos(3, STAND, 3));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(ErrandIds.SLEEP) && pet.getPerformance().startsWith("sleep"),
                "the pet never lay down with its owner: it is " + pet.getMind().errands().errand() + " at " + pet.position()))
            .thenExecute(() -> {
                helper.assertTrue(pet.distanceToSqr(helper.absoluteVec(head.getCenter())) < 36.0,
                    "the pet is not near its sleeping owner: " + pet.position());
                helper.assertFalse(onTheBed(helper, pet, head), "the pet got into its owner's bed");
                owner.stopSleepInBed(false, true);
            })
            .thenWaitUntil(() -> helper.assertTrue(!pet.getMind().errands().isOn(ErrandIds.SLEEP) && pet.getPerformance().isEmpty(),
                "the pet is still asleep after its owner got up"))
            .thenSucceed();
    }

    /** The night ends, and the pet wakes with the morning: it is not still asleep at dawn. */
    @GameTest(template = "floor16", batch = MORNING_BATCH, timeoutTicks = WALK_TICKS)
    public static void a_pet_wakes_in_the_morning(GameTestHelper helper) {
        AbstractPet pet = GameTestKit.owned(helper, InitEntity.SHISA_PET.get(), new BlockPos(4, STAND, 4));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(ErrandIds.SLEEP) && pet.getPerformance().startsWith("sleep"),
                "the pet never lay down: it is " + pet.getMind().errands().errand()))
            .thenExecute(() -> helper.getLevel().setDayTime(MORNING))
            .thenWaitUntil(() -> helper.assertTrue(!pet.getMind().errands().isOn(ErrandIds.SLEEP) && pet.getPerformance().isEmpty()
                    && pet.getIcon() != PetIcon.SLEEPY,
                "the pet is still asleep in the morning: " + pet.getMind().errands().errand()))
            .thenSucceed();
    }

    /**
     * Rain falls on a pet out in the open and there is a roof some steps off: the pet goes under it and sits there, as a
     * pet that is not Chiikawa or Usagi does.
     */
    @GameTest(template = "floor16", batch = SHELTER_BATCH, timeoutTicks = WALK_TICKS)
    public static void a_pet_shelters_from_the_rain(GameTestHelper helper) {
        roof(helper, 8, 8);
        AbstractPet pet = GameTestKit.owned(helper, InitEntity.HACHIWARE_PET.get(), new BlockPos(5, STAND, 5));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(PastimeData.RAIN_SHELTER),
                "the pet never set off for the roof: it is " + pet.getMind().errands().errand() + " at " + pet.position()
                    + weatherOf(helper, pet)))
            .thenWaitUntil(() -> helper.assertTrue(underTheRoof(helper, pet, 8, 8) && pet.getPerformance().startsWith("sit"),
                "the pet is not sitting under the roof: " + pet.position() + " " + pet.getPerformance() + weatherOf(helper, pet)
                    + " it is " + pet.getMind().errands().errand()))
            .thenSucceed();
    }

    /**
     * Chiikawa goes to its owner in the rain, and not to the roof that is as near, and huddles by them.
     */
    @GameTest(template = "floor16", batch = HUDDLE_BATCH, timeoutTicks = WALK_TICKS)
    public static void chiikawa_huddles_by_its_owner_in_the_rain(GameTestHelper helper) {
        roof(helper, 4, 4);
        ServerPlayer owner = player(helper);
        BlockPos across = helper.absolutePos(new BlockPos(12, STAND, 12));
        owner.teleportTo(across.getX() + 0.5, across.getY(), across.getZ() + 0.5);
        AbstractPet pet = ownedBy(helper, InitEntity.CHIIKAWA_PET.get(), owner, PetDirective.FREE, new BlockPos(6, STAND, 6));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(PastimeData.RAIN_SHELTER)
                    && pet.distanceTo(owner) < 4.0 && pet.getPerformance().startsWith("huddle"),
                "the pet never came to huddle by its owner: it is " + pet.getMind().errands().errand() + " at " + pet.position()))
            .thenSucceed();
    }

    /** Usagi stays out in the rain and plays in it, and goes under no roof. */
    @GameTest(template = "floor16", batch = PLAY_BATCH, timeoutTicks = WALK_TICKS)
    public static void usagi_plays_in_the_rain(GameTestHelper helper) {
        roof(helper, 9, 9);
        AbstractPet pet = GameTestKit.owned(helper, InitEntity.USAGI_PET.get(), new BlockPos(4, STAND, 4));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(PastimeData.RAIN_PLAY),
                "the pet never played in the rain: it is " + pet.getMind().errands().errand()))
            .thenExecuteFor(LYING_TICKS, () -> helper.assertFalse(underTheRoof(helper, pet, 9, 9) || pet.getMind().errands().isOn(PastimeData.RAIN_SHELTER),
                "Usagi went in out of the rain"))
            .thenSucceed();
    }

    /**
     * In a world that has turned the pastimes off a pet out in the rain passes the time with nothing, and the moment they are
     * turned on again it does: so it is the setting that kept it from them.
     */
    @GameTest(template = "floor16", batch = NOTHING_BATCH, timeoutTicks = WALK_TICKS)
    public static void pastimes_at_zero_never_happen(GameTestHelper helper) {
        roof(helper, 9, 9);
        WorldSettingsStore store = WorldSettingsStore.of(helper.getLevel());
        store.set(store.get().withPastimes(0.0F));
        AbstractPet usagi = GameTestKit.owned(helper, InitEntity.USAGI_PET.get(), new BlockPos(4, STAND, 4));
        AbstractPet hachiware = GameTestKit.owned(helper, InitEntity.HACHIWARE_PET.get(), new BlockPos(5, STAND, 5));

        helper.startSequence()
            .thenExecuteFor(LYING_TICKS * 2, () -> {
                for (AbstractPet pet : new AbstractPet[] {usagi, hachiware}) {
                    for (ResourceLocation pastime : PastimeData.ids()) {
                        helper.assertFalse(pet.getMind().errands().isOn(pastime), pet.getType().toShortString() + " passed the time with "
                            + pastime + " in a world that has no pastimes");
                    }
                }
            })
            .thenExecute(() -> store.set(store.get().withPastimes(1.0F)))
            .thenWaitUntil(() -> helper.assertTrue(usagi.getMind().errands().isOn(PastimeData.RAIN_PLAY),
                "the pet passed no time once the world had pastimes again: it is " + usagi.getMind().errands().errand()))
            .thenSucceed();
    }

    /**
     * The owner swings a pickaxe across the yard and the pet comes over to watch, and claps; and having watched, rests
     * before it comes to watch again.
     */
    @GameTest(template = "floor16", batch = WATCH_BATCH, timeoutTicks = WALK_TICKS)
    public static void a_pet_watches_its_owner_work(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        BlockPos across = helper.absolutePos(new BlockPos(12, STAND, 12));
        owner.teleportTo(across.getX() + 0.5, across.getY(), across.getZ() + 0.5);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner, PetDirective.FREE, new BlockPos(4, STAND, 4));
        helper.onEachTick(() -> owner.swing(InteractionHand.MAIN_HAND));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(PastimeData.WATCH_OWNER_WORK) && pet.distanceTo(owner) < 4.5,
                "the pet never came to watch its owner work: it is " + pet.getMind().errands().errand() + " at " + pet.position()))
            .thenWaitUntil(() -> helper.assertTrue(!pet.getMind().errands().isOn(PastimeData.WATCH_OWNER_WORK),
                "the pet never had its fill of watching"))
            .thenExecuteAfter(LYING_TICKS, () -> helper.assertFalse(pet.getMind().errands().isOn(PastimeData.WATCH_OWNER_WORK),
                "the pet came back to watch again at once"))
            .thenSucceed();
    }

    /** As the day ends a pet yawns, with its owner by to see it, and says so in its own words. */
    @GameTest(template = "floor16", batch = YAWN_BATCH, timeoutTicks = WALK_TICKS)
    public static void a_pet_yawns_as_the_day_ends(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos near = helper.absolutePos(new BlockPos(5, STAND, 4));
        owner.teleportTo(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
        AbstractPet pet = ownedBy(helper, InitEntity.HACHIWARE_PET.get(), owner, PetDirective.FREE, new BlockPos(4, STAND, 4));
        GameTestKit.quietYard(helper, DUSK);

        helper.startSequence()
            .thenWaitUntil(() -> GameTestKit.assertSaid(helper, pet, VoiceMoment.SLEEPY))
            .thenSucceed();
    }

    /** What a pet that never set off for the roof was to go by, for the case to say so. */
    private static String weatherOf(GameTestHelper helper, AbstractPet pet) {
        LivePetView view = LivePetView.of(pet);
        return " [raining " + helper.getLevel().isRaining() + ", weather " + view.weather() + ", under the sky " + view.underSky()
            + ", shelter " + view.surroundings().shelter() + "]";
    }

    /** A bed whose head is here, facing east, and its foot. */
    private static void putBed(GameTestHelper helper, BlockPos head) {
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
        helper.setBlock(head.west(), bed.setValue(BedBlock.PART, BedPart.FOOT));
        helper.setBlock(head, bed.setValue(BedBlock.PART, BedPart.HEAD));
    }

    /** A player asleep in a bed whose head is here. */
    private static ServerPlayer sleeper(GameTestHelper helper, BlockPos head) {
        putBed(helper, head);
        ServerPlayer owner = player(helper);
        BlockPos in = helper.absolutePos(head);
        owner.teleportTo(in.getX() + 0.5, in.getY() + 0.6, in.getZ() + 0.5);
        owner.startSleeping(in);
        return owner;
    }

    private static AbstractPet ownedBy(GameTestHelper helper, EntityType<? extends AbstractPet> type, ServerPlayer owner,
            PetDirective directive, BlockPos at) {
        AbstractPet pet = GameTestKit.wild(helper, type, at);
        pet.tame(owner);
        pet.setPetDirective(directive);
        return pet;
    }

    /** Whether the pet is on the bed whose head is here, either half of it. */
    private static boolean onTheBed(GameTestHelper helper, AbstractPet pet, BlockPos head) {
        AABB bed = new AABB(helper.absolutePos(head)).minmax(new AABB(helper.absolutePos(head.west())));
        return pet.getBoundingBox().intersects(bed);
    }

    /** A roof of a few planks over the floor, whose corner is here, which keeps the rain off the floor under it. */
    private static void roof(GameTestHelper helper, int x, int z) {
        GameTestKit.openSky(helper);
        for (int dx = 0; dx < ROOF_SIDE; dx++) {
            for (int dz = 0; dz < ROOF_SIDE; dz++) {
                helper.setBlock(new BlockPos(x + dx, STAND + ROOF, z + dz), Blocks.OAK_PLANKS);
            }
        }
    }

    /** Whether the pet stands under the roof whose corner is here, by where the roof is in the world and not in the case's yard. */
    private static boolean underTheRoof(GameTestHelper helper, AbstractPet pet, int x, int z) {
        BlockPos one = helper.absolutePos(new BlockPos(x, STAND, z));
        BlockPos other = helper.absolutePos(new BlockPos(x + ROOF_SIDE - 1, STAND, z + ROOF_SIDE - 1));
        return pet.getX() >= Math.min(one.getX(), other.getX()) && pet.getX() < Math.max(one.getX(), other.getX()) + 1
            && pet.getZ() >= Math.min(one.getZ(), other.getZ()) && pet.getZ() < Math.max(one.getZ(), other.getZ()) + 1;
    }
}
