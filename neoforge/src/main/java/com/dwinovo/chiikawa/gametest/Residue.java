package com.dwinovo.chiikawa.gametest;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * What a pet that has left an errand must not still be holding: a walk, a gaze, a path, a pose, a scene,
 * a drawn bow, a present in its paws, and whatever the errand's scenario says is its own to put right. An
 * errand that is cut short leaves nothing of itself behind.
 *
 * <p>What only some errands hold, a board reserved, a music stream, a partner engaged, a chest left open,
 * is checked by the scenario of the errand that holds it, see {@link ErrandScenarios.Scenario#untidy}.
 */
final class Residue {
    private Residue() {
    }

    /** Fails the case if the pet is still holding anything of the errand it has just left, which was at the step {@code ran} last showed. */
    static void none(GameTestHelper helper, AbstractPet pet, ErrandScenarios.Scenario scenario, Running ran) {
        List<String> left = new ArrayList<>();
        if (pet.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
            left.add("a place to walk to");
        }
        if (pet.getBrain().hasMemoryValue(MemoryModuleType.LOOK_TARGET)) {
            left.add("something to look at");
        }
        if (pet.getBrain().hasMemoryValue(MemoryModuleType.PATH)) {
            left.add("a path");
        }
        if (pet.getActivity() != PetActivity.NONE) {
            left.add("the pose " + pet.getActivity());
        }
        if (!pet.getPerformance().isEmpty()) {
            left.add("the scene " + pet.getPerformance());
        }
        if (pet.isUsingItem()) {
            left.add("an item in use");
        }
        if (!pet.getPendingGift().isEmpty()) {
            left.add("a present in its paws");
        }
        scenario.untidy().apply(ran).ifPresent(left::add);
        helper.assertTrue(left.isEmpty(), "the pet left its errand still holding " + String.join(", ", left));
    }
}
