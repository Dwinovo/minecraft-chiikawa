package com.dwinovo.chiikawa.camera;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.impl.HachiwarePet;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.social.InteractionPlan;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
 * rules, with a {@link PhotoPlan} for every good moment there is. The choice among the moments is
 * not made here: the errand offers them, weighted by what the pet fancies, and the decision draws.
 *
 * <p>Only reads, and draws nothing from the pet's random. Called every time a pet decides what to
 * do next, and by the debug command, so it must not change the world or the pet.
 */
public final class PhotoScout {
    private PhotoScout() {
    }

    /** A camera a pet carries: the item it is and how it behaves. */
    private record Equipped(ResourceLocation item, CameraSettings settings) {
    }

    /** The camera a pet carries, the first it comes to. */
    private static Optional<Equipped> equipped(AbstractPet pet) {
        for (int slot = 0; slot < pet.getBackpack().getContainerSize(); slot++) {
            ItemStack stack = pet.getBackpack().getItem(slot);
            if (!stack.isEmpty()) {
                ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
                Optional<CameraSettings> camera = CameraSettings.of(item);
                if (camera.isPresent()) {
                    return Optional.of(new Equipped(item, camera.get()));
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
        Optional<Equipped> equipped = equipped(pet);
        Optional<CameraSettings> camera = equipped.map(Equipped::settings);
        boolean hasFilm = camera.filter(settings -> FilmRolls.hasRoom(pet.getBackpack(), filmOf(settings))).isPresent();
        LivingEntity owner = pet.getOwner();
        boolean ownerInRange = owner instanceof ServerPlayer && owner.isAlive()
            && camera.filter(settings -> pet.distanceTo(owner) <= settings.ownerRange()).isPresent();
        List<PhotoPlan> moments = equipped.map(found -> moments(pet, owner, found.item())).orElse(List.of());
        return new PhotoOutlook(true, camera, hasFilm, ownerInRange, ownerCalm(owner), moments);
    }

    /**
     * What the pet could take a photo of, and who it would turn to for it: a plan for every kind of photo
     * there is a good moment for now, in the order of the kinds.
     */
    private static List<PhotoPlan> moments(AbstractPet pet, LivingEntity owner, ResourceLocation camera) {
        List<PhotoPlan> moments = new ArrayList<>();
        Optional<NearestVisibleLivingEntities> visible = pet.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        Optional<AbstractPet> friend = friend(pet);
        Vec3 facing = facing(pet);
        if (owner != null && visible.filter(seen -> seen.contains(owner)).isPresent()) {
            moments.add(planFor(camera, PhotoSubject.OWNER, owner, owner.getName().getString(), facing));
        }
        friend.ifPresent(found -> moments.add(planFor(camera, PhotoSubject.FRIEND, found, found.getDisplayName().getString(), facing)));
        if (sceneryWorthTaking(pet)) {
            moments.add(new PhotoPlan(camera, PhotoSubject.SCENERY, Optional.empty(), "", facing));
        }
        return moments;
    }

    private static PhotoPlan planFor(ResourceLocation camera, PhotoSubject kind, LivingEntity subject, String name, Vec3 facing) {
        return new PhotoPlan(camera, kind, Optional.of(subject.getUUID()), name, facing);
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
        return pet.getIntent().map(intent -> intent.equals(ErrandIds.WANDER) || intent.equals(ErrandIds.STAY)).orElse(true);
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
     * The way along the horizon the sun is in, east at sunrise and west at sunset. The sky turns the
     * sun's disc about the east-west axis, a quarter turn from where it stands overhead, which puts
     * it at {@code -sin(angle)} along the world's x axis: positive, east, before noon.
     *
     * @param sunAngle the sun's angle in the sky, as the level gives it
     */
    static Vec3 towardsSun(float sunAngle) {
        return new Vec3(-Math.sin(sunAngle) >= 0.0 ? 1.0 : -1.0, 0.0, 0.0);
    }

    /**
     * Which way to face with nobody to turn to: the sun when it is low, or the way the pet is looking.
     */
    private static Vec3 facing(AbstractPet pet) {
        if (sunIsLow(pet)) {
            return towardsSun(pet.level().getSunAngle(1.0F));
        }
        Vec3 looking = pet.getViewVector(1.0F);
        Vec3 level = new Vec3(looking.x, 0.0, looking.z);
        return level.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : level.normalize();
    }
}
