package com.dwinovo.chiikawa.testing;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

/**
 * An errand of a given id and a given answer to whether it goes on, in no registry, for a test of what runs or
 * judges an errand without the errands of the mod. Two of the same id are two types of one errand, as a reload of
 * the data makes.
 */
public final class StubErrand implements ErrandType<Plan, String> {
    private final ResourceLocation id;
    private final Check stillWanted;

    /** @param id the errand's id */
    public StubErrand(ResourceLocation id) {
        this(id, Check.OK);
    }

    /**
     * @param id the errand's id
     * @param stillWanted what it answers to whether it goes on
     */
    public StubErrand(ResourceLocation id, Check stillWanted) {
        this.id = id;
        this.stillWanted = stillWanted;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.WANDER;
    }

    @Override
    public Codec<String> tuningCodec() {
        return Codec.STRING;
    }

    @Override
    public Offers<Plan> offers(PetView view, String tuning) {
        return Offers.none(Check.fail("stub"));
    }

    @Override
    public Check stillWanted(Plan plan, PetView view, String tuning) {
        return stillWanted;
    }

    @Override
    public Routine routine(Plan plan, ErrandSettings<String> settings) {
        return Routine.of();
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shared();
    }
}
