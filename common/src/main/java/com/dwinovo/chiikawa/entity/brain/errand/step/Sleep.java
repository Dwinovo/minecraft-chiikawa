package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepTag;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Lies down and sleeps, as its character does: on the bed it was walking to, if it still can, and otherwise where it
 * stands. It only lies there, as a cat lies on a bed: it never goes to sleep in it, so the bed is not taken from a player
 * who comes to it, and it does not stop one getting in. Done when it wakes, with the morning or with the owner whose
 * sleep it shares. Always interruptible, and whatever matters more wakes it.
 */
public final class Sleep implements Step {
    /** How high the top of a bed is above the floor it stands on: nine pixels of sixteen. */
    private static final double BED_TOP = 9.0 / 16.0;

    /** What wakes the pet. */
    public enum Wake {
        /** The morning: the night is over. */
        AT_MORNING,
        /** The owner, who is asleep, getting up. */
        WITH_OWNER
    }

    private final Optional<BlockPos> bed;
    private final Wake wake;
    private final ErrandTunings.Performances sleeps;
    private String performance = "";

    /**
     * @param bed the bed it lies on, if it is to
     * @param wake what wakes it
     * @param sleeps how each character sleeps
     */
    public Sleep(Optional<BlockPos> bed, Wake wake, ErrandTunings.Performances sleeps) {
        this.bed = bed;
        this.wake = wake;
        this.sleeps = sleeps;
    }

    @Override
    public void begin(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        performance = sleeps.performanceOf(BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType()));
        handle.stopWalking();
        handle.clearLook();
        bed.ifPresent(head -> lieOn(handle, pet.level(), head));
    }

    /** Sets the pet down on the bed, along it, unless something has made it no bed for a pet since: a player in it. */
    private static void lieOn(Handle handle, Level level, BlockPos head) {
        BlockState state = level.getBlockState(head);
        if (state.is(BlockTags.BEDS) && !state.getOptionalValue(BedBlock.OCCUPIED).orElse(false)) {
            handle.snapTo(new Vec3(head.getX() + 0.5, head.getY() + BED_TOP, head.getZ() + 0.5),
                state.getOptionalValue(BedBlock.FACING).map(facing -> facing.toYRot()).orElse(0.0F));
        }
    }

    @Override
    public StepStatus tick(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        boolean asleep = switch (wake) {
            case AT_MORNING -> DayPhase.in(pet.level()) == DayPhase.NIGHT;
            case WITH_OWNER -> LiveHandle.of(handle).owner().filter(Player::isSleeping).isPresent();
        };
        return asleep ? StepStatus.RUNNING : StepStatus.DONE;
    }

    @Override
    public String performance() {
        return performance;
    }

    @Override
    public Set<StepTag> tags() {
        return Set.of(StepTag.SLEEPING);
    }
}
