package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.MIDNIGHT;
import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.assertSaid;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.pet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.quietYard;
import static com.dwinovo.chiikawa.gametest.GameTestKit.said;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.PetTaskTypeData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.task.TaskTracker;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.PetVoices;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestDontPrefix;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * What the pets say, and when: each at a moment the game already reacts to, in its own
 * words, and then quiet for a while. What a pet said is read back from its memory of it —
 * the same memory that keeps it quiet afterwards — because the stand-in players these cases
 * make cannot be sent a mod's own packets. Some stand beside the pets all the same, which
 * is a case in itself: a player whose game cannot hear a pet is left out, not an error.
 *
 * <p>Pets within earshot of each other take turns, so every case here is a batch of its
 * own, and starts by hushing whatever pets earlier batches left about: a case next door
 * talking at the same moment would otherwise count against the crowd this one is allowed.
 */
@GameTestHolder(namespace = Constants.MOD_ID)
@GameTestDontPrefix
public final class VoiceGameTests {
    private static final int STAND = 2;
    /** Long enough for a pet to notice a zombie and set off after it. */
    private static final int FIGHT_TICKS = 600;
    /**
     * How long a pet at leisure is watched for a word. Usagi speaks up about one tick in
     * five hundred, so by this time a silent Usagi is one that cannot talk.
     */
    private static final int LEISURE_TICKS = 4000;
    /** How many monsters the slip in the pay case asks for. */
    private static final int QUARRY = 2;

    /** Chiikawa, hurt, cries out: one of its own hurt lines. */
    @GameTest(template = "floor8", batch = "chiikawa_voice_hurt", timeoutTicks = 100)
    public static void a_hurt_chiikawa_cries_out(GameTestHelper helper) {
        quietYard(helper, NOON);
        player(helper).setPos(helper.absoluteVec(new BlockPos(3, STAND, 5).getCenter()));
        AbstractPet pet = still(pet(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(3, STAND, 3), false));

        pet.hurt(pet.damageSources().generic(), 1.0F);

        assertSaid(helper, pet, VoiceMoment.HURT);
        helper.succeed();
    }

    /** Rakko takes a hit without a word: a moment with no lines passes in silence. */
    @GameTest(template = "floor8", batch = "chiikawa_voice_stoic", timeoutTicks = 100)
    public static void rakko_takes_a_hit_without_a_word(GameTestHelper helper) {
        quietYard(helper, NOON);
        AbstractPet pet = still(pet(helper, InitEntity.RAKKO_PET.get(), new BlockPos(3, STAND, 3), false));

        pet.hurt(pet.damageSources().generic(), 1.0F);

        helper.assertTrue(pet.getHealth() < pet.getMaxHealth(), "Rakko was not hurt at all");
        helper.assertFalse(said(pet).isPresent(), "Rakko cried out");
        helper.succeed();
    }

    /** Tamed, a pet says so, to the one who tamed it standing right there. */
    @GameTest(template = "floor8", batch = "chiikawa_voice_tame", timeoutTicks = 100)
    public static void a_pet_says_something_when_it_is_tamed(GameTestHelper helper) {
        quietYard(helper, NOON);
        ServerPlayer tamer = player(helper);
        tamer.setPos(helper.absoluteVec(new BlockPos(3, STAND, 5).getCenter()));
        tamer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKIE));
        AbstractPet pet = still(pet(helper, InitEntity.SHISA_PET.get(), new BlockPos(3, STAND, 3), false));

        // Taming is a roll of the dice: offered enough cookies, any pet gives in.
        for (int offer = 0; offer < 100 && !pet.isTame(); offer++) {
            pet.mobInteract(tamer, InteractionHand.MAIN_HAND);
        }

        helper.assertTrue(pet.isTame(), "a hundred cookies did not tame Shisa");
        assertSaid(helper, pet, VoiceMoment.TAME);
        helper.succeed();
    }

    /** Paid for a finished slip, a pet says something about it. */
    @GameTest(template = "floor8", batch = "chiikawa_voice_paid", timeoutTicks = 100)
    public static void a_pet_says_something_when_it_is_paid(GameTestHelper helper) {
        quietYard(helper, NOON);
        AbstractPet pet = still(pet(helper, InitEntity.RAKKO_PET.get(), new BlockPos(3, STAND, 3), true));
        pet.setTask(new PetTask(PetTaskTypeData.MELEE_HUNTING,
            Services.REGISTRY.getKey(InitRegistry.PET_JOB_KEY, InitRegistry.FENCER.get()), PetWorkCounters.SLAY, PetTask.NO_ICON,
            QUARRY, PetTaskTypeData.reward(PetTaskTypeData.MELEE_HUNTING), 0));

        TaskTracker.advance(pet, PetWorkCounters.SLAY, QUARRY);

        helper.assertTrue(pet.getTask().isEmpty(), "the slip was not finished");
        assertSaid(helper, pet, VoiceMoment.PAID);
        helper.succeed();
    }

    /**
     * Setting off after a zombie is when a pet shouts its battle cry. At night, so the
     * zombie does not burn up before anyone goes after it.
     */
    @GameTest(template = "floor16", batch = "chiikawa_voice_hunt", timeoutTicks = FIGHT_TICKS)
    public static void a_pet_setting_off_to_fight_shouts(GameTestHelper helper) {
        quietYard(helper, MIDNIGHT);
        AbstractPet pet = holding(pet(helper, InitEntity.RAKKO_PET.get(), new BlockPos(4, STAND, 4), true),
            Items.IRON_SWORD);
        helper.spawn(EntityType.ZOMBIE, new BlockPos(8, STAND, 4));

        helper.succeedWhen(() -> assertSaid(helper, pet, VoiceMoment.HUNT));
    }

    /** A pet with nothing to do says something now and then. */
    @GameTest(template = "floor16", batch = "chiikawa_voice_idle", timeoutTicks = LEISURE_TICKS)
    public static void a_pet_at_leisure_says_something_now_and_then(GameTestHelper helper) {
        quietYard(helper, NOON);
        AbstractPet pet = pet(helper, InitEntity.USAGI_PET.get(), new BlockPos(8, STAND, 8), false);

        helper.succeedWhen(() -> assertSaid(helper, pet, VoiceMoment.IDLE));
    }

    /** Having said something, a pet keeps quiet until its cooldown is over, and not a tick longer. */
    @GameTest(template = "floor8", batch = "chiikawa_voice_cooldown", timeoutTicks = 400)
    public static void a_pet_keeps_quiet_for_its_cooldown(GameTestHelper helper) {
        quietYard(helper, NOON);
        AbstractPet pet = still(pet(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(3, STAND, 3), false));
        int cooldown = PetVoices.of(pet.getType()).cooldownTicks();

        helper.assertTrue(PetSpeech.say(pet, VoiceMoment.HURT).isPresent(), "Chiikawa said nothing the first time");
        helper.assertFalse(PetSpeech.say(pet, VoiceMoment.HURT).isPresent(), "Chiikawa spoke again straight away");
        helper.runAfterDelay(cooldown - 1, () -> helper.assertFalse(PetSpeech.say(pet, VoiceMoment.HURT).isPresent(),
            "Chiikawa spoke before its cooldown was over"));
        helper.runAfterDelay(cooldown, () -> {
            helper.assertTrue(PetSpeech.say(pet, VoiceMoment.HURT).isPresent(),
                "Chiikawa was still quiet once its cooldown was over");
            helper.succeed();
        });
    }

    /**
     * A yard full of pets takes turns. With as many talking as the crowd allows, one more
     * waits until they have finished, and then has its say.
     */
    @GameTest(template = "floor8", batch = "chiikawa_voice_crowd", timeoutTicks = 200)
    public static void a_crowd_of_pets_takes_turns(GameTestHelper helper) {
        quietYard(helper, NOON);
        List<AbstractPet> talking = new ArrayList<>();
        int crowd = PetVoices.of(InitEntity.CHIIKAWA_PET.get()).crowdLimit();
        for (int i = 0; i < crowd; i++) {
            talking.add(still(pet(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(1 + i, STAND, 2), false)));
        }
        AbstractPet last = still(pet(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(3, STAND, 5), false));

        for (AbstractPet pet : talking) {
            helper.assertTrue(PetSpeech.say(pet, VoiceMoment.HURT).isPresent(), "a pet in a quiet yard said nothing");
        }
        helper.assertFalse(PetSpeech.say(last, VoiceMoment.HURT).isPresent(), "one pet too many talked at once");
        helper.runAfterDelay(PetVoices.of(InitEntity.CHIIKAWA_PET.get()).talkTicks(), () -> {
            helper.assertTrue(PetSpeech.say(last, VoiceMoment.HURT).isPresent(),
                "the last pet was still waiting after the others had finished");
            helper.succeed();
        });
    }

    /** A pet left standing: nothing it thinks of doing gets in the way of what a case does to it. */
    private static AbstractPet still(AbstractPet pet) {
        pet.setNoAi(true);
        return pet;
    }
}
