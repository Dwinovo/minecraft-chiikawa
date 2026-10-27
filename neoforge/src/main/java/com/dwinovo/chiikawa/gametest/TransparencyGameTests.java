package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.assertSaid;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.lastSaid;
import static com.dwinovo.chiikawa.gametest.GameTestKit.pet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.quietYard;
import static com.dwinovo.chiikawa.gametest.GameTestKit.said;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.errand.decide.WhyNot;
import com.dwinovo.chiikawa.entity.brain.errand.run.PetIcon;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.network.PetPayloads.PetOutlookPayload;
import com.dwinovo.chiikawa.settings.WorldSettings;
import com.dwinovo.chiikawa.settings.WorldSettingsStore;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a pet lets its owner see of what is on its mind: the picture over its head, a word in passing about what it is short
 * of, and what its screen says. All three are made from the one account of why the pet is not at work that its mind keeps.
 *
 * <p>What a pet said is read back from its memory of it, and what its screen is told from the payload the server builds for
 * it, because the stand-in players these cases make cannot be sent a mod's own packets. A pet that talks is in a batch of its
 * own: pets within earshot of each other take turns.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TransparencyGameTests {
    private static final String MISSING_BATCH = "chiikawa_transparency_missing";
    private static final String SILENT_BATCH = "chiikawa_transparency_silent";
    private static final String ICON_BATCH = "chiikawa_transparency_icon";
    private static final String SCREEN_BATCH = "chiikawa_transparency_screen";
    private static final int STAND = 2;
    /** How long a pet has to be short of something before it says so, as the remark has it. */
    private static final int LACKED_TICKS = 600;
    /** Long enough after that for a pet by its owner to have said it. */
    private static final int SAY_TICKS = 100;
    /** Longer than the pet's own quiet after speaking, so that a second remark would be the remark's doing and not the voice's. */
    private static final int QUIET_TICKS = 900;
    private static final int ARRIVE_TICKS = 1200;

    /** The world's chatter before a batch turned it down. */
    private static WorldSettings before;

    @BeforeBatch(batch = MISSING_BATCH)
    public static void settleForTheRemark(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = SILENT_BATCH)
    public static void turnChatterOff(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
        WorldSettingsStore store = WorldSettingsStore.of(level);
        before = store.get();
        store.set(before.withChatter(0.0F));
    }

    @AfterBatch(batch = SILENT_BATCH)
    public static void turnChatterBackOn(ServerLevel level) {
        WorldSettingsStore.of(level).set(before);
    }

    @BeforeBatch(batch = ICON_BATCH)
    public static void settleForTheIcons(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = SCREEN_BATCH)
    public static void settleForTheScreen(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /**
     * A farmer with a hoe and not a seed to its name, its owner by: after half a minute of it Rakko says so, in its own
     * words and about the seeds, once, and wears the picture of a pet that is short of something the while. The remark is
     * rested for a good deal longer than the pet's own quiet, which is what the wait at the end shows.
     */
    @GameTest(template = "floor16", batch = MISSING_BATCH, timeoutTicks = LACKED_TICKS + SAY_TICKS + QUIET_TICKS + 100)
    public static void a_farmer_out_of_seeds_says_so_once_and_shows_it(GameTestHelper helper) {
        quietYard(helper, NOON);
        ServerPlayer owner = player(helper);
        AbstractPet pet = farmerOf(helper, owner, new BlockPos(8, STAND, 8));
        keepNear(helper, owner, pet);

        helper.runAfterDelay(LACKED_TICKS - SAY_TICKS, () -> {
            helper.assertFalse(said(pet).isPresent(), "the pet spoke up before it had been short of anything for long");
            helper.assertTrue(pet.getIcon() == PetIcon.MISSING, "the pet short of seeds wears " + pet.getIcon());
        });
        helper.runAfterDelay(LACKED_TICKS + SAY_TICKS, () -> {
            assertSaid(helper, pet, VoiceMoment.MISSING);
            WhyNot why = pet.getMind().whyNot().orElseThrow(() -> new AssertionError("the pet found nothing wrong"));
            helper.assertTrue(why.lack().filter(lack -> lack.translationKey().equals("tag.item.chiikawa.entity_plant_crops")).isPresent(),
                "the pet is short of " + why.lack());
            PetSpeech.Said first = lastSaid(pet).orElseThrow();
            helper.runAfterDelay(QUIET_TICKS, () -> {
                helper.assertTrue(lastSaid(pet).orElseThrow().equals(first), "the pet said it again: " + lastSaid(pet));
                helper.succeed();
            });
        });
    }

    /** The world's chatter turned right down silences a remark as it does any other line, and the picture over the head stays. */
    @GameTest(template = "floor16", batch = SILENT_BATCH, timeoutTicks = LACKED_TICKS + SAY_TICKS + 100)
    public static void a_farmer_out_of_seeds_keeps_quiet_in_a_world_with_no_chatter(GameTestHelper helper) {
        quietYard(helper, NOON);
        ServerPlayer owner = player(helper);
        AbstractPet pet = farmerOf(helper, owner, new BlockPos(8, STAND, 8));
        keepNear(helper, owner, pet);

        helper.runAfterDelay(LACKED_TICKS + SAY_TICKS, () -> {
            helper.assertFalse(said(pet).isPresent(), "the pet spoke in a world with no chatter: " + said(pet));
            helper.assertTrue(pet.getIcon() == PetIcon.MISSING, "the pet short of seeds wears " + pet.getIcon());
            helper.succeed();
        });
    }

    /** A pet with a present for its owner wears a gift over its head while it carries it, and not once it has given it. */
    @GameTest(template = "floor16", batch = ICON_BATCH, timeoutTicks = ARRIVE_TICKS)
    public static void a_pet_with_a_present_shows_the_gift_icon(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos across = helper.absolutePos(new BlockPos(12, STAND, 12));
        owner.teleportTo(across.getX() + 0.5, across.getY(), across.getZ() + 0.5);
        AbstractPet pet = pet(helper, InitEntity.USAGI_PET.get(), new BlockPos(3, STAND, 3), false);
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FREE);
        pet.setPendingGift(new ItemStack(Items.CAKE));
        boolean[] shown = {false};

        helper.onEachTick(() -> shown[0] |= pet.getIcon() == PetIcon.GIFT && !pet.getPendingGift().isEmpty());
        helper.succeedWhen(() -> {
            helper.assertTrue(shown[0], "the pet carried its present without showing it");
            helper.assertTrue(owner.getInventory().contains(new ItemStack(Items.CAKE)), "the owner never got the cake");
            helper.assertTrue(pet.getIcon() == PetIcon.NONE, "the pet still wears " + pet.getIcon() + " with its paws empty");
        });
    }

    /** A pet with seeds but nothing to sow or reap is looking for work, by day. */
    @GameTest(template = "floor16", batch = ICON_BATCH, timeoutTicks = 100)
    public static void a_farmer_with_nothing_to_do_shows_it_is_looking_for_work(GameTestHelper helper) {
        AbstractPet pet = holding(pet(helper, InitEntity.RAKKO_PET.get(), new BlockPos(8, STAND, 8), false), Items.WOODEN_HOE);
        pet.getBackpack().addItem(new ItemStack(Items.WHEAT_SEEDS, 8));

        helper.succeedWhen(() -> helper.assertTrue(pet.getIcon() == PetIcon.SEEKING_WORK, "the pet wears " + pet.getIcon()));
    }

    /** A pet that runs from what hurt it wears the picture of a frightened one. */
    @GameTest(template = "floor8", batch = ICON_BATCH, timeoutTicks = 200)
    public static void a_pet_running_from_a_foe_shows_it_is_scared(GameTestHelper helper) {
        AbstractPet pet = pet(helper, InitEntity.KURIMANJU_PET.get(), new BlockPos(3, STAND, 3), false);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(6, STAND, 6));
        zombie.setNoAi(true);

        pet.hurt(zombie.damageSources().mobAttack(zombie), 1.0F);

        helper.succeedWhen(() -> helper.assertTrue(pet.getIcon() == PetIcon.SCARED, "the pet wears " + pet.getIcon()));
    }

    /**
     * What the screen is told is made from what the pet's mind kept: a farmer with no seeds is short of seeds, and one with
     * seeds and nothing to do is not short of anything. The screen's menu takes it as the client's handler gives it, and the
     * server's menu, which has no one to send it to here, takes its turn without a fuss.
     */
    @GameTest(template = "floor16", batch = SCREEN_BATCH, timeoutTicks = 200)
    public static void the_screen_says_why_a_farmer_is_idle(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        AbstractPet pet = farmerOf(helper, owner, new BlockPos(8, STAND, 8));
        PetBackpackMenu clientsMenu = new PetBackpackMenu(1, owner.getInventory(), pet.getId());

        helper.assertFalse(clientsMenu.outlook().isPresent(), "the menu knew something before it was told");
        helper.runAfterDelay(40, () -> {
            new PetBackpackMenu(2, owner.getInventory(), pet).broadcastChanges();
            clientsMenu.receive(outlookOf(helper, pet));
            PetOutlookPayload shortOfSeeds = clientsMenu.outlook().orElseThrow(() -> new AssertionError("the menu was told nothing"));
            // Strolling is all there is to do, and a pet that is strolling is not told it will stroll next.
            helper.assertTrue(shortOfSeeds.next().isEmpty() || !shortOfSeeds.next().equals(pet.getIntent()), "the pet said " + shortOfSeeds);
            helper.assertTrue(shortOfSeeds.whyNotKey().equals(Optional.of("intent.chiikawa.fail.no_farmland")), "the pet said " + shortOfSeeds);
            helper.assertTrue(shortOfSeeds.lackKey().equals(Optional.of("tag.item.chiikawa.entity_plant_crops")), "the pet said " + shortOfSeeds);

            pet.getBackpack().addItem(new ItemStack(Items.WHEAT_SEEDS, 8));
            helper.runAfterDelay(40, () -> {
                clientsMenu.receive(outlookOf(helper, pet));
                PetOutlookPayload withSeeds = clientsMenu.outlook().orElseThrow();
                helper.assertTrue(withSeeds.whyNotKey().equals(Optional.of("intent.chiikawa.fail.no_crop")), "the pet said " + withSeeds);
                helper.assertTrue(withSeeds.lackKey().isEmpty(), "the pet with seeds is short of " + withSeeds.lackKey());
                helper.succeed();
            });
        });
    }

    /** What the screen of this pet's owner would be told now. */
    private static PetOutlookPayload outlookOf(GameTestHelper helper, AbstractPet pet) {
        return PetOutlookPayload.of(pet.getId(), pet.getMind().look(helper.getLevel()), pet.getMind().whyNot());
    }

    /** A farmer that belongs to this player, with a hoe and no seeds, at ease. Rakko has a word for what it is short of and no chatter besides. */
    private static AbstractPet farmerOf(GameTestHelper helper, ServerPlayer owner, BlockPos rel) {
        AbstractPet pet = holding(pet(helper, InitEntity.RAKKO_PET.get(), rel, false), Items.WOODEN_HOE);
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FREE);
        return pet;
    }

    /** Keeps the player within a few blocks of the pet, wherever the pet wanders, so there is somebody to hear it. */
    private static void keepNear(GameTestHelper helper, ServerPlayer owner, AbstractPet pet) {
        helper.onEachTick(() -> {
            if (owner.distanceTo(pet) > 3.0F) {
                owner.teleportTo(pet.getX() + 2.0, pet.getY(), pet.getZ());
            }
        });
    }
}
