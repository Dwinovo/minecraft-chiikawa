package com.dwinovo.chiikawa.entity.brain.task.tameable;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

/**
 * Hard bound on how far a pet strays from its {@link PetAnchor}. Always active in
 * CORE and reads only the anchor, whatever the pet is doing.
 *
 * <p>Past the teleport distance (only while following the owner) the pet is
 * teleported to the owner. Past the leash, a pet that is not already walking
 * somewhere walks back to the anchor center; intents that steer the pet themselves
 * end on their own once their target leaves the leash. Anchors of pets that cannot
 * move, or are leashed or riding, never pull.
 */
public class AnchorLeashBehavior extends Behavior<AbstractPet> {
    private static final Map<MemoryModuleType<?>, MemoryStatus> REQUIRED_MEMORIES = ImmutableMap.of(
        MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED
    );
    private static final float RETURN_SPEED = 0.6F;

    public AnchorLeashBehavior() {
        super(REQUIRED_MEMORIES, 15);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet pet) {
        PetAnchor anchor = PetConstraints.anchorOf(pet, PetOwnership.of(pet));
        GlobalPos petPos = GlobalPos.of(level.dimension(), pet.blockPosition());
        if (!anchor.canMove() || anchor.withinLeash(petPos)) {
            return false;
        }
        return anchor.beyondTeleport(petPos) || !pet.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        PetAnchor anchor = PetConstraints.anchorOf(pet, PetOwnership.of(pet));
        if (anchor.beyondTeleport(GlobalPos.of(level.dimension(), pet.blockPosition()))) {
            LivingEntity owner = pet.getOwner();
            if (owner != null) {
                pet.teleportToOwner(level, owner);
            }
            return;
        }
        pet.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(anchor.center().pos(), RETURN_SPEED, 0));
    }
}
