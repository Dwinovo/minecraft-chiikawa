package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.item.PetDollItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Where a pet comes from and what happens after it is gone: the tool a wild one is born
 * with, and the doll that brings a dead one back with everything it had — and only that,
 * since a doll anyone could make would be a spawn egg.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LifeGameTests {
    private static final String BATCH = "chiikawa_life";
    private static final int RITUAL_TICKS = 600;

    private static final int STAND = 2;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /**
     * A pet the world spawned for itself arrives with a tool and a life of its own. Which
     * tool is the personality's business and may be either of two, so the case asks only
     * that its hands are not empty — a wild pet holding nothing is a pet with no job, and
     * nothing to do all day.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_wild_pet_is_born_with_a_tool(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(new BlockPos(3, STAND, 3));
        AbstractPet pet = InitEntity.USAGI_PET.get().spawn(helper.getLevel(), at, MobSpawnType.NATURAL);

        helper.assertTrue(pet != null, "nothing spawned");
        helper.assertFalse(pet.getMainHandItem().isEmpty(), "a wild pet turned up empty-handed");
        // Usagi's personality deals a weapon or a hoe and nothing else; a third thing in its
        // hands means the draw stopped reading the personality it belongs to.
        helper.assertTrue(pet.getMainHandItem().is(InitItems.USAGI_WEAPON.get())
                || pet.getMainHandItem().is(Items.WOODEN_HOE),
            "a wild rabbit turned up with something its personality never deals: "
                + pet.getMainHandItem());
        helper.assertTrue(pet.getPetDirective() == PetDirective.FREE,
            "a wild pet was not left to its own devices");
        helper.succeed();
    }

    /**
     * The whole of dying and coming back: a pet with a hoe is killed, leaves a doll, and the
     * doll on a cake brings it back still holding the hoe. Somebody has to be able to undo
     * an accident, and undoing it has to give back what was lost, or the doll is a souvenir
     * rather than a second chance.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = RITUAL_TICKS)
    public static void a_doll_on_a_cake_brings_the_pet_back_with_its_tool(GameTestHelper helper) {
        AbstractPet pet = holding(ownedPet(helper, new BlockPos(3, STAND, 3)), Items.WOODEN_HOE);
        pet.getBackpack().addItem(new ItemStack(Items.WHEAT, 5));
        pet.hurt(pet.damageSources().generic(), pet.getMaxHealth() * 2.0F);

        BlockPos cake = new BlockPos(5, STAND, 3);
        helper.setBlock(cake, Blocks.CAKE);

        helper.runAtTickTime(40, () -> {
            ItemStack doll = GameTestKit.dollNear(helper, new BlockPos(3, STAND, 3), InitItems.USAGI_DOLL.get());
            Player player = GameTestKit.owner(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, doll);
            helper.useBlock(cake, player);
        });

        helper.succeedWhen(() -> {
            AbstractPet revived = helper.getLevel()
                .getEntitiesOfClass(AbstractPet.class, new AABB(helper.absolutePos(cake)).inflate(6.0))
                .stream()
                .filter(AbstractPet::isAlive)
                .findFirst()
                .orElse(null);
            helper.assertTrue(revived != null, "nothing came back from the cake");
            helper.assertTrue(revived.getMainHandItem().is(Items.WOODEN_HOE),
                "the pet came back without the hoe it died with");
            helper.assertTrue(GameTestKit.carries(revived, Items.WHEAT),
                "the pet came back without what was in its bag");
        });
    }

    /**
     * A doll nobody died in — only creative mode hands one out — brings a new pet of its
     * kind, which belongs to nobody and goes its own way until someone tames it, the same
     * as one met in the wild.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = RITUAL_TICKS)
    public static void a_blank_doll_on_a_cake_brings_a_wild_pet(GameTestHelper helper) {
        BlockPos cake = new BlockPos(4, STAND, 3);
        helper.setBlock(cake, Blocks.CAKE);
        // Full, as the other doll case's player is: this goes straight to the block and past
        // the loaders' click events, so a player who could eat would take a slice instead.
        Player player = GameTestKit.owner(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(InitItems.MOMONGA_DOLL.get()));
        helper.useBlock(cake, player);

        helper.succeedWhen(() -> {
            AbstractPet pet = helper.getLevel()
                .getEntitiesOfClass(AbstractPet.class, new AABB(helper.absolutePos(cake)).inflate(6.0))
                .stream()
                .filter(AbstractPet::isAlive)
                .findFirst()
                .orElse(null);
            helper.assertTrue(pet != null, "nothing came from the cake");
            helper.assertTrue(pet.getType() == InitEntity.MOMONGA_PET.get(), "the doll brought the wrong friend: " + pet.getType());
            helper.assertFalse(pet.isTame(), "a blank doll handed over a pet already tamed");
            helper.assertTrue(pet.getPetDirective() == PetDirective.FREE,
                "a pet nobody owns is waiting to follow someone");
        });
    }

    /**
     * Wild pets spawn where the data pack says, and only there: the plains are on the mod's
     * own list and the ocean is not. The spawns reach a biome only through the pet_spawn
     * data, so this is also the case that the data got there at all.
     */
    @GameTest(template = "floor8", batch = BATCH)
    public static void wild_pets_spawn_where_the_data_pack_says(GameTestHelper helper) {
        Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        EntityType<?> usagi = InitEntity.USAGI_PET.get();

        helper.assertTrue(spawns(biomes.getOrThrow(Biomes.PLAINS), usagi), "no Usagi turns up on the plains");
        helper.assertFalse(spawns(biomes.getOrThrow(Biomes.OCEAN), usagi), "Usagi turns up at sea, where no list puts it");
        helper.succeed();
    }

    private static boolean spawns(Biome biome, EntityType<?> type) {
        return biome.getMobSettings().getMobs(type.getCategory()).unwrap().stream()
            .anyMatch(spawner -> spawner.type == type);
    }

    /**
     * A doll is what a pet leaves behind, and the way back for that pet — nothing makes one.
     * A doll anyone could make would bring pets out of thin air: a spawn egg by another name.
     */
    @GameTest(template = "floor8", batch = BATCH)
    public static void no_doll_can_be_made(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        List<RecipeHolder<?>> recipes = List.copyOf(helper.getLevel().getRecipeManager().getRecipes());
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof PetDollItem) {
                helper.assertFalse(recipes.stream().anyMatch(recipe -> recipe.value().getResultItem(registries).is(item)),
                    BuiltInRegistries.ITEM.getKey(item) + " can be made, which turns it into a spawn egg");
            }
        }
        helper.succeed();
    }

}
