package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.carries;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;

/**
 * For each native errand, a setup in which it is the best thing its pet can do and a way to tell whether
 * it was carried out cleanly. The contract cases in {@link ErrandContractGameTests} are made from these, so
 * an errand that goes native adds its setup here and gets its three cases.
 *
 * <p>Each setup is a plain static method of a helper and returns a plain record, so a branch that
 * registers its cases another way rewrites that registration and leaves these as they are.
 */
final class ErrandScenarios {
    private static final int STAND = 2;

    /**
     * A pet set up to be on one errand.
     *
     * @param pet the pet, which sets off on its own
     * @param steps how many steps the errand's routine has, which the cases cut it between
     * @param untidy what the errand may not leave behind that is its own to the world and not to the pet, such as an item
     *               half taken: the problem, or nothing if all is as it should be
     */
    record Scenario(AbstractPet pet, int steps, Supplier<Optional<String>> untidy) {
    }

    private static final Map<ResourceLocation, Function<GameTestHelper, Scenario>> BY_ERRAND = Map.of(
        ErrandIds.PICK_UP_ITEM, ErrandScenarios::pickUpItem);

    private ErrandScenarios() {
    }

    /** @return the errands that have a scenario, which are the native ones */
    static Set<ResourceLocation> ids() {
        return BY_ERRAND.keySet();
    }

    /** @return the pet and what the errand touches, set up in the case's level */
    static Scenario setUp(ResourceLocation errand, GameTestHelper helper) {
        Function<GameTestHelper, Scenario> setUp = BY_ERRAND.get(errand);
        if (setUp == null) {
            throw new IllegalArgumentException(errand + " has no scenario");
        }
        return setUp.apply(helper);
    }

    /**
     * Wheat on the floor some blocks off, to a pet with nothing else to do: far enough to be a walk of many
     * ticks, near enough for the pet to notice it. The wheat is whole and where it was until the pet has it.
     */
    private static Scenario pickUpItem(GameTestHelper helper) {
        AbstractPet pet = ownedPet(helper, new BlockPos(4, STAND, 8));
        ItemEntity wheat = helper.spawnItem(Items.WHEAT, new BlockPos(10, STAND, 8));
        int count = wheat.getItem().getCount();
        return new Scenario(pet, 2, () -> {
            if (carries(pet, Items.WHEAT)) {
                return Optional.of("the pet has the wheat, though the errand was not done");
            }
            if (!wheat.isAlive() || wheat.getItem().getCount() != count) {
                return Optional.of("the wheat on the floor was touched, though the pet does not have it");
            }
            return Optional.empty();
        });
    }
}
