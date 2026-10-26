package com.dwinovo.chiikawa.entity.brain.errand;

import com.dwinovo.chiikawa.entity.brain.errand.impl.AnswerWhistleErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.BlockWorkErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.CheckResultsErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.CombatErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.CooperateErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.DeliverErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.FollowOwnerErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.GiftOwnerErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.PickUpItemErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.PlayMusicErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.ShopErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.SocializeErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.StayErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.TakeExamErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.TakePhotoErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.TakeTaskErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.WanderErrand;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.Brain;
import org.jetbrains.annotations.Nullable;

/**
 * Registry of every errand a pet can be on, by its id. Generic errands
 * are open to every pet; the others are offered by capabilities through
 * {@code PetCapability#errands()}.
 *
 * <p>How much each matters is not decided here but by the errand's {@link ErrandSettings}: its
 * priority, which a pet gives way to only for a higher one, its score among the errands of its
 * priority, and the errands that must have nothing to do before it is started, such as the farmer's
 * strict order of harvest before plant before deliver. Work the pet's slip counts is raised by
 * {@code Decision.TASK_BONUS}, and a pet's personality weighs them all.
 */
public final class Errands {
    private static final Map<ResourceLocation, ErrandType<?, ?>> BY_ID = new LinkedHashMap<>();

    /** Errands every pet considers, whatever its capability. */
    private static final List<ErrandType<?, ?>> GENERIC = List.of(
        register(new FollowOwnerErrand()),
        register(new StayErrand()),
        register(new WanderErrand()),
        register(new PickUpItemErrand()),
        register(new TakeTaskErrand()),
        register(new ShopErrand()),
        register(new GiftOwnerErrand()),
        register(new SocializeErrand()),
        register(new CooperateErrand()),
        register(new TakeExamErrand()),
        register(new CheckResultsErrand()),
        register(new AnswerWhistleErrand()),
        register(new TakePhotoErrand())
    );

    static {
        register(BlockWorkErrand.harvesting());
        register(BlockWorkErrand.planting());
        register(new DeliverErrand());
        register(BlockWorkErrand.weeding());
        register(BlockWorkErrand.pickingMushrooms());
        register(CombatErrand.fencing());
        register(CombatErrand.shooting());
        register(new PlayMusicErrand());
    }

    private Errands() {
    }

    public static @Nullable ErrandType<?, ?> get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    /**
     * @return where the brain keeps this errand's rests: none, for an errand that is no more, as one a data pack
     *         reload took away from a pet that was on it
     */
    public static RestStore restsOf(Brain<?> brain, ResourceLocation id) {
        ErrandType<?, ?> type = get(id);
        return type == null ? RestStore.NONE : type.restStore().of(brain, id);
    }

    /** @return every errand, the generic ones first */
    public static Collection<ErrandType<?, ?>> all() {
        return BY_ID.values();
    }

    /** @return the errands every pet considers */
    public static List<ErrandType<?, ?>> generic() {
        return GENERIC;
    }

    private static <E extends ErrandType<?, ?>> E register(E errand) {
        if (BY_ID.putIfAbsent(errand.id(), errand) != null) {
            throw new IllegalStateException("Duplicate errand " + errand.id());
        }
        errand.restSources().forEach(RestSources::register);
        return errand;
    }
}
