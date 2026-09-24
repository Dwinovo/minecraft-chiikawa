package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.LanguageData;
import com.dwinovo.chiikawa.data.ManualData;
import com.dwinovo.chiikawa.data.ModAdvancementProvider;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTabs;
import com.dwinovo.chiikawa.item.BagItem;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.GameTestDontPrefix;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * The things a pet can be given: what it carries, what happens when that is taken away
 * again, and the one thing it eats rather than carries.
 */
@GameTestHolder(namespace = Constants.MOD_ID)
@GameTestDontPrefix
public final class SupplyGameTests {
    private static final String BATCH = "chiikawa_supply";
    private static final int STAND = 2;
    /** Where the bag's own slots start; the pockets before it are the pet's own. */
    private static final int FIRST_BAG_SLOT = AbstractPet.BACKPACK_SIZE;
    /** A spell of eagerness long enough that no case can tick through it by accident. */
    private static final int DISH = 6000;
    /** And one short enough that a case can wait it out. */
    private static final int SHORT_DISH = 5;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /**
     * Everything the mod adds can be found in its creative tab. The tab is a list kept by
     * hand, and a new item left off it is an item nobody in creative can get.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void every_item_is_in_the_creative_tab(GameTestHelper helper) {
        Set<Item> listed = new HashSet<>();
        InitTabs.addMainItems(item -> listed.add(item.asItem()));
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id.getNamespace().equals(Constants.MOD_ID)) {
                helper.assertTrue(listed.contains(item), id + " is not in the creative tab");
            }
        }
        helper.succeed();
    }

    /**
     * Every word the handbook's own pages show is written in both languages: a title, a
     * caption or a bubble left out shows its raw key on the page.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void every_handbook_word_is_written_in_both_languages(GameTestHelper helper) {
        for (String locale : List.of("zh_cn", "en_us")) {
            Set<String> written = new HashSet<>();
            LanguageData.addTranslations(locale, (key, value) -> written.add(key));
            ManualData.all().forEach((id, page) -> {
                helper.assertTrue(written.contains(page.title()), locale + " has no " + page.title());
                page.panels().forEach(panel -> {
                    helper.assertTrue(written.contains(panel.caption()), locale + " has no " + panel.caption());
                    panel.actors().forEach(actor -> actor.motion().say().ifPresent(say ->
                        helper.assertTrue(written.contains(say), locale + " has no " + say)));
                });
            });
        }
        helper.succeed();
    }

    /**
     * A newcomer is handed the handbook, by the hidden advancement that marks their
     * arrival. Awarded by hand here — a case's player does not tick its way in as a joining
     * one does — to see what the reward gives.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void a_newcomer_is_handed_the_handbook(GameTestHelper helper) {
        ServerPlayer newcomer = player(helper);
        AdvancementHolder arrival = helper.getLevel().getServer().getAdvancements().get(ModAdvancementProvider.HANDBOOK);
        helper.assertTrue(arrival != null, "there is no advancement to hand the handbook out");

        newcomer.getAdvancements().award(arrival, "arrived");

        helper.assertTrue(newcomer.getInventory().contains(new ItemStack(InitItems.HANDBOOK.get())),
            "a newcomer arrived and was handed nothing");
        helper.succeed();
    }

    /**
     * Any bag on it is ten more slots, and the pet knows it is wearing one: the rucksack
     * and each of the pouches alike. Which one is the owner's choice, not the pet's.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void every_bag_adds_room(GameTestHelper helper) {
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));

        helper.assertFalse(pet.isWearingBag(), "a pet turned up already wearing a bag");
        for (Item bag : bags()) {
            pet.setItemSlot(EquipmentSlot.CHEST, new ItemStack(bag));
            helper.assertTrue(pet.isWearingBag(), "a " + bag + " went on and the pet did not notice");
        }
        helper.assertTrue(pet.getBackpack().getContainerSize() == AbstractPet.FULL_BACKPACK_SIZE,
            "the bag's slots are not there to be used");
        helper.succeed();
    }

    /**
     * Swapping one bag for another in the menu — a bag on the cursor, clicked onto the one
     * the pet wears — keeps what was in the old one: the pet never stood without a bag, so
     * the ten slots never shut and nothing lands on the floor.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void swapping_bags_keeps_what_is_in_them(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        pet.setItemSlot(EquipmentSlot.CHEST, new ItemStack(InitItems.BACKPACK.get()));
        pet.getBackpack().setItem(FIRST_BAG_SLOT, new ItemStack(Items.CAKE));
        PetBackpackMenu menu = new PetBackpackMenu(1, owner.getInventory(), pet);

        menu.setCarried(new ItemStack(InitItems.STAR_POUCH.get()));
        menu.clicked(AbstractPet.BAG_SLOT, 0, ClickType.PICKUP, owner);

        helper.assertTrue(pet.getItemBySlot(EquipmentSlot.CHEST).is(InitItems.STAR_POUCH.get()),
            "the star pouch did not go on");
        helper.assertTrue(menu.getCarried().is(InitItems.BACKPACK.get()), "the rucksack did not come off onto the cursor");
        helper.assertTrue(pet.getBackpack().getItem(FIRST_BAG_SLOT).is(Items.CAKE), "the cake did not stay with the pet");
        helper.succeed();
    }

    /** The bag slot takes a bag, any bag, and nothing that is not one. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void the_bag_slot_takes_bags_and_nothing_else(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        Slot bagSlot = new PetBackpackMenu(1, owner.getInventory(), pet).slots.get(AbstractPet.BAG_SLOT);

        for (Item bag : bags()) {
            helper.assertTrue(bagSlot.mayPlace(new ItemStack(bag)), "the bag slot refused a " + bag);
        }
        helper.assertFalse(bagSlot.mayPlace(new ItemStack(Items.CAKE)), "the bag slot took a cake");
        helper.succeed();
    }

    /**
     * Take the bag off and what was in it lands on the floor. Anything else — vanishing,
     * or being stuffed into pockets that may already be full — loses a player's things.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void taking_the_bag_off_empties_it_onto_the_floor(GameTestHelper helper) {
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.setItemSlot(EquipmentSlot.CHEST, new ItemStack(InitItems.BACKPACK.get()));
        pet.getBackpack().setItem(FIRST_BAG_SLOT, new ItemStack(Items.CAKE));

        pet.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        pet.dropBagContents();

        helper.assertTrue(pet.getBackpack().getItem(FIRST_BAG_SLOT).isEmpty(),
            "the cake stayed in a bag nobody is wearing");
        helper.assertItemEntityPresent(Items.CAKE, new BlockPos(3, STAND, 3), 4.0);
        helper.succeed();
    }

    /** The worn bag rides in the pet's own pockets, so it is saved and revived with it. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void the_worn_bag_is_part_of_what_the_pet_carries(GameTestHelper helper) {
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.getBackpack().setItem(AbstractPet.BAG_SLOT, new ItemStack(InitItems.BACKPACK.get()));

        helper.assertTrue(pet.getItemBySlot(EquipmentSlot.CHEST).is(InitItems.BACKPACK.get()),
            "a bag put in the bag slot is not what the pet is wearing");
        helper.succeed();
    }
    /**
     * The menu the client builds, which has no pet in it, must lay out exactly as many
     * slots as the one the server builds with the pet in hand. One short and the first
     * packet of contents runs off the end of the list, which the player does not see as a
     * missing slot — they see themselves thrown out of the world.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void both_sides_of_the_backpack_menu_lay_out_the_same_slots(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);

        int withPet = new PetBackpackMenu(1, owner.getInventory(), pet).slots.size();
        int withoutPet = new PetBackpackMenu(2, owner.getInventory()).slots.size();

        helper.assertTrue(withPet == withoutPet,
            "the server lays out " + withPet + " slots and the client " + withoutPet);
        helper.succeed();
    }

    /**
     * The pet screen has pages and only one of them is the backpack: on the others every
     * slot is shut, the player's own included, so nothing can be dropped into a slot
     * nobody can see.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void on_a_page_without_the_backpack_every_slot_is_shut(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        PetBackpackMenu menu = new PetBackpackMenu(1, owner.getInventory(), pet);

        menu.showSlots(false);
        helper.assertTrue(menu.slots.stream().noneMatch(net.minecraft.world.inventory.Slot::isActive),
            "a slot stayed open on a page that does not show slots");

        menu.showSlots(true);
        helper.assertTrue(menu.slots.get(AbstractPet.MAINHAND_SLOT).isActive(), "the hand did not come back");
        helper.assertTrue(menu.slots.get(menu.slots.size() - 1).isActive(), "the player's own hotbar did not come back");
        helper.succeed();
    }

    /** A dish handed to your own pet is eaten, and the pet gets on with things quicker. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void a_dish_puts_a_pet_in_the_mood(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        // A mock player turns up in creative, where nothing in a hand is ever spent.
        owner.setGameMode(GameType.SURVIVAL);
        double plodding = pet.getAttributeValue(Attributes.MOVEMENT_SPEED);

        owner.setShiftKeyDown(false);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(InitItems.SIMPLE_DISH.get(), 2));
        pet.mobInteract(owner, InteractionHand.MAIN_HAND);

        helper.assertTrue(pet.isEager(), "a pet ate a whole dish and thought nothing of it");
        helper.assertTrue(pet.getAttributeValue(Attributes.MOVEMENT_SPEED) > plodding,
            "an eager pet is no quicker about anything");
        helper.assertTrue(owner.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
            "the dish was not eaten");
        helper.succeed();
    }

    /**
     * Two dishes at once would be a way to keep a pet permanently sprinting. A second one
     * tops the mood up instead, and the pet stays exactly as quick as one dish makes it.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void two_dishes_do_not_make_a_pet_twice_as_quick(GameTestHelper helper) {
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));

        pet.feedDish(DISH);
        double onDish = pet.getAttributeValue(Attributes.MOVEMENT_SPEED);
        pet.feedDish(DISH);

        helper.assertTrue(pet.getAttributeValue(Attributes.MOVEMENT_SPEED) == onDish,
            "a second dish stacked on top of the first");
        helper.succeed();
    }

    /** The mood is a spell, not a gift: it runs out, and the speed goes back with it. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void the_mood_wears_off_and_takes_the_hurry_with_it(GameTestHelper helper) {
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        double plodding = pet.getAttributeValue(Attributes.MOVEMENT_SPEED);
        pet.feedDish(SHORT_DISH);

        helper.succeedWhen(() -> {
            helper.assertFalse(pet.isEager(), "the mood never wore off");
            helper.assertTrue(pet.getAttributeValue(Attributes.MOVEMENT_SPEED) == plodding,
                "the mood wore off and left the pet hurrying anyway");
        });
    }

    /** A dish from somebody else is a dish somebody else is holding. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void a_stranger_cannot_feed_your_pet(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ServerPlayer stranger = player(helper);
        stranger.setGameMode(GameType.SURVIVAL);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);

        stranger.setShiftKeyDown(false);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(InitItems.SIMPLE_DISH.get()));
        pet.mobInteract(stranger, InteractionHand.MAIN_HAND);

        helper.assertFalse(pet.isEager(), "somebody else's dish put the pet in the mood");
        helper.assertTrue(stranger.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
            "the dish was eaten by a pet that is not theirs");
        helper.succeed();
    }

    /**
     * The ten slots a bag brings are shut until the bag is on. They are drawn as a hint
     * rather than as empty wells, and a shut slot is what makes that true rather than
     * decorative.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void the_bags_slots_stay_shut_until_a_bag_is_worn(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        PetBackpackMenu menu = new PetBackpackMenu(1, owner.getInventory(), pet);

        helper.assertFalse(menu.slots.get(FIRST_BAG_SLOT).isActive(),
            "a pet with no bag had the bag's slots open");

        pet.setItemSlot(EquipmentSlot.CHEST, new ItemStack(InitItems.BACKPACK.get()));

        helper.assertTrue(menu.slots.get(FIRST_BAG_SLOT).isActive(),
            "the bag went on and its slots stayed shut");
        helper.succeed();
    }

    /** Every bag there is: the props that are bags. */
    private static List<Item> bags() {
        return InitItems.PROPS.stream().map(Supplier::get).filter(BagItem.class::isInstance)
            .map(Item.class::cast).toList();
    }
}
