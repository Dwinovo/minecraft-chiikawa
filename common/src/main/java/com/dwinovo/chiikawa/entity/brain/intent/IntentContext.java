package com.dwinovo.chiikawa.entity.brain.intent;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.task.musician.PlayMusicBehavior;
import com.dwinovo.chiikawa.utils.Utils;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Plain-data snapshot an intent's conditions and score read, built once per
 * evaluation. It holds the anchor derived from the directive but never the
 * directive itself, and no entity or level, so intents cannot bypass the
 * permission table or change the world while being scored. Tests build it by hand.
 *
 * @param petPos the pet's block position
 * @param anchor where the pet may act
 * @param targets what the sensors remember
 * @param attackCoolingDown whether the pet's attack cooldown is running
 * @param hasArrows whether the pet carries arrows
 * @param hasNewMusicSelection whether the held music box selects a song the pet has not started yet
 * @param playingMusic whether the pet is performing
 */
public record IntentContext(
    GlobalPos petPos,
    PetAnchor anchor,
    PerceivedTargets targets,
    boolean attackCoolingDown,
    boolean hasArrows,
    boolean hasNewMusicSelection,
    boolean playingMusic
) {
    public static IntentContext capture(AbstractPet pet, PetOwnership ownership) {
        return new IntentContext(
            GlobalPos.of(pet.level().dimension(), pet.blockPosition()),
            PetConstraints.anchorOf(pet, ownership),
            PerceivedTargets.capture(pet),
            pet.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN),
            !Utils.getArrow(pet).isEmpty(),
            PlayMusicBehavior.unplayedSelection(pet).isPresent(),
            pet.getActivity() == PetActivity.PLAY_GUITAR
        );
    }
}
