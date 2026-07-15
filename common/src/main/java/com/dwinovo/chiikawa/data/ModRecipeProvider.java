package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitItems;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    public void buildRecipes() {
        HolderGetter<Item> itemLookup = this.registries.lookupOrThrow(Registries.ITEM);
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, InitItems.USAGI_WEAPON.get())
            .define('Y', Items.WOOL.yellow())
            .define('F', Items.FLINT)
            .define('S', Items.STICK)
            .pattern("  Y")
            .pattern("FSF")
            .pattern("Y  ")
            .unlockedBy(getHasName(Items.WOOL.yellow()), has(Items.WOOL.yellow()))
            .save(this.output, ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "usagi_weapon")));

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, InitItems.HACHIWARE_WEAPON.get())
            .define('B', Items.WOOL.blue())
            .define('S', Items.STICK)
            .pattern(" B ")
            .pattern(" SB")
            .pattern("S  ")
            .unlockedBy(getHasName(Items.WOOL.blue()), has(Items.WOOL.blue()))
            .save(this.output, ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "hachiware_weapon")));

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, InitItems.CHIIKAWA_WEAPON.get())
            .define('P', Items.WOOL.pink())
            .define('S', Items.STICK)
            .pattern(" P ")
            .pattern(" SP")
            .pattern("S  ")
            .unlockedBy(getHasName(Items.WOOL.pink()), has(Items.WOOL.pink()))
            .save(this.output, ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chiikawa_weapon")));
    }
}
