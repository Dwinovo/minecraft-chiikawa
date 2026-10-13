package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.carries;
import static com.dwinovo.chiikawa.gametest.GameTestKit.count;
import static com.dwinovo.chiikawa.gametest.GameTestKit.pet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.camera.CameraSettings;
import com.dwinovo.chiikawa.camera.FilmRolls;
import com.dwinovo.chiikawa.camera.PhotoScout;
import com.dwinovo.chiikawa.data.ShopCatalogData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.init.InitActivity;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.item.FilmItem;
import com.dwinovo.chiikawa.photo.FilmRoll;
import com.dwinovo.chiikawa.photo.PhotoChunk;
import com.dwinovo.chiikawa.photo.PhotoData;
import com.dwinovo.chiikawa.photo.PhotoRaster;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.photo.PhotoSystem;
import com.dwinovo.chiikawa.shop.ShopBasket;
import com.dwinovo.chiikawa.shop.Wallet;
import com.dwinovo.chiikawa.shop.ShopCatalog;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.GameTestDontPrefix;

/**
 * Hachiware's camera: it takes a photo only with a camera and film in its bag, an owner
 * about, and not when told to stay; a photo that comes back is paid for with a roll of film
 * and carried to the owner as a present; a photo that does not costs nothing.
 *
 * <p>There is no game on the other end of these cases to draw the picture, so what the
 * owner's game would send back is sent from here, once the server has asked for it. Each
 * case is a batch of its own: a pet in the next yard over is a friend to photograph, and
 * the one case would then be about two cases' pets.
 */
@GameTestHolder(namespace = Constants.MOD_ID)
@GameTestDontPrefix
public final class CameraGameTests {
    private static final int STAND = 2;
    /** Long enough to cross a yard, aim and press the shutter. */
    private static final int SHOT_TICKS = 600;
    /** Longer than all of that, for a case about something that does not happen. */
    private static final int WAIT_TICKS = 400;
    private static final int ROLLS = 3;
    /** How soon after losing its subject a pet must be resting, and how long it is watched after that. */
    private static final int RESTING_AFTER = 3;
    private static final int SETTLED_TICKS = 20;
    /** What the camera costs, and a little short of it. */
    private static final int CAMERA_PRICE = 32;
    private static final int SHORT_OF_A_CAMERA = 10;
    /** Long enough to walk to a counter and buy a few things, and to see nothing bought. */
    private static final int SAVING_TICKS = 1200;
    /** How far round a yard another yard's pets might be seen from it. */
    private static final double NEIGHBOURHOOD = 40.0;
    /** The cameras as the data pack has them, kept before any case changes them. */
    private static Map<ResourceLocation, CameraSettings> dataPackCameras;

    @BeforeBatch(batch = "chiikawa_camera_takes")
    public static void settleTakes(ServerLevel level) {
        settle(level);
    }

    @BeforeBatch(batch = "chiikawa_camera_owner")
    public static void settleOwner(ServerLevel level) {
        settle(level);
    }

    @BeforeBatch(batch = "chiikawa_camera_friend")
    public static void settleFriend(ServerLevel level) {
        settle(level);
    }

    @BeforeBatch(batch = "chiikawa_camera_no_film")
    public static void settleNoFilm(ServerLevel level) {
        settle(level);
    }

    @BeforeBatch(batch = "chiikawa_camera_no_camera")
    public static void settleNoCamera(ServerLevel level) {
        settle(level);
    }

    @BeforeBatch(batch = "chiikawa_camera_stay")
    public static void settleStay(ServerLevel level) {
        settle(level);
    }

    @BeforeBatch(batch = "chiikawa_camera_usagi")
    public static void settleUsagi(ServerLevel level) {
        settle(level);
    }

    @BeforeBatch(batch = "chiikawa_camera_failed")
    public static void settleFailed(ServerLevel level) {
        settle(level);
    }

    @BeforeBatch(batch = "chiikawa_camera_candid")
    public static void settleCandid(ServerLevel level) {
        settle(level);
    }

    @BeforeBatch(batch = "chiikawa_camera_posed")
    public static void settlePosed(ServerLevel level) {
        settle(level);
    }

    @BeforeBatch(batch = "chiikawa_camera_saving")
    public static void settleSaving(ServerLevel level) {
        settle(level);
    }

    @BeforeBatch(batch = "chiikawa_camera_buys_camera")
    public static void settleBuysCamera(ServerLevel level) {
        settle(level);
    }

    @BeforeBatch(batch = "chiikawa_camera_lost")
    public static void settleLost(ServerLevel level) {
        settle(level);
    }

    @BeforeBatch(batch = "chiikawa_camera_beyond")
    public static void settleBeyond(ServerLevel level) {
        settle(level);
    }

    /**
     * The world as every case here wants it, and the camera as the data pack has it: a case
     * that tries another camera does not leave it for the next.
     */
    private static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
        if (dataPackCameras == null) {
            dataPackCameras = CameraSettings.all();
        }
        CameraSettings.replaceAll(dataPackCameras);
    }

    /** All it needs, and a photo comes of it: a roll begun with the shot on it, and a rest before the next. */
    @GameTest(template = "floor16", batch = "chiikawa_camera_takes", timeoutTicks = SHOT_TICKS)
    public static void a_hachiware_with_a_camera_and_film_takes_a_photo_and_starts_a_roll(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(10, STAND, 10));
        AbstractPet pet = photographer(helper, InitEntity.HACHIWARE_PET.get(), owner, true, true);
        PhotoSystem photos = PhotoSystem.of(helper.getLevel().getServer());
        Set<UUID> answered = new HashSet<>();

        helper.succeedWhen(() -> {
            for (UUID asked : photos.waiting()) {
                if (answered.add(asked)) {
                    send(photos, owner, asked, validPicture());
                }
            }
            helper.assertFalse(answered.isEmpty(), "Hachiware never asked for a photo");
            List<ItemStack> rolls = shotRolls(pet);
            helper.assertTrue(rolls.size() == 1, "there were " + rolls.size() + " rolls with shots on");
            FilmRoll roll = FilmItem.rollOf(rolls.get(0));
            helper.assertTrue(roll.shots().size() == 1 && roll.exposures() == camera().exposures(),
                "the roll had " + roll.shots().size() + " of " + roll.exposures());
            helper.assertTrue(rolls.get(0).getMaxStackSize() == 1, "a roll with a shot on it stacks");
            helper.assertTrue(count(pet, InitItems.FILM.get()) == ROLLS, "the roll was not made of one of the fresh ones");
            helper.assertTrue(pet.getPendingGift().isEmpty(), "an unfinished roll was a present");
            helper.assertTrue(pet.getBrain().hasMemoryValue(InitMemory.PHOTO_COOLDOWN.get()),
                "Hachiware took a photo and was ready to take another");
            helper.assertTrue(answered.size() == 1, "Hachiware asked for " + answered.size() + " photos");
        });
    }

    /**
     * With nothing to photograph but its owner, across the yard, it goes over and takes the
     * owner. The view is left out of it by having been to every biome there is.
     */
    @GameTest(template = "floor16", batch = "chiikawa_camera_owner", timeoutTicks = SHOT_TICKS)
    public static void a_hachiware_walks_over_to_photograph_its_owner(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(12, STAND, 12));
        AbstractPet pet = photographer(helper, InitEntity.HACHIWARE_PET.get(), owner, true, true);
        seenEveryView(pet);
        PhotoSystem photos = PhotoSystem.of(helper.getLevel().getServer());
        Set<UUID> answered = new HashSet<>();

        helper.succeedWhen(() -> {
            for (UUID asked : photos.waiting()) {
                if (answered.add(asked)) {
                    helper.assertTrue(pet.distanceTo(owner) < 6.0F, "Hachiware pressed the shutter from "
                        + pet.distanceTo(owner) + " blocks away");
                    send(photos, owner, asked, validPicture());
                }
            }
            helper.assertFalse(answered.isEmpty(), "Hachiware never asked for a photo");
            PhotoData photo = photoOf(pet);
            helper.assertTrue(photo.subject() == PhotoSubject.OWNER, "the photo was of " + photo.subject());
            helper.assertTrue(photo.name().equals(owner.getName().getString()), "the photo was of " + photo.name());
            helper.assertTrue(photo.photographer().equals(pet.getDisplayName().getString()),
                "the photo was taken by " + photo.photographer());
        });
    }

    /**
     * With its owner out of sight and another pet by, it photographs that pet, which turns to
     * the camera and says something.
     */
    @GameTest(template = "floor16", batch = "chiikawa_camera_friend", timeoutTicks = SHOT_TICKS)
    public static void a_hachiware_photographs_a_friend_who_says_something(GameTestHelper helper) {
        posedChance(1.0F);
        // Out of sight, but near enough for the game that draws the picture to have the place.
        ServerPlayer owner = player(helper);
        BlockPos far = helper.absolutePos(new BlockPos(8, STAND, 8)).offset(40, 0, 0);
        owner.teleportTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
        AbstractPet pet = photographer(helper, InitEntity.HACHIWARE_PET.get(), owner, true, true);
        seenEveryView(pet);
        AbstractPet friend = pet(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(11, STAND, 4), false);
        friend.setNoAi(true);
        PhotoSystem photos = PhotoSystem.of(helper.getLevel().getServer());
        Set<UUID> answered = new HashSet<>();

        helper.succeedWhen(() -> {
            for (UUID asked : photos.waiting()) {
                if (answered.add(asked)) {
                    helper.assertTrue(pet.distanceTo(friend) < 6.0F, "Hachiware pressed the shutter from "
                        + pet.distanceTo(friend) + " blocks away");
                    send(photos, owner, asked, validPicture());
                }
            }
            helper.assertFalse(answered.isEmpty(), "Hachiware never asked for a photo");
            PhotoData photo = photoOf(pet);
            helper.assertTrue(photo.subject() == PhotoSubject.FRIEND, "the photo was of " + photo.subject());
            helper.assertTrue(photo.name().equals(friend.getDisplayName().getString()), "the photo was of " + photo.name());
            GameTestKit.assertSaid(helper, friend, VoiceMoment.PHOTOGRAPHED);
        });
    }

    /**
     * With no camera and ten emeralds, and a camera at thirty-two, it buys nothing else, however
     * much it fancies sugar and eggs: it is saving up.
     */
    @GameTest(template = "floor16", batch = "chiikawa_camera_saving", timeoutTicks = SAVING_TICKS + 20)
    public static void a_hachiware_without_a_camera_saves_up_instead_of_buying_little_things(GameTestHelper helper) {
        AbstractPet pet = shopper(helper, false, SHORT_OF_A_CAMERA);

        helper.runAfterDelay(SAVING_TICKS, () -> {
            helper.assertTrue(Wallet.count(pet.getBackpack()) == SHORT_OF_A_CAMERA, "Hachiware spent what it was saving: "
                + Wallet.count(pet.getBackpack()) + " emeralds left");
            helper.assertFalse(carries(pet, Items.SUGAR) || carries(pet, Items.EGG) || carries(pet, Items.WHEAT),
                "Hachiware bought something while it was saving up");
            helper.succeed();
        });
    }

    /** With enough for it, it goes and buys the camera, for itself and not as a present. */
    @GameTest(template = "floor16", batch = "chiikawa_camera_buys_camera", timeoutTicks = SAVING_TICKS)
    public static void a_hachiware_with_enough_buys_its_camera_for_itself(GameTestHelper helper) {
        AbstractPet pet = shopper(helper, false, CAMERA_PRICE + 8);

        helper.succeedWhen(() -> {
            helper.assertTrue(carries(pet, InitItems.CAMERA.get()), "Hachiware never bought its camera");
            helper.assertTrue(pet.getPendingGift().isEmpty(), "the camera was a present for its owner");
        });
    }

    /**
     * A posed photo of a friend: it turns to the camera and says something. A candid one: nothing
     * is done to it at all, not a look, a word or a change of what it is doing.
     */
    @GameTest(template = "floor16", batch = "chiikawa_camera_posed", timeoutTicks = SHOT_TICKS)
    public static void a_posed_photo_has_the_friend_say_its_line(GameTestHelper helper) {
        photographFriend(helper, 1.0F, (pet, shotFriend) -> {
            GameTestKit.assertSaid(helper, shotFriend, VoiceMoment.PHOTOGRAPHED);
            helper.assertTrue(shotFriend.getBrain().hasMemoryValue(MemoryModuleType.LOOK_TARGET),
                "the friend did not turn to the camera");
        });
    }

    @GameTest(template = "floor16", batch = "chiikawa_camera_candid", timeoutTicks = SHOT_TICKS)
    public static void a_candid_photo_leaves_the_friend_alone(GameTestHelper helper) {
        photographFriend(helper, 0.0F, (pet, friend) -> {
            helper.assertFalse(GameTestKit.said(friend).isPresent(), "the friend said " + GameTestKit.said(friend));
            helper.assertFalse(friend.getBrain().hasMemoryValue(MemoryModuleType.LOOK_TARGET),
                "the friend was turned to the camera");
            helper.assertTrue(friend.getActivity() == PetActivity.READ_RESULTS, "the friend was taken from what it was doing");
            helper.assertFalse(GameTestKit.said(pet).filter(line -> GameTestKit.isLine(pet, VoiceMoment.PHOTO, line)).isPresent(),
                "Hachiware called out over a candid photo");
        });
    }

    /** A camera and no film: nothing to take it on. */
    @GameTest(template = "floor16", batch = "chiikawa_camera_no_film", timeoutTicks = WAIT_TICKS + 20)
    public static void a_hachiware_with_no_film_takes_no_photo(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(10, STAND, 10));
        photographer(helper, InitEntity.HACHIWARE_PET.get(), owner, true, false);

        stillNotAsked(helper);
    }

    /** Film and no camera: nothing to take it with. */
    @GameTest(template = "floor16", batch = "chiikawa_camera_no_camera", timeoutTicks = WAIT_TICKS + 20)
    public static void a_hachiware_with_no_camera_takes_no_photo(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(10, STAND, 10));
        photographer(helper, InitEntity.HACHIWARE_PET.get(), owner, false, true);

        stillNotAsked(helper);
    }

    /** Told to sit, it sits. */
    @GameTest(template = "floor16", batch = "chiikawa_camera_stay", timeoutTicks = WAIT_TICKS + 20)
    public static void a_hachiware_told_to_stay_takes_no_photo(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(10, STAND, 10));
        AbstractPet pet = photographer(helper, InitEntity.HACHIWARE_PET.get(), owner, true, true);
        pet.setPetDirective(PetDirective.STAY);

        stillNotAsked(helper);
    }

    /** Only Hachiware takes photos, camera or no camera. */
    @GameTest(template = "floor16", batch = "chiikawa_camera_usagi", timeoutTicks = WAIT_TICKS + 20)
    public static void only_hachiware_takes_photos(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(10, STAND, 10));
        photographer(helper, InitEntity.USAGI_PET.get(), owner, true, true);

        stillNotAsked(helper);
    }

    /**
     * Set off to photograph a friend who then is gone, Hachiware gives it up and rests: the rest
     * is set and nothing is asked for, and it does not set off again in the next tick.
     */
    @GameTest(template = "floor16", batch = "chiikawa_camera_lost", timeoutTicks = SHOT_TICKS)
    public static void a_photo_errand_whose_subject_vanishes_leaves_the_pet_resting(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos far = helper.absolutePos(new BlockPos(8, STAND, 8)).offset(40, 0, 0);
        owner.teleportTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
        AbstractPet pet = photographer(helper, InitEntity.HACHIWARE_PET.get(), owner, true, true);
        seenEveryView(pet);
        AbstractPet friend = pet(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(11, STAND, 4), false);
        friend.setNoAi(true);
        PhotoSystem photos = PhotoSystem.of(helper.getLevel().getServer());
        long[] lostAt = {-1};

        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            boolean walking = pet.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
                && pet.getBrain().getMemory(InitMemory.CURRENT_INTENT.get())
                    .filter(running -> running.id().equals(PetIntents.TAKE_PHOTO)).isPresent();
            if (lostAt[0] < 0 && walking && pet.distanceTo(friend) > 6.0F) {
                friend.discard();
                lostAt[0] = now;
            }
            if (lostAt[0] >= 0 && now - lostAt[0] >= RESTING_AFTER) {
                helper.assertTrue(pet.getBrain().hasMemoryValue(InitMemory.PHOTO_COOLDOWN.get()),
                    "the pet is not resting " + (now - lostAt[0]) + " ticks after it lost its subject");
                helper.assertFalse(pet.getBrain().isActive(InitActivity.TAKE_PHOTO.get()),
                    "the pet set off again " + (now - lostAt[0]) + " ticks after it lost its subject");
                helper.assertTrue(photos.waiting().isEmpty(), "the pet asked for a photo of nobody");
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(lostAt[0] >= 0, "the pet never set off for its friend");
            helper.assertTrue(helper.getLevel().getGameTime() - lostAt[0] >= RESTING_AFTER + SETTLED_TICKS,
                "still too soon to say");
        });
    }

    /**
     * A free-roaming Hachiware keeps to its own area for a photo as for any errand: a friend in
     * sight but outside it is not walked out to, and none is asked for.
     */
    @GameTest(template = "floor16", batch = "chiikawa_camera_beyond", timeoutTicks = WAIT_TICKS + 20)
    public static void a_free_hachiware_does_not_walk_out_of_its_area_for_a_photo(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos far = helper.absolutePos(new BlockPos(8, STAND, 8)).offset(40, 0, 0);
        owner.teleportTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
        AbstractPet pet = photographer(helper, InitEntity.HACHIWARE_PET.get(), owner, true, true);
        seenEveryView(pet);
        // Sixteen blocks off on the diagonal of the yard: in sight, and beyond the thirteen it reaches.
        AbstractPet friend = pet(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(15, STAND, 15), false);
        friend.setNoAi(true);

        stillNotAsked(helper);
    }

    /** A photo that comes back the wrong size is no photo: no film used, nothing to give, a short rest. */
    @GameTest(template = "floor16", batch = "chiikawa_camera_failed", timeoutTicks = SHOT_TICKS)
    public static void a_photo_that_does_not_come_back_costs_nothing(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(10, STAND, 10));
        AbstractPet pet = photographer(helper, InitEntity.HACHIWARE_PET.get(), owner, true, true);
        PhotoSystem photos = PhotoSystem.of(helper.getLevel().getServer());
        Set<UUID> answered = new HashSet<>();


        helper.succeedWhen(() -> {
            for (UUID asked : photos.waiting()) {
                if (answered.add(asked)) {
                    send(photos, owner, asked, wrongPicture());
                }
            }
            helper.assertFalse(answered.isEmpty(), "Hachiware never asked for a photo");
            helper.assertTrue(count(pet, InitItems.FILM.get()) == ROLLS && shotRolls(pet).isEmpty(),
                "a photo that never came was put on a roll");
            helper.assertTrue(pet.getBrain().hasMemoryValue(InitMemory.PHOTO_COOLDOWN.get()),
                "Hachiware went straight on to another photo");
            helper.assertTrue(photos.waiting().isEmpty(), "the request was still waiting");
        });
    }

    /**
     * What Hachiware buys is up to what it has: film only for a camera it has, and a camera
     * only while it has none. The shop's own list and Hachiware's own tastes, as the game
     * loaded them, drawn from many times over.
     */
    @GameTest(template = "floor8", batch = "chiikawa_camera_shopping", timeoutTicks = 100)
    public static void hachiware_buys_film_only_while_it_has_none_to_shoot_on(GameTestHelper helper) {
        AbstractPet pet = pet(helper, InitEntity.HACHIWARE_PET.get(), new BlockPos(3, STAND, 3), false);
        ShopCatalog shop = ShopCatalogData.all().get(ShopCatalogData.GENERAL);
        Item film = InitItems.FILM.get();
        Item camera = InitItems.CAMERA.get();
        // Rich enough for anything on the list, so what is left out is left out by taste.
        int money = 64;
        PhotoData shot = new PhotoData(UUID.randomUUID(), PhotoSubject.SCENERY, "", 1, "Hachiware");

        helper.assertFalse(bought(pet, shop, money).contains(film), "Hachiware bought film with no camera to put it in");

        pet.getBackpack().addItem(new ItemStack(camera));
        Set<Item> withACamera = bought(pet, shop, money);
        helper.assertTrue(withACamera.contains(film), "Hachiware with a camera never bought film");
        helper.assertFalse(withACamera.contains(camera), "Hachiware bought a second camera");

        pet.getBackpack().addItem(new ItemStack(film));
        helper.assertFalse(bought(pet, shop, money).contains(film), "Hachiware bought film with film to shoot on");
        pet.getBackpack().removeItemType(film, 64);

        ItemStack unfinished = FilmRolls.begin(film, 8, shot);
        pet.getBackpack().setItem(5, unfinished);
        helper.assertFalse(bought(pet, shop, money).contains(film), "Hachiware bought film with a roll it had not finished");

        for (int taken = 1; taken < 8; taken++) {
            FilmRolls.add(unfinished, shot);
        }
        helper.assertTrue(FilmItem.rollOf(unfinished).full(), "the roll is not full");
        helper.assertTrue(bought(pet, shop, money).contains(film), "Hachiware with only a full roll never bought film");
        helper.succeed();
    }

    /** What a pet would pick, over many visits to a shop, from what it has. */
    private static Set<Item> bought(AbstractPet pet, ShopCatalog shop, int money) {
        Set<Item> bought = new HashSet<>();
        for (int visit = 0; visit < 400; visit++) {
            ShopBasket.wants(PetPersonalities.of(pet.getType()), shop, money, ShopBasket.carriedIn(pet.getBackpack()),
                pet.getRandom()).map(ShopCatalog.Entry::item).ifPresent(bought::add);
        }
        return bought;
    }

    /**
     * Hachiware with only a friend to photograph, a camera that poses a photo {@code posedChance}
     * of the time, and the friend busy reading its exam results. Once the shutter has gone, and
     * the case has answered for the picture, {@code afterwards} looks at both.
     */
    private static void photographFriend(GameTestHelper helper, float posedChance,
            BiConsumer<AbstractPet, AbstractPet> afterwards) {
        posedChance(posedChance);
        // Out of sight, but near enough for the game that draws the picture to have the place.
        ServerPlayer owner = player(helper);
        BlockPos far = helper.absolutePos(new BlockPos(8, STAND, 8)).offset(40, 0, 0);
        owner.teleportTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
        AbstractPet pet = photographer(helper, InitEntity.HACHIWARE_PET.get(), owner, true, true);
        seenEveryView(pet);
        AbstractPet friend = pet(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(11, STAND, 4), false);
        friend.setNoAi(true);
        friend.setActivity(PetActivity.READ_RESULTS);
        PhotoSystem photos = PhotoSystem.of(helper.getLevel().getServer());
        Set<UUID> answered = new HashSet<>();

        helper.succeedWhen(() -> {
            for (UUID asked : photos.waiting()) {
                if (answered.add(asked)) {
                    send(photos, owner, asked, validPicture());
                }
            }
            helper.assertFalse(answered.isEmpty(), "Hachiware never asked for a photo");
            afterwards.accept(pet, friend);
        });
    }

    /**
     * Hachiware at a counter across the yard, with a camera in its bag if it is to have one,
     * film if it is to have film, and this much money.
     */
    private static AbstractPet shopper(GameTestHelper helper, boolean camera, int money) {
        helper.setBlock(new BlockPos(9, STAND, 4), InitBlocks.SHOP.get());
        ServerPlayer owner = ownerAt(helper, new BlockPos(2, STAND, 12));
        AbstractPet pet = photographer(helper, InitEntity.HACHIWARE_PET.get(), owner, camera, false);
        pet.getBackpack().addItem(new ItemStack(Items.EMERALD, money));
        return pet;
    }

    /** Nothing asked for after a good long while, with everything but the one thing ready. */
    private static void stillNotAsked(GameTestHelper helper) {
        PhotoSystem photos = PhotoSystem.of(helper.getLevel().getServer());
        helper.runAfterDelay(WAIT_TICKS, () -> {
            helper.assertTrue(photos.waiting().isEmpty(), "Hachiware asked for a photo");
            helper.succeed();
        });
    }

    /** The owner, in the yard where the pet can see them, and in no state to be disturbed. */
    private static ServerPlayer ownerAt(GameTestHelper helper, BlockPos at) {
        ServerPlayer owner = player(helper);
        BlockPos spot = helper.absolutePos(at);
        owner.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        return owner;
    }

    /**
     * A pet of its owner's, free to roam, with a camera in its bag if it is to have one and
     * film if it is to have film.
     */
    private static AbstractPet photographer(GameTestHelper helper, EntityType<? extends AbstractPet> type,
            ServerPlayer owner, boolean camera, boolean film) {
        clearNeighbours(helper);
        AbstractPet pet = pet(helper, type, new BlockPos(4, STAND, 4), false);
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FREE);
        if (camera) {
            pet.getBackpack().addItem(new ItemStack(InitItems.CAMERA.get()));
        }
        if (film) {
            pet.getBackpack().addItem(new ItemStack(InitItems.FILM.get(), ROLLS));
        }
        return pet;
    }

    /** Has the camera pose photos this often, until the next case puts it back. */
    private static void posedChance(float chance) {
        CameraSettings camera = camera();
        CameraSettings.replaceAll(Map.of(BuiltInRegistries.ITEM.getKey(InitItems.CAMERA.get()), new CameraSettings(
            camera.film(), camera.ownerRange(), camera.cooldownTicks(), camera.retryTicks(), camera.width(),
            camera.height(), camera.fov(), camera.subjects(), camera.aimTicks(), camera.exposures(), chance)));
    }

    /** The rolls in the pet's bag that have shots on them. */
    private static List<ItemStack> shotRolls(AbstractPet pet) {
        List<ItemStack> rolls = new ArrayList<>();
        for (int slot = 0; slot < pet.getBackpack().getContainerSize(); slot++) {
            ItemStack stack = pet.getBackpack().getItem(slot);
            if (stack.is(InitItems.FILM.get()) && FilmItem.rollOf(stack).started()) {
                rolls.add(stack);
            }
        }
        return rolls;
    }

    /** The first shot on the pet's roll. */
    private static PhotoData photoOf(AbstractPet pet) {
        List<ItemStack> rolls = shotRolls(pet);
        if (rolls.isEmpty()) {
            throw new GameTestAssertException("there is no photo on any roll");
        }
        return FilmItem.rollOf(rolls.get(0)).shots().get(0);
    }

    /** Remembers every biome as photographed, so a view is never what it would take a photo of. */
    private static void seenEveryView(AbstractPet pet) {
        pet.getBrain().setMemory(InitMemory.PHOTOGRAPHED_BIOMES.get(),
            PhotoScout.biomeOf(pet).map(List::of).orElseThrow());
    }

    /**
     * Sends away the pets earlier cases left standing in the yards round this one: the yards
     * are close, and a pet in the next one over is somebody Hachiware may want a photo of and
     * never get to.
     */
    private static void clearNeighbours(GameTestHelper helper) {
        helper.getLevel().getEntitiesOfClass(AbstractPet.class, helper.getBounds().inflate(NEIGHBOURHOOD))
            .forEach(Entity::discard);
    }

    private static CameraSettings camera() {
        return CameraSettings.of(BuiltInRegistries.ITEM.getKey(InitItems.CAMERA.get())).orElseThrow();
    }

    /** What the owner's game would send: a PNG exactly the size that was asked for. */
    private static byte[] validPicture() {
        return picture(camera().width(), camera().height());
    }

    private static byte[] wrongPicture() {
        return picture(camera().width() + 1, camera().height());
    }

    private static byte[] picture(int width, int height) {
        try {
            return PhotoRaster.encodePng(new int[width * height], width, height);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static void send(PhotoSystem photos, ServerPlayer sender, UUID photo, byte[] png) {
        PhotoChunk.split(photo, png).forEach(chunk -> photos.receive(sender, chunk));
    }
}
