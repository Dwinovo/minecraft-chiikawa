package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTag;
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
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
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
        HolderLookup.Provider registries = registriesFuture.join();
        Holder<Enchantment> fireAspect = registries.lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.FIRE_ASPECT)
                .orElseThrow();
        Holder<Enchantment> knockback = registries.lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.KNOCKBACK)
                .orElseThrow();
        ItemStack result = new ItemStack(InitItems.USAGI_WEAPON.get());
        result.enchant(fireAspect, 1);

        saveEnchantedShaped(
                recipeOutput,
                RecipeCategory.COMBAT,
                result,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "usagi_weapon"),
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
        hachiwareResult.enchant(knockback, 1);
        saveEnchantedShaped(
                recipeOutput,
                RecipeCategory.COMBAT,
                hachiwareResult,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "hachiware_weapon"),
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
        chiikawaResult.enchant(knockback, 1);
        saveEnchantedShaped(
                recipeOutput,
                RecipeCategory.COMBAT,
                chiikawaResult,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "chiikawa_weapon"),
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

        saveEnchantedShaped(
                recipeOutput,
                RecipeCategory.MISC,
                new ItemStack(InitItems.MUSIC_BOX.get()),
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "music_box"),
                getHasName(Items.NOTE_BLOCK),
                has(Items.NOTE_BLOCK),
                Map.of(
                        'P', Ingredient.of(ItemTags.PLANKS),
                        'N', Ingredient.of(Items.NOTE_BLOCK),
                        'G', Ingredient.of(Items.GOLD_INGOT)
                ),
                " P ",
                "PNP",
                " G "
        );

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, InitItems.LABOR_BOARD.get())
                .define('P', Items.PAPER)
                .define('W', ItemTags.PLANKS)
                .define('S', Items.STICK)
                .pattern("PPP")
                .pattern("WWW")
                .pattern("S S")
                .unlockedBy(getHasName(Items.PAPER), has(Items.PAPER))
                .save(recipeOutput);

        // A little bell on a stick: gold for the ring, an emerald for the pets to hear.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, InitItems.PET_BELL.get())
                .pattern(" G ")
                .pattern("GEG")
                .pattern(" S ")
                .define('G', Items.GOLD_INGOT)
                .define('E', Items.EMERALD)
                .define('S', Items.STICK)
                .unlockedBy(getHasName(Items.EMERALD), has(Items.EMERALD))
                .save(recipeOutput);

        // A plate of something hot: bread, a vegetable, and something cooked.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, InitItems.SIMPLE_DISH.get())
                .requires(Items.BREAD)
                .requires(Items.BOWL)
                .requires(InitTag.ENTITY_PLANT_CROPS)
                .unlockedBy(getHasName(Items.BREAD), has(Items.BREAD))
                .save(recipeOutput);

        // The grey rucksack everybody takes to work: leather about a grey woollen body,
        // a string for each shoulder.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, InitItems.BACKPACK.get())
                .define('L', Items.LEATHER)
                .define('W', Items.GRAY_WOOL)
                .define('S', Items.STRING)
                .pattern("S S")
                .pattern("LWL")
                .pattern("LLL")
                .unlockedBy(getHasName(Items.LEATHER), has(Items.LEATHER))
                .save(recipeOutput);
        // The pouches: fleece in the friend's own colour, on a string.
        pouch(recipeOutput, InitItems.BEAR_POUCH.get(), Items.PINK_WOOL);
        pouch(recipeOutput, InitItems.WHALE_POUCH.get(), Items.LIGHT_BLUE_WOOL);
        pouch(recipeOutput, InitItems.STAR_POUCH.get(), Items.YELLOW_WOOL);

        // Another handbook, for one lost or given away: a book with a pink cover.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, InitItems.HANDBOOK.get())
                .requires(Items.BOOK)
                .requires(Items.PINK_DYE)
                .unlockedBy(getHasName(Items.BOOK), has(Items.BOOK))
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

    /** A pouch: a string over the top, three of its wool for the body. */
    private static void pouch(RecipeOutput recipeOutput, ItemLike pouch, ItemLike wool) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, pouch)
                .define('W', wool)
                .define('S', Items.STRING)
                .pattern("S S")
                .pattern("WWW")
                .unlockedBy(getHasName(wool), has(wool))
                .save(recipeOutput);
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
