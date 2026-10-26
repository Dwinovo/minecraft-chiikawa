package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.view.Attacker;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Runs from whatever hurt the pet, to somewhere better: behind its owner as seen from what it runs from, if the owner is
 * near; otherwise home, if it has one; otherwise straight away. The spot is worked out when the pet sets off, from the
 * world as it is then, and the run is to that spot, even if the owner moves on. The pet is done when it is there, or as
 * far as the brain's mover takes it. Always interruptible.
 */
public final class RunTo implements Step {
    private final ErrandTunings.Flee tuning;
    private Vec3 spot;
    /** Whether the pet could look for a way when the brain's mover last had its turn, which is what its report is worth. */
    private boolean lookedFromTheGround;

    /** @param tuning how fast, how near the owner has to be and how far */
    public RunTo(ErrandTunings.Flee tuning) {
        this.tuning = tuning;
    }

    @Override
    public void begin(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        Optional<Vec3> danger = Attacker.of(live.pet()).flatMap(attacker -> live.creature(attacker.id())).map(Entity::position);
        spot = spotFrom(live, danger);
        handle.stopWalking();
        lookedFromTheGround = handle.canFindWay();
        handle.walkTo(spot, tuning.speed(), 0);
    }

    @Override
    public StepStatus tick(Handle handle) {
        double arrive = tuning.arriveDistance();
        if (LiveHandle.of(handle).pet().position().distanceToSqr(spot) <= arrive * arrive) {
            return StepStatus.DONE;
        }
        // The mover lets the walk go when it has taken the pet as far along a path as the path goes: as near as it gets.
        // What it said about a turn it took in the air is that it could not look, not that there is no way.
        boolean blocked = lookedFromTheGround && handle.blockedTicks().isPresent();
        lookedFromTheGround = handle.canFindWay();
        if (blocked && !handle.walking()) {
            return StepStatus.DONE;
        }
        handle.walkTo(spot, tuning.speed(), 0);
        return StepStatus.RUNNING;
    }

    @Override
    public void release(Handle handle) {
        handle.stopWalking();
    }

    private Vec3 spotFrom(LiveHandle live, Optional<Vec3> danger) {
        AbstractPet pet = live.pet();
        Optional<Player> owner = live.owner().filter(near -> near.level() == pet.level() && near.distanceTo(pet) <= tuning.ownerRange());
        if (owner.isPresent()) {
            Vec3 at = owner.get().position();
            return danger.map(from -> at.add(awayFrom(from, at).scale(tuning.behindOwner()))).orElse(at);
        }
        Optional<GlobalPos> home = pet.getBrain().getMemory(MemoryModuleType.HOME)
            .filter(found -> found.dimension().equals(pet.level().dimension()));
        if (home.isPresent()) {
            return Vec3.atBottomCenterOf(home.get().pos());
        }
        return danger.map(from -> pet.position().add(awayFrom(from, pet.position()).scale(tuning.awayStep()))).orElse(pet.position());
    }

    /** @return the level heading that takes something at {@code point} straight away from {@code danger} */
    private static Vec3 awayFrom(Vec3 danger, Vec3 point) {
        return new Vec3(point.x - danger.x, 0.0, point.z - danger.z).normalize();
    }
}
