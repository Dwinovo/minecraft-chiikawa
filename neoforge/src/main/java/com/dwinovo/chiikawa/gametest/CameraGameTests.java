package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.count;
import static com.dwinovo.chiikawa.gametest.GameTestKit.pet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.camera.CameraSettings;
import com.dwinovo.chiikawa.camera.PhotoScout;
import com.dwinovo.chiikawa.data.ShopCatalogData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.init.InitDataComponents;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.photo.PhotoChunk;
import com.dwinovo.chiikawa.photo.PhotoData;
import com.dwinovo.chiikawa.photo.PhotoRaster;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.photo.PhotoSystem;
import com.dwinovo.chiikawa.shop.ShopBasket;
import com.dwinovo.chiikawa.shop.ShopCatalog;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CameraGameTests {
    private static final int STAND = 2;
    /** Long enough to cross a yard, aim and press the shutter. */
    private static final int SHOT_TICKS = 600;
    /** Longer than all of that, for a case about something that does not happen. */
    private static final int WAIT_TICKS = 400;
    private static final int ROLLS = 3;
    /** How far round a yard another yard's pets might be seen from it. */
    private static final double NEIGHBOURHOOD = 40.0;

    @BeforeBatch(batch = "chiikawa_camera_takes")
    public static void settleTakes(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = "chiikawa_camera_owner")
    public static void settleOwner(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = "chiikawa_camera_friend")
    public static void settleFriend(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = "chiikawa_camera_no_film")
    public static void settleNoFilm(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = "chiikawa_camera_no_camera")
    public static void settleNoCamera(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = "chiikawa_camera_stay")
    public static void settleStay(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = "chiikawa_camera_usagi")
    public static void settleUsagi(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = "chiikawa_camera_failed")
    public static void settleFailed(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** All it needs, and a photo comes of it: one roll used, the photo for its owner, a rest before the next. */
    @GameTest(template = "floor16", batch = "chiikawa_camera_takes", timeoutTicks = SHOT_TICKS)
    public static void a_hachiware_with_a_camera_and_film_takes_a_photo_and_gives_it_to_its_owner(GameTestHelper helper) {
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
            helper.assertTrue(count(pet, InitItems.FILM.get()) == ROLLS - 1, "a photo came back and "
                + (ROLLS - count(pet, InitItems.FILM.get())) + " rolls of film were used");
            helper.assertTrue(isPhoto(pet.getPendingGift()) || owner.getInventory().contains(new ItemStack(InitItems.PHOTO.get())),
                "the photo went nowhere");
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
        ServerPlayer owner = ownerAt(helper, new BlockPos(14, STAND, 14));
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
            PhotoData photo = photoOf(pet, owner);
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
            PhotoData photo = photoOf(pet, owner);
            helper.assertTrue(photo.subject() == PhotoSubject.FRIEND, "the photo was of " + photo.subject());
            helper.assertTrue(photo.name().equals(friend.getDisplayName().getString()), "the photo was of " + photo.name());
            GameTestKit.assertSaid(helper, friend, VoiceMoment.PHOTOGRAPHED);
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
            helper.assertTrue(count(pet, InitItems.FILM.get()) == ROLLS, "film was used up for a photo that never came");
            helper.assertFalse(isPhoto(pet.getPendingGift()), "Hachiware had a photo to give that never came");
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
    public static void hachiware_buys_film_only_while_it_has_a_camera(GameTestHelper helper) {
        AbstractPet pet = pet(helper, InitEntity.HACHIWARE_PET.get(), new BlockPos(3, STAND, 3), false);
        ShopCatalog shop = ShopCatalogData.all().get(ShopCatalogData.GENERAL);
        Item film = InitItems.FILM.get();
        Item camera = InitItems.CAMERA.get();
        // Rich enough for anything on the list, so what is left out is left out by taste.
        int money = 64;

        Set<Item> withoutACamera = bought(pet, shop, money);
        helper.assertFalse(withoutACamera.contains(film), "Hachiware bought film with no camera to put it in");

        pet.getBackpack().addItem(new ItemStack(camera));
        Set<Item> withACamera = bought(pet, shop, money);
        helper.assertTrue(withACamera.contains(film), "Hachiware with a camera never bought film");
        helper.assertFalse(withACamera.contains(camera), "Hachiware bought a second camera");
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

    /** The photo the pet has to give its owner, in its paws or already in the owner's hands. */
    private static PhotoData photoOf(AbstractPet pet, ServerPlayer owner) {
        if (isPhoto(pet.getPendingGift())) {
            return pet.getPendingGift().get(InitDataComponents.PHOTO.get());
        }
        for (ItemStack stack : owner.getInventory().items) {
            if (isPhoto(stack)) {
                return stack.get(InitDataComponents.PHOTO.get());
            }
        }
        throw new GameTestAssertException("there is no photo to give");
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

    private static boolean isPhoto(ItemStack stack) {
        PhotoData data = stack.get(InitDataComponents.PHOTO.get());
        return stack.is(InitItems.PHOTO.get()) && data != null;
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
