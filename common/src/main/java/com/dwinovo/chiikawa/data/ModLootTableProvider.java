package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.init.InitBlocks;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * Block drops and slip rewards. Built without the loaders' block loot helpers, which
 * each cover all blocks in their own way, so both loaders share this one provider.
 */
public final class ModLootTableProvider extends LootTableProvider {
    public ModLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), List.of(
            new SubProviderEntry(lookup -> new BlockDrops(), LootContextParamSets.BLOCK),
            new SubProviderEntry(lookup -> new SlipRewards(), LootContextParamSets.GIFT)
        ), registries);
    }

    private static final class BlockDrops implements LootTableSubProvider {
        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            dropSelf(output, InitBlocks.LABOR_BOARD.get());
            dropSelf(output, InitBlocks.SHOP.get());
        }

        private static void dropSelf(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output, Block block) {
            output.accept(block.getLootTable(), LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block))
                .when(ExplosionCondition.survivesExplosion())));
        }
    }

    /** Mostly emeralds, now and then a little something (gameplay doc, section 3). */
    private static final class SlipRewards implements LootTableSubProvider {
        private static final float EXTRA_CHANCE = 0.3F;

        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            reward(output, PetTaskTypeData.WEEDING, 1, 2, Items.BREAD, 1, 1);
            reward(output, PetTaskTypeData.MUSHROOM_PICKING, 1, 3, Items.MUSHROOM_STEW, 1, 1);
            reward(output, PetTaskTypeData.STREET_PERFORMANCE, 2, 4, Items.COOKIE, 2, 4);
        }

        private static void reward(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output, ResourceLocation type,
                int minEmeralds, int maxEmeralds, Item extra, int minExtra, int maxExtra) {
            output.accept(PetTaskTypeData.reward(type), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(Items.EMERALD)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(minEmeralds, maxEmeralds)))))
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .when(LootItemRandomChanceCondition.randomChance(EXTRA_CHANCE))
                    .add(LootItem.lootTableItem(extra)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(minExtra, maxExtra))))));
        }
    }
}
