package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTag;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
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
    /** What a newcomer is handed: the handbook, by the advancement that marks their arrival. */
    public static final ResourceLocation HANDBOOK_GIFT = new ResourceLocation(Constants.MOD_ID, "gifts/handbook");

    public ModLootTableProvider(PackOutput output) {
        super(output, Set.of(), List.of(
            new SubProviderEntry(BlockDrops::new, LootContextParamSets.BLOCK),
            new SubProviderEntry(SlipRewards::new, LootContextParamSets.GIFT),
            new SubProviderEntry(Gifts::new, LootContextParamSets.ADVANCEMENT_REWARD)
        ));
    }

    /** What advancements hand out. */
    private static final class Gifts implements LootTableSubProvider {
        @Override
        public void generate(BiConsumer<ResourceLocation, LootTable.Builder> output) {
            output.accept(HANDBOOK_GIFT, LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(InitItems.HANDBOOK.get()))));
        }
    }

    private static final class BlockDrops implements LootTableSubProvider {
        @Override
        public void generate(BiConsumer<ResourceLocation, LootTable.Builder> output) {
            dropSelf(output, InitBlocks.LABOR_BOARD.get());
            dropSelf(output, InitBlocks.SHOP.get());
        }

        private static void dropSelf(BiConsumer<ResourceLocation, LootTable.Builder> output, Block block) {
            output.accept(block.getLootTable(), LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block))
                .when(ExplosionCondition.survivesExplosion())));
        }
    }

    /**
     * Mostly money, now and then a little something (gameplay doc, section 3). The money is
     * whatever is in the currency tag, so a pack that changes what money is changes what
     * slips pay without touching these.
     */
    private static final class SlipRewards implements LootTableSubProvider {
        private static final float EXTRA_CHANCE = 0.3F;

        @Override
        public void generate(BiConsumer<ResourceLocation, LootTable.Builder> output) {
            reward(output, PetTaskTypeData.WEEDING, 1, 2, Items.BREAD, 1, 1);
            reward(output, PetTaskTypeData.STREET_PERFORMANCE, 2, 4, Items.COOKIE, 2, 4);
            // Hunting pays most: it is the only work a pet can fail by falling.
            reward(output, PetTaskTypeData.MELEE_HUNTING, 4, 7, Items.COOKED_BEEF, 1, 2);
            reward(output, PetTaskTypeData.RANGED_HUNTING, 4, 7, Items.ARROW, 4, 8);
        }

        private static void reward(BiConsumer<ResourceLocation, LootTable.Builder> output, ResourceLocation type,
                int minPay, int maxPay, Item extra, int minExtra, int maxExtra) {
            output.accept(PetTaskTypeData.reward(type), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(TagEntry.expandTag(InitTag.CURRENCY)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(minPay, maxPay)))))
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .when(LootItemRandomChanceCondition.randomChance(EXTRA_CHANCE))
                    .add(LootItem.lootTableItem(extra)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(minExtra, maxExtra))))));
        }
    }
}
