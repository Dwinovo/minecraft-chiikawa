package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.camera.CameraSettings;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.photo.PhotoChunk;
import com.dwinovo.chiikawa.photo.PhotoRaster;
import com.dwinovo.chiikawa.photo.PhotoSystem;
import com.dwinovo.chiikawa.photo.PhotoUploads;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The photo library as the server runs it: a photo is asked for, and kept only when the player
 * it was asked of sends all of it, at the size it was asked for, within a minute. There is no
 * game on the other end of these cases, so the pieces a game would send are sent from here.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PhotoGameTests {
    private static final int STAND = 2;
    private static final int WIDTH = 32;
    private static final int HEIGHT = 24;
    private static final CameraSettings CAMERA = new CameraSettings(
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "film"), 64.0, 12000, WIDTH, HEIGHT, 70,
        new CameraSettings.Subjects(3, 5, 1), 40);

    /** What a request was told, in order. */
    private static final class Heard implements PhotoUploads.Listener {
        private final List<String> told = new ArrayList<>();

        @Override
        public void stored(UUID photo) {
            told.add("stored");
        }

        @Override
        public void failed(UUID photo) {
            told.add("failed");
        }
    }

    private static byte[] png(int width, int height) {
        try {
            return PhotoRaster.encodePng(new int[width * height], width, height);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static void send(PhotoSystem photos, ServerPlayer sender, UUID photo, byte[] png) {
        PhotoChunk.split(photo, png).forEach(chunk -> photos.receive(sender, chunk));
    }

    /** All of a photo, at the right size, from the player it was asked of, is kept. */
    @GameTest(template = "floor8", batch = "chiikawa_photo_stored", timeoutTicks = 100)
    public static void a_photo_sent_back_whole_is_kept(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        PhotoSystem photos = PhotoSystem.of(helper.getLevel().getServer());
        Heard heard = new Heard();

        UUID photo = photos.request(owner, pet, CAMERA, heard);
        helper.assertFalse(photos.library().has(photo), "the photo was there before it was sent");
        send(photos, owner, photo, png(WIDTH, HEIGHT));

        helper.assertTrue(photos.library().has(photo), "a photo sent back whole was not kept");
        helper.assertTrue(heard.told.equals(List.of("stored")), "the request was told " + heard.told);
        helper.succeed();
    }

    /** A piece from somebody else is dropped, and the photo still comes when its owner sends it. */
    @GameTest(template = "floor8", batch = "chiikawa_photo_stranger", timeoutTicks = 100)
    public static void a_photo_from_another_player_is_refused(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ServerPlayer stranger = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        PhotoSystem photos = PhotoSystem.of(helper.getLevel().getServer());
        Heard heard = new Heard();

        UUID photo = photos.request(owner, pet, CAMERA, heard);
        send(photos, stranger, photo, png(WIDTH, HEIGHT));

        helper.assertFalse(photos.library().has(photo), "a stranger's photo was kept");
        helper.assertTrue(heard.told.isEmpty(), "a stranger's piece changed the request: " + heard.told);
        send(photos, owner, photo, png(WIDTH, HEIGHT));
        helper.assertTrue(photos.library().has(photo), "the owner's photo was refused after a stranger tried");
        helper.assertTrue(heard.told.equals(List.of("stored")), "the request was told " + heard.told);
        helper.succeed();
    }

    /** A photo past the most a photo may be fails the request and keeps nothing. */
    @GameTest(template = "floor8", batch = "chiikawa_photo_oversize", timeoutTicks = 100)
    public static void a_photo_that_is_too_big_is_refused(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        PhotoSystem photos = PhotoSystem.of(helper.getLevel().getServer());
        Heard heard = new Heard();

        UUID photo = photos.request(owner, pet, CAMERA, heard);
        // As many full pieces as a photo is allowed to be cut into, and so more than it may weigh.
        send(photos, owner, photo, new byte[(PhotoChunk.MAX_PHOTO_BYTES / PhotoChunk.MAX_BYTES + 1) * PhotoChunk.MAX_BYTES]);

        helper.assertFalse(photos.library().has(photo), "a photo that was too big was kept");
        helper.assertTrue(heard.told.equals(List.of("failed")), "the request was told " + heard.told);
        helper.succeed();
    }

    /** All of a photo that is not the size asked for is refused. */
    @GameTest(template = "floor8", batch = "chiikawa_photo_size", timeoutTicks = 100)
    public static void a_photo_of_the_wrong_size_is_refused(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        PhotoSystem photos = PhotoSystem.of(helper.getLevel().getServer());
        Heard heard = new Heard();

        UUID photo = photos.request(owner, pet, CAMERA, heard);
        send(photos, owner, photo, png(WIDTH + 1, HEIGHT));

        helper.assertFalse(photos.library().has(photo), "a photo of the wrong size was kept");
        helper.assertTrue(heard.told.equals(List.of("failed")), "the request was told " + heard.told);
        helper.succeed();
    }

    /** What the server never asked for is dropped. */
    @GameTest(template = "floor8", batch = "chiikawa_photo_unasked", timeoutTicks = 100)
    public static void a_photo_nobody_asked_for_is_refused(GameTestHelper helper) {
        ServerPlayer sender = player(helper);
        PhotoSystem photos = PhotoSystem.of(helper.getLevel().getServer());
        UUID photo = UUID.randomUUID();

        send(photos, sender, photo, png(WIDTH, HEIGHT));

        helper.assertFalse(photos.library().has(photo), "a photo nobody asked for was kept");
        helper.succeed();
    }

    /** A photo that never comes is given up on after a minute, and the request is told. */
    @GameTest(template = "floor8", batch = "chiikawa_photo_expiry", timeoutTicks = (int) PhotoUploads.WAIT_TICKS + 200)
    public static void a_photo_that_never_comes_is_given_up_on(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        PhotoSystem photos = PhotoSystem.of(helper.getLevel().getServer());
        Heard heard = new Heard();

        UUID photo = photos.request(owner, pet, CAMERA, heard);

        helper.runAfterDelay(PhotoUploads.WAIT_TICKS / 2, () -> {
            helper.assertTrue(heard.told.isEmpty(), "the request was given up on after half a minute");
        });
        helper.runAfterDelay(PhotoUploads.WAIT_TICKS + 20, () -> {
            helper.assertTrue(heard.told.equals(List.of("failed")), "the request was told " + heard.told);
            send(photos, owner, photo, png(WIDTH, HEIGHT));
            helper.assertFalse(photos.library().has(photo), "a photo that came after the minute was kept");
            helper.succeed();
        });
    }
}
