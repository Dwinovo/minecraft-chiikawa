package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitItems;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class ModRecipeProvider extends RecipeProvider {
    private final CompletableFuture<HolderLookup.Provider> registriesFuture;

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
        this.registriesFuture = registries;
    }

    @Override
    public void buildRecipes(RecipeOutput recipeOutput) {
        ItemStack result = new ItemStack(InitItems.USAGI_WEAPON.get());
        result.enchant(Enchantments.FIRE_ASPECT, 1);

        saveEnchantedShaped(
                recipeOutput,
                RecipeCategory.COMBAT,
                result,
                new ResourceLocation(Constants.MOD_ID, "usagi_weapon"),
                getHasName(Items.YELLOW_WOOL),
                has(Items.YELLOW_WOOL),
                Map.of(
                        'Y', Ingredient.of(Items.YELLOW_WOOL),
                        'F', Ingredient.of(Items.FLINT),
                        'S', Ingredient.of(Items.STICK)
                ),
                "  Y",
                "FSF",
                "Y  "
        );

        ItemStack hachiwareResult = new ItemStack(InitItems.HACHIWARE_WEAPON.get());
        hachiwareResult.enchant(Enchantments.KNOCKBACK, 1);
        saveEnchantedShaped(
                recipeOutput,
                RecipeCategory.COMBAT,
                hachiwareResult,
                new ResourceLocation(Constants.MOD_ID, "hachiware_weapon"),
                getHasName(Items.BLUE_WOOL),
                has(Items.BLUE_WOOL),
                Map.of(
                        'B', Ingredient.of(Items.BLUE_WOOL),
                        'S', Ingredient.of(Items.STICK)
                ),
                " B ",
                " SB",
                "S  "
        );

        ItemStack chiikawaResult = new ItemStack(InitItems.CHIIKAWA_WEAPON.get());
        chiikawaResult.enchant(Enchantments.KNOCKBACK, 1);
        saveEnchantedShaped(
                recipeOutput,
                RecipeCategory.COMBAT,
                chiikawaResult,
                new ResourceLocation(Constants.MOD_ID, "chiikawa_weapon"),
                getHasName(Items.PINK_WOOL),
                has(Items.PINK_WOOL),
                Map.of(
                        'P', Ingredient.of(Items.PINK_WOOL),
                        'S', Ingredient.of(Items.STICK)
                ),
                " P ",
                " SP",
                "S  "
        );
    }

    private void saveEnchantedShaped(
            RecipeOutput recipeOutput,
            RecipeCategory category,
            ItemStack result,
            ResourceLocation id,
            String unlockName,
            Criterion<?> unlockCriterion,
            Map<Character, Ingredient> key,
            String... pattern
    ) {
        ShapedRecipePattern shapedPattern = ShapedRecipePattern.of(key, List.of(pattern));
        ShapedRecipe recipe = new ShapedRecipe(
                "",
                RecipeBuilder.determineBookCategory(category),
                shapedPattern,
                result,
                true
        );
        Advancement.Builder advancement = recipeOutput.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                .rewards(AdvancementRewards.Builder.recipe(id))
                .requirements(AdvancementRequirements.Strategy.OR)
                .addCriterion(unlockName, unlockCriterion);
        recipeOutput.accept(
                id,
                recipe,
                advancement.build(id.withPrefix("recipes/" + category.getFolderName() + "/"))
        );
    }
}
