package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTag;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
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

        // Music Box recipe
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, InitItems.MUSIC_BOX.get())
                .pattern(" P ")
                .pattern("PNP")
                .pattern(" G ")
                .define('P', ItemTags.PLANKS)
                .define('N', Items.NOTE_BLOCK)
                .define('G', Items.GOLD_INGOT)
                .unlockedBy(getHasName(Items.NOTE_BLOCK), has(Items.NOTE_BLOCK))
                .save(recipeOutput, new ResourceLocation(Constants.MOD_ID, "music_box"));

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, InitItems.LABOR_BOARD.get())
                .define('P', Items.PAPER)
                .define('W', ItemTags.PLANKS)
                .define('S', Items.STICK)
                .pattern("PPP")
                .pattern("WWW")
                .pattern("S S")
                .unlockedBy(getHasName(Items.PAPER), has(Items.PAPER))
                .save(recipeOutput);

        // A plate of something hot: bread, a vegetable, and something cooked.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, InitItems.SIMPLE_DISH.get())
                .requires(Items.BREAD)
                .requires(Items.BOWL)
                .requires(InitTag.ENTITY_PLANT_CROPS)
                .unlockedBy(getHasName(Items.BREAD), has(Items.BREAD))
                .save(recipeOutput);

        // A small satchel: leather about a woollen body, hung on a string.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, InitItems.BEAR_BACKPACK.get())
                .define('L', Items.LEATHER)
                .define('W', ItemTags.WOOL)
                .define('S', Items.STRING)
                .pattern("S S")
                .pattern("LWL")
                .pattern("LLL")
                .unlockedBy(getHasName(Items.LEATHER), has(Items.LEATHER))
                .save(recipeOutput);

        // A counter: a slab of planks over a chest, with an emerald on the till.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, InitItems.SHOP.get())
                .define('W', ItemTags.PLANKS)
                .define('C', Items.CHEST)
                .define('E', Items.EMERALD)
                .pattern("WEW")
                .pattern("WCW")
                .pattern("W W")
                .unlockedBy(getHasName(Items.EMERALD), has(Items.EMERALD))
                .save(recipeOutput);
    }
}
