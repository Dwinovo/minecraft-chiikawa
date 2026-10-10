package com.dwinovo.chiikawa.camera;

import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

/**
 * How a camera behaves, loaded from {@code data/<namespace>/pet_camera/<item>.json} by
 * {@link CameraSettingsLoader}: one file per camera item, named by the item's own id, so a
 * pack can retune the camera or make another kind of it.
 *
 * <p>Server-side only: a player's game is told the size and the field of view with each
 * photo it is asked for, and needs nothing else.
 *
 * @param film the item a photo is taken on, one for each
 * @param ownerRange how far from its owner a pet may be and still take a photo, in blocks;
 *                   the picture is drawn by the owner's game, which only has what is near it
 * @param cooldownTicks the least time between two photos a pet takes
 * @param width how wide a photo is, in pixels
 * @param height how tall
 * @param fov how wide the lens sees, in degrees from top to bottom of the picture
 * @param subjects how much a pet fancies taking each kind of photo
 * @param aimTicks how long a pet holds the camera up before the shutter goes
 */
public record CameraSettings(ResourceLocation film, double ownerRange, int cooldownTicks, int width, int height,
                             int fov, Subjects subjects, int aimTicks) {
    /**
     * How much a pet fancies each kind of photo, as weights: a pet picks among the kinds it
     * has a chance to take now in proportion to these.
     */
    public record Subjects(int owner, int friend, int scenery) {
        public static final Codec<Subjects> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("owner").forGetter(Subjects::owner),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("friend").forGetter(Subjects::friend),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("scenery").forGetter(Subjects::scenery)
        ).apply(instance, Subjects::new));

        public int weight(PhotoSubject subject) {
            return switch (subject) {
                case OWNER -> owner;
                case FRIEND -> friend;
                case SCENERY -> scenery;
            };
        }
    }

    public static final Codec<CameraSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("film").forGetter(CameraSettings::film),
        Codec.doubleRange(1.0, 256.0).fieldOf("owner_range").forGetter(CameraSettings::ownerRange),
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("cooldown_ticks").forGetter(CameraSettings::cooldownTicks),
        Codec.intRange(16, 1024).fieldOf("width").forGetter(CameraSettings::width),
        Codec.intRange(16, 1024).fieldOf("height").forGetter(CameraSettings::height),
        Codec.intRange(30, 110).fieldOf("fov").forGetter(CameraSettings::fov),
        Subjects.CODEC.fieldOf("subjects").forGetter(CameraSettings::subjects),
        ExtraCodecs.POSITIVE_INT.fieldOf("aim_ticks").forGetter(CameraSettings::aimTicks)
    ).apply(instance, CameraSettings::new));

    private static volatile Map<ResourceLocation, CameraSettings> byItem = Collections.emptyMap();

    /**
     * @param item an item's id
     * @return how that item takes photos, or nothing when no data pack makes a camera of it:
     *         an item whose settings are gone takes none rather than guessing
     */
    public static Optional<CameraSettings> of(ResourceLocation item) {
        return Optional.ofNullable(byItem.get(item));
    }

    /** @return every camera that is loaded, by the id of its item */
    public static Map<ResourceLocation, CameraSettings> all() {
        return byItem;
    }

    static void replaceAll(Map<ResourceLocation, CameraSettings> settings) {
        byItem = Map.copyOf(settings);
    }
}
