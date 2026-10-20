package com.dwinovo.chiikawa.entity.brain.errand;

import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * One thing a pet may decide to do. An errand type is stateless: what it offers and whether it is
 * still wanted read only the {@link PetView}, and what a pet has set out to do is its {@link Plan}.
 * How much it matters, and what it is worth to the pet, is data: {@link ErrandSettings}, whose
 * {@code tuning} block this type gives the shape of.
 *
 * <p>The decision filters errands by the directive's permission table before asking what they
 * offer, and the executor runs the {@link #routine} of the one it starts.
 *
 * @param <P> the shape of its plans
 * @param <T> the shape of its tuning
 */
public interface ErrandType<P extends Plan, T> {
    ResourceLocation id();

    ErrandCategory category();

    /** @return the codec of the {@code tuning} block of the errand's data file */
    Codec<T> tuningCodec();

    /**
     * The ways the pet could set out on this errand now; none when it could not. Pure: reads only
     * the view, draws nothing from a random and writes nothing, and answers the same when asked
     * twice, since it is asked of errands that are only being looked at.
     */
    List<Offer<P>> offers(PetView view, T tuning);

    /**
     * @return why {@link #offers} gives none, as a failed check with a translation key, for the debug
     *         command. Asked only then, and pure like {@link #offers}
     */
    Check whyNoOffer(PetView view, T tuning);

    /**
     * How much the pet fancies this among the errands of its priority, from 0 to 1, before its
     * personality weighs it: the score in the errand's settings, unless the errand is worth more or
     * less as the pet's situation changes. Offers of one errand are not told apart by it; the weights do that.
     */
    default float score(P plan, PetView view, ErrandSettings<T> settings) {
        return settings.score();
    }

    /**
     * Whether the errand goes on: the condition to keep at it, which may be looser than the one to
     * start. Pure. The area, the directive and the capability are checked by the mind, whatever the errand.
     */
    default Check stillWanted(P plan, PetView view, T tuning) {
        return Check.OK;
    }

    /** @return the steps of the errand, made for one start and used up by it */
    Routine routine(P plan, ErrandSettings<T> settings);

    /**
     * The work counter this errand's work reports, see {@link com.dwinovo.chiikawa.task.PetWorkCounters}.
     * A pet carrying a slip that counts it prefers this errand.
     */
    default Optional<ResourceLocation> workCounter() {
        return Optional.empty();
    }

    /**
     * Memories holding this errand's progress that only make sense while the errand is available; the
     * mind erases them whenever the pet's capability no longer offers it or its directive no longer
     * permits it.
     */
    default Set<MemoryModuleType<?>> forgetWhenUnavailable() {
        return Set.of();
    }

    /** @return where this errand's rests are kept, see {@link RestStore.Kind}; a declaration, bound to a pet by the ledger */
    RestStore.Kind restStore();
}
