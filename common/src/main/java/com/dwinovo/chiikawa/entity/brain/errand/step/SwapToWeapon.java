package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.combat.Arms;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;

/**
 * Takes the weapon out of the bag into the hand, the tool that was in the hand going into the bag in its place, so that
 * the pet fights as a fencer or an archer for as long as the swap lasts. Gives up if the weapon is not in the bag after
 * all. Done in the tick it takes, so it is never cut half way.
 *
 * <p>The tool is the pet's to take back, by {@link #swapBack}, when the errand ends however it does; that and this step are
 * the only writers of the tool put aside, and it is never saved, see {@code AbstractPet#lendTool}.
 */
public final class SwapToWeapon implements Step {
    private final Arms.Weapon weapon;

    /** @param weapon the weapon the pet means to take up, and where it has it */
    public SwapToWeapon(Arms.Weapon weapon) {
        this.weapon = weapon;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        if (!Arms.of(pet).swap().filter(weapon::equals).isPresent()) {
            return StepStatus.gaveUp("no_weapon");
        }
        pet.lendTool(weapon.slot());
        return StepStatus.DONE;
    }

    /** The tool goes back in the hand and the weapon into the bag. Safe to call at any time, whether or not the pet took a weapon up. */
    public static void swapBack(Handle handle) {
        LiveHandle.of(handle).pet().takeToolBack();
    }
}
