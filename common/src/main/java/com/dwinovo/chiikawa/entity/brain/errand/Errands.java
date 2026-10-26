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
import com.dwinovo.chiikawa.entity.brain.errand.impl.ShopErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.SocializeErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.StayErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.TakeExamErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.TakePhotoErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.TakeTaskErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.WanderErrand;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkErrand;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.Brain;
import org.jetbrains.annotations.Nullable;

/**
 * Registry of every errand a pet can be on, by its id: the code errands, which are fixed, and the work errands the
 * loaded errand files make, see {@link WorkErrand}, which are installed together with the settings of every errand
 * and replaced with them by a reload. Generic errands are open to every pet; the others are offered by a job through
 * its work, see {@link com.dwinovo.chiikawa.entity.job.JobWork}.
 *
 * <p>How much each matters is not decided here but by the errand's {@link ErrandSettings}: its
 * priority, which a pet gives way to only for a higher one, its score among the errands of its
 * priority, and the errands that must have nothing to do before it is started, such as the farmer's
 * strict order of harvest before plant before deliver. Work the pet's slip counts is raised by
 * {@code Decision.TASK_BONUS}, and a pet's personality weighs them all.
 */
public final class Errands {
    private static final Map<ResourceLocation, ErrandType<?, ?>> CODE = new LinkedHashMap<>();

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
    }

    private static volatile Map<ResourceLocation, WorkErrand<?, ?>> works = Map.of();
    private static volatile List<ErrandType<?, ?>> all = List.copyOf(CODE.values());

    private Errands() {
    }

    /** @return the errand with this id: a code errand, or else the work errand a loaded file made */
    public static @Nullable ErrandType<?, ?> get(ResourceLocation id) {
        ErrandType<?, ?> code = CODE.get(id);
        return code != null ? code : works.get(id);
    }

    /** @return whether this id is one of the errands written in code, which no errand file can make a work of */
    public static boolean isCode(ResourceLocation id) {
        return CODE.containsKey(id);
    }

    /**
     * Makes these the work errands and their settings the errands go by, as a data pack reload does. Public so a test
     * can have what the mod ships, see {@code ErrandSettingsLoader}.
     *
     * @param works the work errands by id
     * @param settings the settings of every errand, work or code, by id
     */
    public static void install(Map<ResourceLocation, WorkErrand<?, ?>> works, Map<ResourceLocation, ErrandSettings<?>> settings) {
        List<ErrandType<?, ?>> everyErrand = new ArrayList<>(CODE.values());
        everyErrand.addAll(works.values());
        all = List.copyOf(everyErrand);
        Errands.works = Map.copyOf(works);
        ErrandSettings.replaceAll(settings);
    }

    /**
     * @return where the brain keeps this errand's rests: none, for an errand that is no more, as one a data pack
     *         reload took away from a pet that was on it
     */
    public static RestStore restsOf(Brain<?> brain, ResourceLocation id) {
        ErrandType<?, ?> type = get(id);
        return type == null ? RestStore.NONE : type.restStore().of(brain, id);
    }

    /** @return every errand: the code errands, the generic ones first, then the work errands */
    public static Collection<ErrandType<?, ?>> all() {
        return all;
    }

    /** @return the errands every pet considers */
    public static List<ErrandType<?, ?>> generic() {
        return GENERIC;
    }

    private static <E extends ErrandType<?, ?>> E register(E errand) {
        if (CODE.putIfAbsent(errand.id(), errand) != null) {
            throw new IllegalStateException("Duplicate errand " + errand.id());
        }
        errand.restSources().forEach(RestSources::register);
        return errand;
    }
}
