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
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.network.PetPayloads.PetOutlookPayload;
import com.dwinovo.chiikawa.settings.WorldSettings;
import com.dwinovo.chiikawa.settings.WorldSettingsStore;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What a pet lets its owner see of what is on its mind: a word in passing about what it is short of, and what its screen
 * says. Both are made from the one account of why the pet is not at work that its mind keeps.
 *
 * <p>What a pet said is read back from its memory of it, and what its screen is told from the payload the server builds for
 * it, because the stand-in players these cases make cannot be sent a mod's own packets. A pet that talks is in a batch of its
 * own: pets within earshot of each other take turns.
 */
@GameTestHolder(Constants.MOD_ID)
public final class TransparencyGameTests {
    private static final String MISSING_BATCH = "chiikawa_transparency_missing";
    private static final String SILENT_BATCH = "chiikawa_transparency_silent";
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

    @BeforeBatch(batch = SCREEN_BATCH)
    public static void settleForTheScreen(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /**
     * A farmer with a hoe and not a seed to its name, its owner by: after half a minute of it Rakko says so, in its own
     * words and about the seeds, once. The remark is rested for a good deal longer than the pet's own quiet, which is what
     * the wait at the end shows.
     */
    @GameTest(template = "floor16", batch = MISSING_BATCH, timeoutTicks = LACKED_TICKS + SAY_TICKS + QUIET_TICKS + 100)
    public static void a_farmer_out_of_seeds_says_so_once(GameTestHelper helper) {
        quietYard(helper, NOON);
        ServerPlayer owner = player(helper);
        AbstractPet pet = farmerOf(helper, owner, new BlockPos(8, STAND, 8));
        keepNear(helper, owner, pet);

        helper.runAfterDelay(LACKED_TICKS - SAY_TICKS, () -> {
            helper.assertFalse(said(pet).isPresent(), Component.literal("the pet spoke up before it had been short of anything for long"));
        });
        helper.runAfterDelay(LACKED_TICKS + SAY_TICKS, () -> {
            assertSaid(helper, pet, VoiceMoment.MISSING);
            WhyNot why = pet.getMind().whyNot().orElseThrow(() -> new AssertionError("the pet found nothing wrong"));
            helper.assertTrue(why.lack().filter(lack -> lack.translationKey().equals("tag.item.chiikawa.entity_plant_crops")).isPresent(),
                Component.literal("the pet is short of " + why.lack()));
            PetSpeech.Said first = lastSaid(pet).orElseThrow();
            helper.runAfterDelay(QUIET_TICKS, () -> {
                helper.assertTrue(lastSaid(pet).orElseThrow().equals(first), Component.literal("the pet said it again: " + lastSaid(pet)));
                helper.succeed();
            });
        });
    }

    /** The world's chatter turned right down silences a remark as it does any other line, and the pet still knows what it is short of. */
    @GameTest(template = "floor16", batch = SILENT_BATCH, timeoutTicks = LACKED_TICKS + SAY_TICKS + 100)
    public static void a_farmer_out_of_seeds_keeps_quiet_in_a_world_with_no_chatter(GameTestHelper helper) {
        quietYard(helper, NOON);
        ServerPlayer owner = player(helper);
        AbstractPet pet = farmerOf(helper, owner, new BlockPos(8, STAND, 8));
        keepNear(helper, owner, pet);

        helper.runAfterDelay(LACKED_TICKS + SAY_TICKS, () -> {
            helper.assertFalse(said(pet).isPresent(), Component.literal("the pet spoke in a world with no chatter: " + said(pet)));
            helper.assertTrue(pet.getMind().whyNot().flatMap(WhyNot::lack).isPresent(), Component.literal("the pet short of seeds found nothing it lacks"));
            helper.succeed();
        });
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

        helper.assertFalse(clientsMenu.outlook().isPresent(), Component.literal("the menu knew something before it was told"));
        helper.runAfterDelay(40, () -> {
            new PetBackpackMenu(2, owner.getInventory(), pet).broadcastChanges();
            clientsMenu.receive(outlookOf(helper, pet));
            PetOutlookPayload shortOfSeeds = clientsMenu.outlook().orElseThrow(() -> new AssertionError("the menu was told nothing"));
            // Strolling is all there is to do, and a pet that is strolling is not told it will stroll next.
            helper.assertTrue(shortOfSeeds.next().isEmpty() || !shortOfSeeds.next().equals(pet.getIntent()), Component.literal("the pet said " + shortOfSeeds));
            helper.assertTrue(shortOfSeeds.whyNotKey().equals(Optional.of("intent.chiikawa.fail.no_farmland")), Component.literal("the pet said " + shortOfSeeds));
            helper.assertTrue(shortOfSeeds.lackKey().equals(Optional.of("tag.item.chiikawa.entity_plant_crops")), Component.literal("the pet said " + shortOfSeeds));

            pet.pockets().stow(new ItemStack(Items.WHEAT_SEEDS, 8));
            helper.runAfterDelay(40, () -> {
                clientsMenu.receive(outlookOf(helper, pet));
                PetOutlookPayload withSeeds = clientsMenu.outlook().orElseThrow();
                helper.assertTrue(withSeeds.whyNotKey().equals(Optional.of("intent.chiikawa.fail.no_crop")), Component.literal("the pet said " + withSeeds));
                helper.assertTrue(withSeeds.lackKey().isEmpty(), Component.literal("the pet with seeds is short of " + withSeeds.lackKey()));
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
