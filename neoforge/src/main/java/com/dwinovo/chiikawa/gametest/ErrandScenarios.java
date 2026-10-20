package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.carries;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.block.DeskBooking;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import com.dwinovo.chiikawa.camera.PhotoResults;
import com.dwinovo.chiikawa.camera.PhotoScout;
import com.dwinovo.chiikawa.data.QualificationData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.photo.PhotoSystem;
import com.dwinovo.chiikawa.qualification.ExamStage;
import com.dwinovo.chiikawa.qualification.Licence;
import com.dwinovo.chiikawa.shop.Wallet;
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
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

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
    private static final int COOKIES = 3;
    /** Enough for anything on the list, so what the pet does at the counter is about wanting rather than affording. */
    private static final int WAGES = 8;
    /** The step of a photo at which the shutter goes: after the walk and the aim. */
    private static final int SHUTTER_STEP = 2;

    /**
     * A pet set up to be on one errand.
     *
     * @param pet the pet, which sets off on its own
     * @param steps how many steps the errand's routine has, which the cases cut it between
     * @param reported the report the errand's steps have made, by the name its settings give it, by the time the errand was
     *                 last seen at this step: a rest the errand begins when something happens in the middle of it, and
     *                 which however it ends it still keeps
     * @param untidy what the errand may not leave behind that is its own to the world and not to the pet, such as an item
     *               half taken: the problem, or nothing if all is as it should be; told the errand as it was last seen
     *               running, since what is left behind may be fair where the errand had got to
     */
    record Scenario(AbstractPet pet, int steps, Function<Running, Optional<String>> reported,
                    Function<Running, Optional<String>> untidy) {
        /** A scenario of an errand whose steps report nothing. */
        Scenario(AbstractPet pet, int steps, Function<Running, Optional<String>> untidy) {
            this(pet, steps, ran -> Optional.empty(), untidy);
        }
    }

    private static final Map<ResourceLocation, Function<GameTestHelper, Scenario>> BY_ERRAND = Map.of(
        ErrandIds.PICK_UP_ITEM, ErrandScenarios::pickUpItem,
        ErrandIds.TAKE_PHOTO, ErrandScenarios::takePhoto,
        ErrandIds.GIFT_OWNER, ErrandScenarios::giftOwner,
        ErrandIds.SHOP, ErrandScenarios::shop,
        ErrandIds.TAKE_EXAM, ErrandScenarios::takeExam,
        ErrandIds.CHECK_RESULTS, ErrandScenarios::checkResults);

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
     * A pet with three cookies in its bag and its owner across the yard, which is a present in mind and a walk of many
     * ticks to the owner. Whatever is cut short, the cookies are all back in the bag and the owner has none, unless the
     * present was handed over.
     */
    private static Scenario giftOwner(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos across = helper.absolutePos(new BlockPos(10, STAND, 10));
        owner.teleportTo(across.getX() + 0.5, across.getY(), across.getZ() + 0.5);
        AbstractPet pet = GameTestKit.wild(helper, InitEntity.USAGI_PET.get(), new BlockPos(3, STAND, 3));
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FREE);
        pet.getBackpack().setItem(AbstractPet.BAG_SLOT + 1, new ItemStack(Items.COOKIE, COOKIES));
        return new Scenario(pet, 3, ran -> {
            if (GameTestKit.count(pet, Items.COOKIE) != COOKIES) {
                return Optional.of("the bag has " + GameTestKit.count(pet, Items.COOKIE) + " cookies, not the " + COOKIES + " it had");
            }
            if (owner.getInventory().countItem(Items.COOKIE) > 0) {
                return Optional.of("the owner was given a cookie, though the present was not handed over");
            }
            return Optional.empty();
        });
    }

    /**
     * A pet with a purse of emeralds and nothing to do but spend them, and a shop across the yard: a walk of many ticks to
     * the counter. A purchase is money paid for goods in the bag, both or neither, so what is left is never one without the
     * other.
     */
    private static Scenario shop(GameTestHelper helper) {
        helper.setBlock(new BlockPos(13, STAND, 4), InitBlocks.SHOP.get());
        AbstractPet pet = GameTestKit.worker(helper, new BlockPos(3, STAND, 4));
        pet.getBackpack().addItem(new ItemStack(Items.EMERALD, WAGES));
        return new Scenario(pet, 2, ran -> {
            boolean paid = Wallet.count(pet.getBackpack()) < WAGES;
            boolean goods = false;
            for (int slot = 0; slot < pet.getBackpack().getContainerSize(); slot++) {
                ItemStack stack = pet.getBackpack().getItem(slot);
                goods |= !stack.isEmpty() && !Wallet.isMoney(stack);
            }
            if (paid && !goods) {
                return Optional.of("money was paid, though the pet came away with nothing for it");
            }
            if (goods && !paid) {
                return Optional.of("the pet has goods it did not pay for");
            }
            return Optional.empty();
        });
    }

    /**
     * A pet signed up for an exam at a desk across the yard, on the day it is held. The paper is handed in whole or not
     * at all, the licence and the desk together, and the desk stays booked for the pet whatever becomes of its errand:
     * a pet called away before it hands in can come back to it while the exam is on.
     */
    private static Scenario takeExam(GameTestHelper helper) {
        ExamDeskBlockEntity desk = ExamGameTests.placeDesk(helper);
        AbstractPet pet = ExamGameTests.signedUp(helper, desk, new BlockPos(4, STAND, 8));
        return new Scenario(pet, 5, ran -> {
            boolean paperIn = pet.licences().get(QualificationData.WEEDING).paper().isPresent();
            boolean resultOnDesk = desk.booking().flatMap(DeskBooking::passed).isPresent();
            if (paperIn != resultOnDesk) {
                return Optional.of(paperIn ? "the pet's licence has a paper in, though the desk has none of it"
                    : "the desk has the paper, though the pet's licence has not handed it in");
            }
            if (desk.booking().filter(booking -> booking.pet().equals(pet.getUUID())).isEmpty()) {
                return Optional.of("the desk is no longer booked for the pet that was sitting at it");
            }
            return Optional.empty();
        });
    }

    /**
     * A pet that sat an exam yesterday and passed, on the morning the results are up, a few blocks from the desk. The results
     * are heard whole or not at all: the licence has gone up a grade and the paper is gone, or neither has happened.
     */
    private static Scenario checkResults(GameTestHelper helper) {
        settleWorld(helper.getLevel(), Difficulty.NORMAL, (ExamGameTests.EXAM_DAY_NUMBER + 1) * Level.TICKS_PER_DAY + ExamGameTests.MORNING);
        ExamDeskBlockEntity desk = ExamGameTests.placeDesk(helper);
        AbstractPet pet = GameTestKit.worker(helper, new BlockPos(4, STAND, 8));
        pet.licences().set(QualificationData.WEEDING, new Licence(0, 0, 0, false,
            new ExamStage.Sat(ExamGameTests.at(helper, ExamGameTests.DESK), ExamGameTests.EXAM_DAY_NUMBER, true)));
        desk.book(new DeskBooking(pet.getUUID(), "Shisa", QualificationData.WEEDING, 5, ExamGameTests.EXAM_DAY_NUMBER,
            Optional.of(true)));
        return new Scenario(pet, 3, ran -> {
            Licence licence = pet.licences().get(QualificationData.WEEDING);
            if (licence.paper().isPresent() == (licence.held() == 1)) {
                return Optional.of(licence.held() == 1 ? "the pet has the grade, and the paper it was granted for"
                    : "the pet has no grade, and the paper it should have been granted for is gone");
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
        return new Scenario(pet, 4, ran -> ran.stepIndex() > SHUTTER_STEP ? Optional.of(PhotoResults.TAKEN) : Optional.empty(), ran -> {
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
