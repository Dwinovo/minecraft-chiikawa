package com.dwinovo.chiikawa.entity.brain.errand.view;

import com.dwinovo.chiikawa.entity.AbstractPet;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Whether a bed is one a pet may lie on, and where a pet that may not sleeps instead: the one account of it, as a cat's
 * {@code isValidTarget} is for the goal that looks for a bed and the goal that goes on lying on it. A pet asks when it
 * chooses to go, and again when it gets there, for at nightfall every pet chooses at once and the bed is free to all of them
 * until one lies down.
 */
public final class PetBed {
    /** How near the bed a pet stands to count as on it: a pet against its side steps up onto it at the next push. */
    private static final double AGAINST = 0.3;

    private PetBed() {
    }

    /**
     * @param pet the pet that would lie on it
     * @param head the head of the bed
     * @return whether it is a bed still, with no player in it, as the game keeps count with {@code OCCUPIED}, and no other
     *         pet on either half of it
     */
    public static boolean isFreeFor(AbstractPet pet, BlockPos head) {
        Level level = pet.level();
        BlockState state = level.getBlockState(head);
        if (!state.is(BlockTags.BEDS) || state.getOptionalValue(BedBlock.OCCUPIED).orElse(false)) {
            return false;
        }
        return level.getEntitiesOfClass(AbstractPet.class, halves(state, head), other -> other != pet).isEmpty();
    }

    /**
     * Where a pet that has come to a bed it cannot lie on lies down instead. A bed is a step a pet walks up, so one that came
     * to it on foot stands on it or against it, and it is set down on the floor beside it, as near to where it is as there is
     * room for it, and no other pet there.
     *
     * @return that place; none if the pet is not on the bed or against it, or there is no floor beside the bed to put it on
     */
    public static Optional<Vec3> groundBeside(AbstractPet pet, BlockPos head) {
        Level level = pet.level();
        BlockState state = level.getBlockState(head);
        if (!state.is(BlockTags.BEDS) || !pet.getBoundingBox().inflate(AGAINST).intersects(halves(state, head))) {
            return Optional.empty();
        }
        BlockPos foot = foot(state, head);
        return Stream.of(head, foot)
            .flatMap(half -> Direction.Plane.HORIZONTAL.stream().map(half::relative))
            .filter(cell -> !cell.equals(head) && !cell.equals(foot))
            .map(cell -> new Vec3(cell.getX() + 0.5, cell.getY(), cell.getZ() + 0.5))
            .filter(spot -> roomOnTheFloor(pet, spot))
            .min(Comparator.comparingDouble(spot -> spot.distanceToSqr(pet.position())));
    }

    /** Whether the pet fits at this place, has a floor under it, and is not on top of another pet. */
    private static boolean roomOnTheFloor(AbstractPet pet, Vec3 spot) {
        Level level = pet.level();
        AABB there = pet.getBoundingBox().move(spot.subtract(pet.position()));
        return level.noCollision(pet, there) && !level.noCollision(pet, there.move(0.0, -0.1, 0.0))
            && level.getEntitiesOfClass(AbstractPet.class, there, other -> other != pet).isEmpty();
    }

    /** Both halves of the bed, as one box. */
    private static AABB halves(BlockState state, BlockPos head) {
        return new AABB(head).minmax(new AABB(foot(state, head)));
    }

    private static BlockPos foot(BlockState state, BlockPos head) {
        return state.getOptionalValue(BedBlock.FACING).map(facing -> head.relative(facing.getOpposite())).orElse(head);
    }
}
