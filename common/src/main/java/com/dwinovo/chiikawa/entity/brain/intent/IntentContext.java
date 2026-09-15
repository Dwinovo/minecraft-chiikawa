package com.dwinovo.chiikawa.entity.brain.intent;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.entity.brain.task.musician.PlayMusicBehavior;
import com.dwinovo.chiikawa.utils.Utils;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;

/**
 * Plain-data snapshot an intent's conditions and score read, built once per
 * evaluation. It holds the anchor derived from the directive but never the
 * directive itself, and no entity or level, so intents cannot bypass the
 * permission table or change the world while being scored. Tests build it by hand.
 *
 * @param petPos the pet's block position
 * @param phase the part of the day in the pet's level
 * @param ownership who the pet belongs to
 * @param personality how the pet's kind leans
 * @param anchor where the pet may act
 * @param targets what the sensors remember
 * @param attackCoolingDown whether the pet's attack cooldown is running
 * @param hasArrows whether the pet carries arrows
 * @param hasNewMusicSelection whether the held music box selects a song the pet has not started yet
 * @param playingMusic whether the pet is performing
 */
public record IntentContext(
    GlobalPos petPos,
    DayPhase phase,
    PetOwnership ownership,
    Personality personality,
    PetAnchor anchor,
    PerceivedTargets targets,
    boolean attackCoolingDown,
    boolean hasArrows,
    boolean hasNewMusicSelection,
    boolean playingMusic
) {
    public static IntentContext capture(AbstractPet pet, PetOwnership ownership) {
        Level level = pet.level();
        return new IntentContext(
            GlobalPos.of(level.dimension(), pet.blockPosition()),
            // A level without a day cycle, like the Nether, stays at its fixed time of day.
            DayPhase.of(level.dimensionType().fixedTime().orElse(level.getDayTime())),
            ownership,
            PetPersonalities.of(pet.getType()),
            PetConstraints.anchorOf(pet, ownership),
            PerceivedTargets.capture(pet),
            pet.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN),
            !Utils.getArrow(pet).isEmpty(),
            PlayMusicBehavior.unplayedSelection(pet).isPresent(),
            pet.getActivity() == PetActivity.PLAY_GUITAR
        );
    }
}
