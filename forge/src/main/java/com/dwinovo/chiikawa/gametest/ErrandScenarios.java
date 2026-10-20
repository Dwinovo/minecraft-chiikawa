package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.carries;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;

import com.dwinovo.chiikawa.camera.PhotoScout;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.photo.PhotoSystem;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
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
    /** How far off the owner is, east, along the row of yards. */
    private static final int OWNER_AWAY = 40;
    private static final int FILM = 3;
    /** The step of a photo at which the shutter goes: after the walk and the aim. */
    private static final int SHUTTER_STEP = 2;

    /**
     * A pet set up to be on one errand.
     *
     * @param pet the pet, which sets off on its own
     * @param steps how many steps the errand's routine has, which the cases cut it between
     * @param untidy what the errand may not leave behind that is its own to the world and not to the pet, such as an item
     *               half taken: the problem, or nothing if all is as it should be; told the errand as it was last seen
     *               running, since what is left behind may be fair where the errand had got to
     */
    record Scenario(AbstractPet pet, int steps, Function<Running, Optional<String>> untidy) {
    }

    private static final Map<ResourceLocation, Function<GameTestHelper, Scenario>> BY_ERRAND = Map.of(
        ErrandIds.PICK_UP_ITEM, ErrandScenarios::pickUpItem,
        ErrandIds.TAKE_PHOTO, ErrandScenarios::takePhoto);

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
        return new Scenario(pet, 2, ran -> {
            if (carries(pet, Items.WHEAT)) {
                return Optional.of("the pet has the wheat, though the errand was not done");
            }
            if (!wheat.isAlive() || wheat.getItem().getCount() != count) {
                return Optional.of("the wheat on the floor was touched, though the pet does not have it");
            }
            return Optional.empty();
        });
    }

    /**
     * Hachiware with a camera and film, its owner out of sight but near enough for their game to draw the picture, and
     * a friend to photograph who has nothing to do. The view is left out by having been to every biome there is, so the
     * friend is the one moment, and the errand has all its four steps: the walk, the aim, the shutter and the hold.
     * Nothing is asked for and nobody turns to the camera until the shutter has gone.
     */
    private static Scenario takePhoto(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos far = helper.absolutePos(new BlockPos(8, STAND, 8)).offset(OWNER_AWAY, 0, 0);
        owner.teleportTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
        AbstractPet pet = GameTestKit.pet(helper, InitEntity.HACHIWARE_PET.get(), new BlockPos(4, STAND, 4), false);
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FREE);
        pet.getBackpack().addItem(new ItemStack(InitItems.CAMERA.get()));
        pet.getBackpack().addItem(new ItemStack(InitItems.FILM.get(), FILM));
        pet.getBrain().setMemory(InitMemory.PHOTOGRAPHED_BIOMES.get(), PhotoScout.biomeOf(pet).map(List::of).orElseThrow());
        AbstractPet friend = GameTestKit.pet(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(11, STAND, 4), false);
        friend.setNoAi(true);
        PhotoSystem photos = PhotoSystem.of(helper.getLevel().getServer());
        Set<UUID> asked = photos.waiting();
        return new Scenario(pet, 4, ran -> {
            boolean shutterPressed = ran.stepIndex() >= SHUTTER_STEP;
            if (!shutterPressed && !asked.containsAll(photos.waiting())) {
                return Optional.of("a photo was asked for before the shutter");
            }
            if (!shutterPressed && friend.getBrain().hasMemoryValue(MemoryModuleType.LOOK_TARGET)) {
                return Optional.of("the friend was turned to the camera before the shutter");
            }
            return Optional.empty();
        });
    }
}
