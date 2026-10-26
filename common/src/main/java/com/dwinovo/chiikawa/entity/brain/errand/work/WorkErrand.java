package com.dwinovo.chiikawa.entity.brain.errand.work;

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
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The errand a {@link Work} makes: its requirements first, then its action, which does the rest. Made by the loader
 * from the errand file of the work, so it exists only while that file does.
 *
 * @param <P> the shape of its plans
 * @param <T> the shape of its tuning
 */
public final class WorkErrand<P extends Plan, T> implements ErrandType<P, T> {
    private final ResourceLocation id;
    private final Work<P, T> work;

    /**
     * @param id the id of the errand, which is the name of its file
     * @param work what the file says about the work
     */
    public WorkErrand(ResourceLocation id, Work<P, T> work) {
        this.id = id;
        this.work = work;
    }

    /** @return what does the work; the progress it leaves in memory is its own, see {@link WorkAction#forgetWhenUnavailable} */
    public WorkAction<P, T> action() {
        return work.action();
    }

    /** @return whether the work is done to a target of this kind */
    public boolean takes(WorkTarget.Kind kind) {
        return work.target().filter(target -> target.kind() == kind).isPresent();
    }

    /** @return the kind of block the work is done to, if it is done to a block the work sensor finds */
    public Optional<WorkTarget.BlockTarget> blocks() {
        return work.target().filter(WorkTarget.BlockTarget.class::isInstance).map(WorkTarget.BlockTarget.class::cast);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public ErrandCategory category() {
        return work.category();
    }

    @Override
    public Codec<T> tuningCodec() {
        return work.action().tuningCodec();
    }

    @Override
    public Offers<P> offers(PetView view, T tuning) {
        Check required = required(view);
        return required.ok() ? work.action().offers(work, id, view, tuning) : Offers.none(required);
    }

    @Override
    public Check stillWanted(P plan, PetView view, T tuning) {
        Check required = required(view);
        return required.ok() ? work.action().stillWanted(work, id, plan, view, tuning) : required;
    }

    @Override
    public Routine routine(P plan, ErrandSettings<T> settings) {
        return work.action().routine(work, plan, settings);
    }

    @Override
    public Optional<ResourceLocation> workCounter() {
        return work.counter();
    }

    @Override
    public RestStore.Kind restStore() {
        return work.action().restStore();
    }

    private Check required(PetView view) {
        for (WorkRequirement requirement : work.requires()) {
            Check check = requirement.check(work, view);
            if (!check.ok()) {
                return check;
            }
        }
        return Check.OK;
    }
}
