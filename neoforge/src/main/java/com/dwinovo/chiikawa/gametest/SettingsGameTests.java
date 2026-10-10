package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.pet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.quietYard;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.network.SettingsPayloads.ChangeWorldSettingsPayload;
import com.dwinovo.chiikawa.network.SettingsServerPacketHandler;
import com.dwinovo.chiikawa.settings.WorldSettings;
import com.dwinovo.chiikawa.settings.WorldSettingsStore;
import com.dwinovo.chiikawa.spawn.WildSpawning;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The world's settings, as the server applies them: chatter silences a pet at a moment it
 * would speak, wild spawning stops the game's own spawns but not a spawn egg, and only an
 * operator's change is taken.
 *
 * <p>The settings are the world's, one set for every case at once, so each case here is a
 * batch of its own and puts them back as it found them. What a screen shows and sends is
 * the client's, which these cases have none of; the handler's logic is called as the
 * handler calls it.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SettingsGameTests {
    private static final int STAND = 2;
    /** The permission level of a game master. */
    private static final int GAME_MASTER = 2;

    /** With no chatter a pet says nothing at a moment it would speak at, and speaks again once it is turned back up. */
    @GameTest(template = "floor8", batch = "chiikawa_settings_chatter", timeoutTicks = 100)
    public static void no_chatter_silences_a_pet(GameTestHelper helper) {
        quietYard(helper, NOON);
        AbstractPet pet = still(pet(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(3, STAND, 3), false));
        WorldSettingsStore store = WorldSettingsStore.of(helper.getLevel());
        WorldSettings before = store.get();
        try {
            store.set(new WorldSettings(before.wildSpawn(), 0.0F));
            helper.assertFalse(PetSpeech.say(pet, VoiceMoment.HURT).isPresent(), "Chiikawa spoke in a world with no chatter");

            store.set(new WorldSettings(before.wildSpawn(), 1.0F));
            helper.assertTrue(PetSpeech.say(pet, VoiceMoment.HURT).isPresent(),
                "Chiikawa stayed quiet once the chatter was turned back up");
        } finally {
            store.set(before);
        }
        helper.succeed();
    }

    /**
     * With no wild spawning the game's own spawns are refused, and a spawn egg still works.
     * The ground is grass in daylight, so the same spot passes with the setting at full: what
     * is refused is the setting's doing and nothing else's.
     */
    @GameTest(template = "floor8", batch = "chiikawa_settings_spawn", timeoutTicks = 100)
    public static void no_wild_spawning_stops_natural_spawns_but_not_spawn_eggs(GameTestHelper helper) {
        settleWorld(helper.getLevel(), Difficulty.NORMAL, NOON);
        BlockPos ground = new BlockPos(3, STAND - 1, 3);
        helper.setBlock(ground, Blocks.GRASS_BLOCK);
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(3, STAND, 3));
        var type = InitEntity.USAGI_PET.get();
        WorldSettingsStore store = WorldSettingsStore.of(level);
        WorldSettings before = store.get();
        try {
            store.set(new WorldSettings(1.0F, before.chatter()));
            helper.assertTrue(WildSpawning.canSpawn(type, level, MobSpawnType.NATURAL, at, level.getRandom()),
                "the spot is not one an animal may spawn on, so this case proves nothing");

            store.set(new WorldSettings(0.0F, before.chatter()));
            helper.assertFalse(WildSpawning.canSpawn(type, level, MobSpawnType.NATURAL, at, level.getRandom()),
                "a natural spawn went through with wild spawning at nothing");
            helper.assertFalse(WildSpawning.canSpawn(type, level, MobSpawnType.CHUNK_GENERATION, at, level.getRandom()),
                "a spawn while the chunk was made went through with wild spawning at nothing");
            helper.assertTrue(WildSpawning.canSpawn(type, level, MobSpawnType.SPAWN_EGG, at, level.getRandom()),
                "a spawn egg was refused");
            Animal born = type.spawn(level, at.above(), MobSpawnType.SPAWN_EGG);
            helper.assertTrue(born != null, "a spawn egg spawned nothing");
            born.discard();
        } finally {
            store.set(before);
        }
        helper.succeed();
    }

    /** Somebody with no say in the world asks to change it and is refused; an operator is taken. */
    @GameTest(template = "floor8", batch = "chiikawa_settings_change", timeoutTicks = 100)
    public static void only_an_operators_change_is_taken(GameTestHelper helper) {
        ServerPlayer stranger = player(helper);
        WorldSettingsStore store = WorldSettingsStore.of(helper.getLevel());
        WorldSettings before = store.get();
        WorldSettings asked = new WorldSettings(0.25F, 0.75F);
        try {
            helper.assertFalse(SettingsServerPacketHandler.canChange(stranger), "a stranger may change the world");
            SettingsServerPacketHandler.handleChange(new ChangeWorldSettingsPayload(asked), stranger);
            helper.assertTrue(store.get().equals(before), "a stranger's change was taken");

            // A game master, spelled out: the level a plain op gets is whatever the server's settings say.
            stranger.server.getPlayerList().getOps()
                .add(new ServerOpListEntry(stranger.getGameProfile(), GAME_MASTER, false));
            helper.assertTrue(SettingsServerPacketHandler.canChange(stranger), "an operator may not change the world");
            SettingsServerPacketHandler.handleChange(new ChangeWorldSettingsPayload(asked), stranger);
            helper.assertTrue(store.get().equals(asked), "an operator's change was not taken");
        } finally {
            stranger.server.getPlayerList().getOps().remove(stranger.getGameProfile());
            store.set(before);
        }
        helper.succeed();
    }

    /** A pet left standing: nothing it thinks of doing gets in the way of what a case does to it. */
    private static AbstractPet still(AbstractPet pet) {
        pet.setNoAi(true);
        return pet;
    }
}
