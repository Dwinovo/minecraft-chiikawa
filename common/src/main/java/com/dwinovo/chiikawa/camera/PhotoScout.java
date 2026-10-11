package com.dwinovo.chiikawa.camera;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * rules, and the choice among the good moments is {@link PhotoSubjects}, which is pure. The choice
 * is made here, as the pet decides what to do, and carried by a {@link PhotoPlan}.
 *
 * <p>Only reads. Called every time a pet decides what to do next, so it must not change the
 * world or the pet.
 */
public final class PhotoScout {
    private PhotoScout() {
    }

    /**
     * A {@link PhotoPlan} with its subject found: what the behavior carries out.
     *
     * @param kind the kind of photo
     * @param target who is in it, when somebody is
     * @param name who is in it, by name, for the photo's caption; nothing for a view
     * @param facing which way a pet that has nobody to turn to faces
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
        Optional<PhotoPlan> plan = camera.isEmpty() ? Optional.empty()
            : remembered(pet).or(() -> chosen(pet, camera.get(), owner));
        return new PhotoOutlook(true, camera, hasFilm, ownerInRange, ownerCalm(owner),
            pet.getBrain().hasMemoryValue(InitMemory.PHOTO_COOLDOWN.get()), plan);
    }

    /**
     * The plan the pet is carrying out, if it has one and its subject is still about, with the subject
     * where it is now: a pet that has set off for somebody goes on for them, not for whoever it would
     * pick this time.
     */
    private static Optional<PhotoPlan> remembered(AbstractPet pet) {
        return pet.getBrain().getMemory(InitMemory.PHOTO_PLAN.get()).flatMap(plan -> {
            if (plan.subject().isEmpty()) {
                return Optional.of(plan);
            }
            return subjectOf(pet.level(), plan).map(subject -> plan.at(positionOf(subject)));
        });
    }

    /**
     * What the pet would take a photo of, and who it would turn to for it: drawn from the moments there
     * are by what it fancies.
     */
    private static Optional<PhotoPlan> chosen(AbstractPet pet, CameraSettings camera, LivingEntity owner) {
        return PhotoSubjects.choose(moments(pet, owner), camera.subjects(), pet.getRandom()).flatMap(kind -> {
            Vec3 facing = facing(pet);
            return switch (kind) {
                case OWNER -> Optional.ofNullable(owner).map(person -> planFor(kind, person, person.getName().getString(), facing));
                case FRIEND -> friend(pet).map(friend -> planFor(kind, friend, friend.getDisplayName().getString(), facing));
                case SCENERY -> Optional.of(new PhotoPlan(kind, Optional.empty(), "", facing, Optional.empty()));
            };
        });
    }

    private static PhotoPlan planFor(PhotoSubject kind, LivingEntity subject, String name, Vec3 facing) {
        return new PhotoPlan(kind, Optional.of(subject.getUUID()), name, facing, Optional.of(positionOf(subject)));
    }

    private static GlobalPos positionOf(LivingEntity entity) {
        return GlobalPos.of(entity.level().dimension(), entity.blockPosition());
    }

    private static Optional<LivingEntity> subjectOf(Level level, PhotoPlan plan) {
        return plan.subject().flatMap(id -> level instanceof ServerLevel server && server.getEntity(id) instanceof LivingEntity found
            && found.isAlive() ? Optional.of(found) : Optional.empty());
    }

    /**
     * The plan with its subject found, nothing when the subject is gone by the time the pet comes to it.
     * Hands the entity to the behavior, which carries the plan out.
     */
    public static Optional<Shot> resolve(Level level, PhotoPlan plan) {
        if (plan.subject().isEmpty()) {
            return Optional.of(new Shot(plan.kind(), Optional.empty(), plan.name(), plan.facing()));
        }
        return subjectOf(level, plan).map(subject -> new Shot(plan.kind(), Optional.of(subject), plan.name(), plan.facing()));
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

    /** The kinds of photo there is a good moment for now. */
    private static Set<PhotoSubject> moments(AbstractPet pet, LivingEntity owner) {
        Set<PhotoSubject> moments = EnumSet.noneOf(PhotoSubject.class);
        Optional<NearestVisibleLivingEntities> visible = pet.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if (owner != null && visible.filter(seen -> seen.contains(owner)).isPresent()) {
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
                for (LivingEntity entity : visible.findAll(other -> other instanceof AbstractPet && other != pet && other.isAlive())) {
                    pets.add((AbstractPet) entity);
                }
                return pets;
            }).orElse(List.of());
        Comparator<AbstractPet> nearest = Comparator.comparingDouble(pet::distanceToSqr);
        Optional<AbstractPet> inAMoment = seen.stream().filter(PhotoScout::inAMoment).min(nearest);
        return inAMoment.isPresent() ? inAMoment : seen.stream().filter(PhotoScout::idle).min(nearest);
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
