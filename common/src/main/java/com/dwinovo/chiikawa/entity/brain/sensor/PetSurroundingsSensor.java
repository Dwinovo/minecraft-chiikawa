package com.dwinovo.chiikawa.entity.brain.sensor;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.view.OwnerSeen;
import com.dwinovo.chiikawa.entity.brain.errand.view.Surroundings;
import com.dwinovo.chiikawa.entity.brain.errand.view.Surroundings.Sighting;
import com.dwinovo.chiikawa.init.InitMemory;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Looks at what is within a few blocks of the pet, as {@link SurroundingsSettings} says how far, and remembers it for the
 * pet's pastimes to go by: somewhere to get out of the rain, the creatures and the things lying about or held in a hand, a
 * creeper about to go off, and whether its owner has just come back after a while away or has left off a long spell of work.
 * It only looks; what a pet makes of it is for its errands to say, through the pet's view.
 *
 * <p>Shelter is looked for only while it rains, which is the only time anything asks for it. It is a place the pet could
 * stand where the rain does not fall, see {@link #exposed}: under a roof, a ledge or the leaves of a tree.
 */
public class PetSurroundingsSensor extends Sensor<AbstractPet> {
    /** A pet can stand in a place where there is room for it: a floor, and two blocks of air above. */
    private static final int HEADROOM = 2;
    /** How far above and below itself the pet looks for shelter. */
    private static final int SHELTER_REACH_Y = 2;

    public PetSurroundingsSensor() {
        // What lies about changes slowly, and a pet notices a creeper in a couple of seconds: every two seconds is plenty.
        super(40);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(InitMemory.SURROUNDINGS.get(), InitMemory.OWNER_SEEN.get());
    }

    @Override
    protected void doTick(ServerLevel level, AbstractPet pet) {
        Brain<AbstractPet> brain = pet.getBrain();
        Optional<SurroundingsSettings> settings = SurroundingsSettings.current();
        if (settings.isEmpty()) {
            brain.eraseMemory(InitMemory.SURROUNDINGS.get());
            brain.eraseMemory(InitMemory.OWNER_SEEN.get());
            return;
        }
        brain.setMemory(InitMemory.SURROUNDINGS.get(), look(level, pet, settings.get().radius()));
        if (pet.getOwner() instanceof Player owner && owner.isAlive() && owner.level() == level
                && owner.distanceToSqr(pet) <= (double) settings.get().radius() * settings.get().radius()) {
            brain.setMemory(InitMemory.OWNER_SEEN.get(), OwnerSeen.seen(
                brain.getMemory(InitMemory.OWNER_SEEN.get()).orElse(null), level.getGameTime(), settings.get().awayTicks(),
                atWork(owner), settings.get().settleTicks()));
        }
    }

    /**
     * @param owner a player
     * @return whether they are at work, as an owner is seen to be: swinging at it, a tool or a block in the hand
     */
    public static boolean atWork(Player owner) {
        return owner.isAlive() && owner.swinging
            && (owner.getMainHandItem().getItem() instanceof DiggerItem || owner.getMainHandItem().getItem() instanceof BlockItem);
    }

    private static Surroundings look(ServerLevel level, AbstractPet pet, int radius) {
        var around = pet.getBoundingBox().inflate(radius);
        double reachSqr = (double) radius * radius;
        List<LivingEntity> creatures = level.getEntitiesOfClass(LivingEntity.class, around,
            seen -> seen != pet && seen.isAlive() && seen.distanceToSqr(pet) <= reachSqr);
        List<ItemEntity> lying = level.getEntitiesOfClass(ItemEntity.class, around,
            seen -> seen.isAlive() && seen.distanceToSqr(pet) <= reachSqr);
        // A thing is about, to a pet, whether it lies on the ground or is held up in somebody's hand: either is where it is.
        List<Seen<Item>> items = new ArrayList<>();
        lying.forEach(seen -> items.add(new Seen<>(seen, seen.getItem().getItem())));
        for (LivingEntity holder : creatures) {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack held = holder.getItemInHand(hand);
                if (!held.isEmpty()) {
                    items.add(new Seen<>(holder, held.getItem()));
                }
            }
        }
        return new Surroundings(
            level.isRaining() ? shelterNear(level, pet.blockPosition(), radius) : Optional.empty(),
            creatures.stream().sorted(Comparator.comparingDouble(pet::distanceToSqr))
                .<Sighting<EntityType<?>>>map(seen -> new Sighting<>(seen.getUUID(), seen.blockPosition(), seen.getType()))
                .toList(),
            items.stream().sorted(Comparator.comparingDouble(seen -> pet.distanceToSqr(seen.where())))
                .<Sighting<Item>>map(seen -> new Sighting<>(seen.where().getUUID(), seen.where().blockPosition(), seen.kind()))
                .toList(),
            creatures.stream().anyMatch(seen -> seen instanceof Creeper creeper && (creeper.isIgnited() || creeper.getSwellDir() > 0)));
    }

    /** A kind of thing, and the entity it is found at: lying there, or held by it. */
    private record Seen<T>(Entity where, T kind) {
    }

    /** @return the nearest place within the radius where the pet could stand and the rain does not fall */
    private static Optional<BlockPos> shelterNear(Level level, BlockPos origin, int radius) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = (double) dx * dx + (double) dz * dz;
                if (distance > (double) radius * radius || distance >= bestDistance) {
                    continue;
                }
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                for (int dy = -SHELTER_REACH_Y; dy <= SHELTER_REACH_Y; dy++) {
                    BlockPos spot = new BlockPos(x, origin.getY() + dy, z);
                    if (!exposed(level, spot) && standable(level, spot)) {
                        best = spot;
                        bestDistance = distance;
                        break;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * @return whether the rain falls on this block, as the game works that out for a block: nothing that stops motion above
     *         it. A glass roof keeps the rain off as a stone one does
     */
    public static boolean exposed(Level level, BlockPos pos) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY();
    }

    /** @return whether a pet could stand in this block: a sturdy floor under it and nothing in the way above */
    private static boolean standable(Level level, BlockPos spot) {
        BlockPos floor = spot.below();
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
            return false;
        }
        for (int up = 0; up < HEADROOM; up++) {
            BlockPos at = spot.above(up);
            if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty() || !level.getFluidState(at).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
