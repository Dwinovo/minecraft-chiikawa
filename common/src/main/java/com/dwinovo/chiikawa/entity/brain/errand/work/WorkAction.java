package com.dwinovo.chiikawa.entity.brain.errand.work;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * What a pet does for a kind of work, written once in code and named by the errand files that use it: the
 * {@code "action"} of {@code data/<namespace>/pet_errand/<errand>.json}. It is to a {@link Work} what an errand
 * type is to its settings: stateless, asked only through the {@link PetView}, and what a pet has set out to do is
 * its {@link Plan}. The actions are fixed, see {@link WorkActions}; what varies between the works that share one is data.
 *
 * @param <P> the shape of its plans
 * @param <T> the shape of its tuning, the {@code tuning} block of the file
 */
public interface WorkAction<P extends Plan, T> {
    ResourceLocation id();

    /** @return the codec of the {@code tuning} block of the files that name this action */
    Codec<T> tuningCodec();

    /**
     * @param target what a file says the work is done to
     * @return why this action cannot do the work to that, or nothing; checked when the file is read, and a file that
     *         fails it is switched off
     */
    Optional<String> refuses(Optional<WorkTarget> target);

    /**
     * The ways the pet could set out on this work now, or why it could not. Pure, as
     * {@link com.dwinovo.chiikawa.entity.brain.errand.ErrandType#offers}.
     *
     * @param errand the id of the errand the work is, which is what the pet's sensors keep what they see of it under
     */
    Offers<P> offers(Work<P, T> work, ResourceLocation errand, PetView view, T tuning);

    /** Whether the work goes on: the condition to keep at it, as {@link com.dwinovo.chiikawa.entity.brain.errand.ErrandType#stillWanted}. */
    default Check stillWanted(Work<P, T> work, ResourceLocation errand, P plan, PetView view, T tuning) {
        return Check.OK;
    }

    /** @return the steps of the work, made for one start and used up by it */
    Routine routine(Work<P, T> work, P plan, ErrandSettings<T> settings);

    /** @return where the rests of the works of this action are kept, see {@link RestStore.Kind} */
    RestStore.Kind restStore();

    /** The named numbers the rests of its works point at that only this action can work out, see {@link RestSources}. */
    default Map<ResourceLocation, RestSources.Resolver> restSources() {
        return Map.of();
    }

    /**
     * Memories holding progress that only makes sense while the work is available; the mind erases them whenever the
     * pet's job no longer offers it or its directive no longer permits it.
     */
    default Set<MemoryModuleType<?>> forgetWhenUnavailable() {
        return Set.of();
    }
}
