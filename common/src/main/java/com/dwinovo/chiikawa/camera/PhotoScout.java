package com.dwinovo.chiikawa.camera;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.intent.DayPhase;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.entity.impl.HachiwarePet;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.social.InteractionPlan;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Looks round a pet for a photo to take, and for what it would take it of. This is where a
 * pet is read: the facts go into a {@link PhotoOutlook}, which is plain data and has the
 * rules, and the choice among the good moments is {@link PhotoSubjects}, which is pure.
 *
 * <p>Only reads. Called every time a pet decides what to do next, so it must not change the
 * world or the pet.
 */
public final class PhotoScout {
    private PhotoScout() {
    }

    /**
     * What a pet means to take a photo of.
     *
     * @param kind the kind of photo
     * @param target who is in it, when somebody is
     * @param name who is in it, by name, for the photo's caption; nothing for a view
     * @param facing which way a pet that has nobody to turn to faces: the sun when it is low, or as it was looking
     */
    public record Shot(PhotoSubject kind, Optional<LivingEntity> target, String name, Vec3 facing) {
    }

    /** The settings of the camera a pet carries, the first it comes to. */
    public static Optional<CameraSettings> cameraOf(AbstractPet pet) {
        for (int slot = 0; slot < pet.getBackpack().getContainerSize(); slot++) {
            ItemStack stack = pet.getBackpack().getItem(slot);
            if (!stack.isEmpty()) {
                Optional<CameraSettings> camera = CameraSettings.of(BuiltInRegistries.ITEM.getKey(stack.getItem()));
                if (camera.isPresent()) {
                    return camera;
                }
            }
        }
        return Optional.empty();
    }

    /** The film a camera takes: air, which no pet carries, when a pack names an item there is none of. */
    public static Item filmOf(CameraSettings camera) {
        return BuiltInRegistries.ITEM.getOptional(camera.film()).orElse(Items.AIR);
    }

    public static PhotoOutlook outlook(AbstractPet pet) {
        // Only Hachiware: the one who loves taking photos.
        if (!(pet instanceof HachiwarePet)) {
            return PhotoOutlook.NONE;
        }
        Optional<CameraSettings> camera = cameraOf(pet);
        boolean hasFilm = camera.filter(settings -> FilmRolls.hasRoom(pet.getBackpack(), filmOf(settings))).isPresent();
        LivingEntity owner = pet.getOwner();
        boolean ownerInRange = owner instanceof ServerPlayer && owner.isAlive()
            && camera.filter(settings -> pet.distanceTo(owner) <= settings.ownerRange()).isPresent();
        return new PhotoOutlook(true, camera, hasFilm, ownerInRange, ownerCalm(owner),
            pet.getBrain().hasMemoryValue(InitMemory.PHOTO_COOLDOWN.get()), moments(pet, owner));
    }

    /**
     * What the pet takes a photo of, and who it turns to for it, drawn from the moments there
     * are by what it fancies.
     */
    public static Optional<Shot> plan(AbstractPet pet, PhotoOutlook outlook, RandomSource random) {
        if (outlook.camera().isEmpty()) {
            return Optional.empty();
        }
        return PhotoSubjects.choose(outlook.moments(), outlook.camera().get().subjects(), random).flatMap(kind -> {
            Vec3 facing = facing(pet);
            return switch (kind) {
                case OWNER -> Optional.ofNullable(pet.getOwner())
                    .map(owner -> new Shot(kind, Optional.of(owner), owner.getName().getString(), facing));
                case FRIEND -> friend(pet)
                    .map(friend -> new Shot(kind, Optional.of(friend), friend.getDisplayName().getString(), facing));
                case SCENERY -> Optional.of(new Shot(kind, Optional.empty(), "", facing));
            };
        });
    }

    /** The biome the pet stands in. */
    public static Optional<ResourceLocation> biomeOf(AbstractPet pet) {
        return pet.level().getBiome(pet.blockPosition()).unwrapKey().map(ResourceKey::location);
    }

    /** Hurt just now, riding, or flying, in any of the ways the owner's own game has enough to do already. */
    private static boolean ownerCalm(LivingEntity owner) {
        return owner != null && owner.isAlive() && !owner.isSpectator() && !owner.isSleeping()
            && !owner.isPassenger() && !owner.isFallFlying()
            && !(owner instanceof Player player && player.getAbilities().flying)
            // The game keeps the last thing that hurt someone for two seconds.
            && owner.getLastDamageSource() == null;
    }

    /**
     * The kinds of photo there is a good moment for. Which one the pet takes is picked as it sets
     * about it, so the area it acts in is kept to here: a person is a moment only where the pet's
     * anchor admits going to them, as it does for any errand, while a view is taken from where it stands.
     */
    private static Set<PhotoSubject> moments(AbstractPet pet, LivingEntity owner) {
        Set<PhotoSubject> moments = EnumSet.noneOf(PhotoSubject.class);
        Optional<NearestVisibleLivingEntities> visible = pet.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if (owner != null && visible.filter(seen -> seen.contains(owner)).isPresent()
                && admitted(pet, owner)) {
            moments.add(PhotoSubject.OWNER);
        }
        if (friend(pet).isPresent()) {
            moments.add(PhotoSubject.FRIEND);
        }
        if (sceneryWorthTaking(pet)) {
            moments.add(PhotoSubject.SCENERY);
        }
        return moments;
    }

    /**
     * A pet nearby that is worth a photo: one in a little scene, listening to music or just
     * reading its exam results, the nearest of those; otherwise the nearest that is idle.
     */
    private static Optional<AbstractPet> friend(AbstractPet pet) {
        List<AbstractPet> seen = pet.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            .map(visible -> {
                List<AbstractPet> pets = new ArrayList<>();
                for (LivingEntity entity : visible.findAll(other -> other instanceof AbstractPet && other != pet && other.isAlive()
                        && admitted(pet, other))) {
                    pets.add((AbstractPet) entity);
                }
                return pets;
            }).orElse(List.of());
        Comparator<AbstractPet> nearest = Comparator.comparingDouble(pet::distanceToSqr);
        Optional<AbstractPet> inAMoment = seen.stream().filter(PhotoScout::inAMoment).min(nearest);
        return inAMoment.isPresent() ? inAMoment : seen.stream().filter(PhotoScout::idle).min(nearest);
    }

    /** Whether the pet's anchor admits setting off for somebody: the one rule every errand goes by. */
    private static boolean admitted(AbstractPet pet, LivingEntity subject) {
        return PetConstraints.anchorOf(pet, PetOwnership.of(pet))
            .admits(GlobalPos.of(subject.level().dimension(), subject.blockPosition()), true);
    }

    /** In a scene with another pet, listening to music (which is one), or reading the results of an exam. */
    private static boolean inAMoment(AbstractPet pet) {
        return !pet.getPerformance().isEmpty() || pet.getActivity() == PetActivity.READ_RESULTS
            || pet.getBrain().getMemory(InitMemory.INTERACTION_PLAN.get())
                .filter(InteractionPlan::engaged).isPresent();
    }

    /** With nothing to do: sitting where it was told to, or wandering about. */
    private static boolean idle(AbstractPet pet) {
        return pet.getIntent().map(intent -> intent.equals(PetIntents.WANDER) || intent.equals(PetIntents.STAY)).orElse(true);
    }

    /**
     * A view: the sun while it is low, which is the hour a view is worth a photo, or a biome
     * the pet has not been photographing yet.
     */
    private static boolean sceneryWorthTaking(AbstractPet pet) {
        return sunIsLow(pet) || biomeOf(pet).filter(biome -> !photographed(pet).contains(biome)).isPresent();
    }

    private static boolean sunIsLow(AbstractPet pet) {
        Level level = pet.level();
        DayPhase phase = DayPhase.of(level.dimensionType().fixedTime().orElse(level.getDayTime()));
        return (phase == DayPhase.MORNING || phase == DayPhase.EVENING) && level.canSeeSky(pet.blockPosition());
    }

    /** The biomes the pet has taken a photo in. */
    public static List<ResourceLocation> photographed(AbstractPet pet) {
        return pet.getBrain().getMemory(InitMemory.PHOTOGRAPHED_BIOMES.get()).orElse(List.of());
    }

    /**
     * Which way to face with nobody to turn to: the sun when it is low, east in the morning
     * and west in the evening, or the way the pet is looking.
     */
    private static Vec3 facing(AbstractPet pet) {
        if (sunIsLow(pet)) {
            // The sun's path runs east to west across the sky, and it is east of the world before noon.
            return new Vec3(Math.cos(pet.level().getSunAngle(1.0F)) >= 0.0 ? 1.0 : -1.0, 0.0, 0.0);
        }
        Vec3 looking = pet.getViewVector(1.0F);
        Vec3 level = new Vec3(looking.x, 0.0, looking.z);
        return level.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : level.normalize();
    }
}
