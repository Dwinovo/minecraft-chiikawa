package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    public void buildRecipes(RecipeOutput recipeOutput) {
        // Usagi Weapon recipe
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, InitItems.USAGI_WEAPON.get())
                .pattern("  Y")
                .pattern("FSF")
                .pattern("Y  ")
                .define('Y', Items.YELLOW_WOOL)
                .define('F', Items.FLINT)
                .define('S', Items.STICK)
                .unlockedBy(getHasName(Items.YELLOW_WOOL), has(Items.YELLOW_WOOL))
                .save(recipeOutput, new ResourceLocation(Constants.MOD_ID, "usagi_weapon"));

        // Hachiware Weapon recipe
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, InitItems.HACHIWARE_WEAPON.get())
                .pattern(" B ")
                .pattern(" SB")
                .pattern("S  ")
                .define('B', Items.BLUE_WOOL)
                .define('S', Items.STICK)
                .unlockedBy(getHasName(Items.BLUE_WOOL), has(Items.BLUE_WOOL))
                .save(recipeOutput, new ResourceLocation(Constants.MOD_ID, "hachiware_weapon"));

        // Chiikawa Weapon recipe
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, InitItems.CHIIKAWA_WEAPON.get())
                .pattern(" P ")
                .pattern(" SP")
                .pattern("S  ")
                .define('P', Items.PINK_WOOL)
                .define('S', Items.STICK)
                .unlockedBy(getHasName(Items.PINK_WOOL), has(Items.PINK_WOOL))
                .save(recipeOutput, new ResourceLocation(Constants.MOD_ID, "chiikawa_weapon"));
    }
}
