package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTag;
import java.util.Set;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.MultiRegistryBootstrap;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

public final class ModRecipeProvider extends RecipeProvider {
    /** Any seed, by the convention tag every loader and most mods agree on. */
    private static final TagKey<Item> SEEDS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "seeds"));

    private final HolderGetter<Item> items;

    public ModRecipeProvider(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        super(recipes, advancements);
        this.items = recipes.lookup(Registries.ITEM);
    }

    /** Runs this over the recipes and the advancements they unlock, as the game runs its own; both loaders' data generators register it. */
    public static MultiRegistryBootstrap bootstrap() {
        return new MultiRegistryBootstrap() {
            @Override
            public Set<ResourceKey<? extends Registry<?>>> requestedRegistries() {
                return Set.of(Registries.RECIPE, Registries.ADVANCEMENT);
            }

            @Override
            public void run(MultiRegistryBootstrap.BootstrapGetter registries) {
                new ModRecipeProvider(registries.get(Registries.RECIPE), registries.get(Registries.ADVANCEMENT)).buildRecipes();
            }
        };
    }

    @Override
    public void buildRecipes() {
        HolderGetter<Item> itemLookup = this.items;
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

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.MISC, InitItems.MUSIC_BOX.get())
            .define('P', ItemTags.PLANKS)
            .define('N', Items.NOTE_BLOCK)
            .define('G', Items.GOLD_INGOT)
            .pattern(" P ")
            .pattern("PNP")
            .pattern(" G ")
            .unlockedBy(getHasName(Items.NOTE_BLOCK), has(Items.NOTE_BLOCK))
            .save(this.output, ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "music_box")));

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.DECORATIONS, InitItems.LABOR_BOARD.get())
            .define('P', Items.PAPER)
            .define('W', ItemTags.PLANKS)
            .define('S', Items.STICK)
            .pattern("PPP")
            .pattern("WWW")
            .pattern("S S")
            .unlockedBy(getHasName(Items.PAPER), has(Items.PAPER))
            .save(this.output);

        // A little bell on a stick: gold for the ring, an emerald for the pets to hear.
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.TOOLS, InitItems.PET_BELL.get())
            .pattern(" G ")
            .pattern("GEG")
            .pattern(" S ")
            .define('G', Items.GOLD_INGOT)
            .define('E', Items.EMERALD)
            .define('S', Items.STICK)
            .unlockedBy(getHasName(Items.EMERALD), has(Items.EMERALD))
            .save(this.output);

        // A plate of something hot: bread, a vegetable, and something cooked.
        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.FOOD, InitItems.SIMPLE_DISH.get())
            .requires(Items.BREAD)
            .requires(Items.BOWL)
            .requires(InitTag.ENTITY_PLANT_CROPS)
            .unlockedBy(getHasName(Items.BREAD), has(Items.BREAD))
            .save(this.output);

        // The grey rucksack everybody takes to work: leather about a grey woollen body,
        // a string for each shoulder.
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.TOOLS, InitItems.BACKPACK.get())
            .define('L', Items.LEATHER)
            .define('W', Items.WOOL.gray())
            .define('S', Items.STRING)
            .pattern("S S")
            .pattern("LWL")
            .pattern("LLL")
            .unlockedBy(getHasName(Items.LEATHER), has(Items.LEATHER))
            .save(this.output);
        // The pouches: fleece in the friend's own colour, on a string.
        pouch(itemLookup, InitItems.BEAR_POUCH.get(), Items.WOOL.pink());
        pouch(itemLookup, InitItems.WHALE_POUCH.get(), Items.WOOL.lightBlue());
        pouch(itemLookup, InitItems.STAR_POUCH.get(), Items.WOOL.yellow());

        // Another handbook, for one lost or given away: a book with a pink cover.
        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.MISC, InitItems.HANDBOOK.get())
            .requires(Items.BOOK)
            .requires(Items.DYE.pink())
            .unlockedBy(getHasName(Items.BOOK), has(Items.BOOK))
            .save(this.output);

        // The weeding study guide, for one who would rather not buy it: a book and any seed.
        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.MISC, InitItems.WEEDING_BOOK.get())
            .requires(Items.BOOK)
            .requires(SEEDS)
            .unlockedBy(getHasName(Items.BOOK), has(Items.BOOK))
            .save(this.output);

        // Sweets from a sugar spoon: three sugar make four candies you can blow.
        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.MISC, InitItems.WHISTLE_CANDY.get(), 4)
            .requires(Items.SUGAR, 3)
            .unlockedBy(getHasName(Items.SUGAR), has(Items.SUGAR))
            .save(this.output);

        // A counter: a slab of planks over a chest, with an emerald on the till.
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.DECORATIONS, InitItems.SHOP.get())
            .define('W', ItemTags.PLANKS)
            .define('C', Items.CHEST)
            .define('E', Items.EMERALD)
            .pattern("WEW")
            .pattern("WCW")
            .pattern("W W")
            .unlockedBy(getHasName(Items.EMERALD), has(Items.EMERALD))
            .save(this.output);

        // A low desk: a top of slabs on two legs, with a sheet of paper for the exam.
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.DECORATIONS, InitItems.EXAM_DESK.get())
            .define('W', ItemTags.WOODEN_SLABS)
            .define('S', Items.STICK)
            .define('P', Items.PAPER)
            .pattern("WWW")
            .pattern("SPS")
            .unlockedBy(getHasName(Items.PAPER), has(Items.PAPER))
            .save(this.output);
    }

    /** A pouch: a string over the top, three of its wool for the body. */
    private void pouch(HolderGetter<Item> itemLookup, ItemLike pouch, ItemLike wool) {
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.TOOLS, pouch)
            .define('W', wool)
            .define('S', Items.STRING)
            .pattern("S S")
            .pattern("WWW")
            .unlockedBy(getHasName(wool), has(wool))
            .save(this.output);
    }
}
