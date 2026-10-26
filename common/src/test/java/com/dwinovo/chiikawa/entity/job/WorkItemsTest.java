package com.dwinovo.chiikawa.entity.job;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.JobWorkData;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Errands;
import com.dwinovo.chiikawa.entity.job.impl.NoneJob;
import com.dwinovo.chiikawa.testing.TestErrands;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * What a job works with, so that a pet does not give it as a present. What is a job's tool is a tag, and no tag is bound
 * outside a game: the tool and the arrows are tried in the game, where {@code GiftGameTests} has the pets give.
 */
class WorkItemsTest {
    @BeforeAll
    static void shippedErrands() {
        TestErrands.load();
    }

    private static Predicate<ItemStack> of(ResourceLocation job) {
        List<ErrandType<?, ?>> errands = new ArrayList<>(Errands.generic());
        JobWorkData.all().get(job).work().forEach(id -> errands.add(Errands.get(id)));
        return WorkItems.of(new NoneJob(0), errands);
    }

    @Test
    void theSeedsAFarmerPlantsAreWhatItWorksWith() {
        Predicate<ItemStack> farmer = of(JobIds.FARMER);

        assertTrue(farmer.test(new ItemStack(Items.WHEAT_SEEDS)));
        assertFalse(farmer.test(new ItemStack(Items.COOKIE)));
        assertFalse(farmer.test(new ItemStack(Items.WHEAT)), "the harvest is not what the farmer plants");
    }

    @Test
    void nobodyButTheFarmerWorksWithSeeds() {
        assertFalse(of(JobIds.FENCER).test(new ItemStack(Items.WHEAT_SEEDS)));
        assertFalse(of(JobIds.ARCHER).test(new ItemStack(Items.WHEAT_SEEDS)));
        assertFalse(of(JobIds.MUSICIAN).test(new ItemStack(Items.WHEAT_SEEDS)));
    }

    @Test
    void aPetWithNoJobWorksWithNothing() {
        assertFalse(WorkItems.of(new NoneJob(0), Errands.generic()).test(new ItemStack(Items.WHEAT_SEEDS)));
    }
}
