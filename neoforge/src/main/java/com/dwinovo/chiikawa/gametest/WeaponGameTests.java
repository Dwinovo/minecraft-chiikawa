package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.shop.ShopCatalog;
import com.dwinovo.chiikawa.shop.ShopCatalogs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The pets' own weapons, from the series. Rakko's sword is a fencer's like the others, and
 * the one way to come by it is to meet a Rakko: nobody makes one and no shop sells one.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WeaponGameTests {
    private static final String BATCH = "chiikawa_weapons";

    private static final int STAND = 2;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** A sword by the tag that says what a fencer holds, and so a fencer in whoever's hands. */
    @GameTest(template = "floor8", batch = BATCH)
    public static void rakkos_sword_makes_a_fencer(GameTestHelper helper) {
        Item sword = InitItems.RAKKO_SWORD.get();
        helper.assertTrue(new ItemStack(sword).is(InitTag.ENTITY_FENCER_TOOLS), "Rakko's sword is not a fencer's tool");
        AbstractPet pet = holding(wildPet(helper, new BlockPos(3, STAND, 3)), sword);
        helper.assertTrue(pet.getPetJobId() == InitRegistry.FENCER_ID, "Rakko's sword did not make a fencer");
        helper.succeed();
    }

    /** A Rakko the world spawned for itself carries its own sword, as it always does in the series. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_wild_rakko_is_born_with_its_sword(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(new BlockPos(3, STAND, 3));
        AbstractPet rakko = InitEntity.RAKKO_PET.get().spawn(helper.getLevel(), at, MobSpawnType.NATURAL);

        helper.assertTrue(rakko != null, "nothing spawned");
        helper.assertTrue(rakko.getMainHandItem().is(InitItems.RAKKO_SWORD.get()),
            "a wild Rakko turned up without its sword: " + rakko.getMainHandItem());
        helper.succeed();
    }

    /** Its hilt is carved for the top of the ranking; a crafting table cannot carve one. */
    @GameTest(template = "floor8", batch = BATCH)
    public static void nobody_makes_rakkos_sword(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        helper.assertFalse(helper.getLevel().getRecipeManager().getRecipes().stream()
                .anyMatch(recipe -> recipe.value().getResultItem(registries).is(InitItems.RAKKO_SWORD.get())),
            "Rakko's sword can be made");
        helper.succeed();
    }

    /** Nor bought, nor sold on: it is not on any price list the data pack loads. */
    @GameTest(template = "floor8", batch = BATCH)
    public static void no_shop_deals_in_rakkos_sword(GameTestHelper helper) {
        Item sword = InitItems.RAKKO_SWORD.get();
        helper.assertFalse(ShopCatalogs.all().isEmpty(), "no price list loaded, so this proves nothing");
        for (ShopCatalog catalog : ShopCatalogs.all().values()) {
            helper.assertTrue(catalog.sale(sword).isEmpty() && catalog.purchase(sword).isEmpty(),
                "a shop deals in Rakko's sword");
        }
        helper.succeed();
    }
}
