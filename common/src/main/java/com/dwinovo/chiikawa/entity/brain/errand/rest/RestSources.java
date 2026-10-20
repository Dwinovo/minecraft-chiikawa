package com.dwinovo.chiikawa.entity.brain.errand.rest;

import com.dwinovo.chiikawa.Constants;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * The named numbers an errand's rest can point at instead of holding its own, so each number
 * lives in exactly one file: the camera's cooldown stays in {@code pet_camera}, the present
 * cooldown in {@code pet_gifting}, and an errand file says {@code {"from": "chiikawa:camera_retry"}}.
 *
 * <p>The names are fixed here and checked when an errand file is loaded: an errand pointing at one
 * that is not on this list is turned off. What a name resolves to is registered by the errand that
 * goes native and needs it, because it asks a live system (a camera's settings, the music config) that
 * a load cannot.
 */
public final class RestSources {
    /** The least time between two photos, from the camera's {@code cooldown_ticks}. */
    public static final ResourceLocation CAMERA_COOLDOWN = id("camera_cooldown");
    /** How long to wait after a photo that did not come back, from the camera's {@code retry_ticks}. */
    public static final ResourceLocation CAMERA_RETRY = id("camera_retry");
    /** How long a photo is waited for while the owner's game draws it. */
    public static final ResourceLocation PHOTO_UPLOAD_WAIT = id("photo_upload_wait");
    /** The least time between two presents, from {@code pet_gifting}. */
    public static final ResourceLocation GIFTING_COOLDOWN = id("gifting_cooldown");
    /** How long a musician waits after a song that would not start, from the music config. */
    public static final ResourceLocation MUSIC_RETRY = id("music_retry");
    /** How long the same two pets leave a scene alone, from {@code pet_interaction}. */
    public static final ResourceLocation SCENE_COOLDOWN = id("scene_cooldown");

    private static final Set<ResourceLocation> NAMES = Set.of(CAMERA_COOLDOWN, CAMERA_RETRY, PHOTO_UPLOAD_WAIT,
        GIFTING_COOLDOWN, MUSIC_RETRY, SCENE_COOLDOWN);
    private static final Map<ResourceLocation, Resolver> RESOLVERS = new ConcurrentHashMap<>();

    private RestSources() {
    }

    /** Works a named number out. */
    @FunctionalInterface
    public interface Resolver {
        /**
         * @param context what the rest is about
         * @return how many ticks the source gives for it
         */
        long ticks(RestContext context);
    }

    /** @return whether an errand file may point at this name */
    public static boolean isKnown(ResourceLocation name) {
        return NAMES.contains(name);
    }

    /** @return every name an errand file may point at */
    public static Set<ResourceLocation> names() {
        return NAMES;
    }

    /**
     * Says what a name resolves to. Public so the errand that needs it can, and an in-game case can
     * try another.
     *
     * @throws IllegalArgumentException for a name that is not one of the six
     */
    public static void register(ResourceLocation name, Resolver resolver) {
        if (!isKnown(name)) {
            throw new IllegalArgumentException("Unknown rest source " + name);
        }
        RESOLVERS.put(name, resolver);
    }

    /** @return what the name resolves to, or nothing while no errand has registered it */
    public static Optional<Resolver> resolver(ResourceLocation name) {
        return Optional.ofNullable(RESOLVERS.get(name));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
