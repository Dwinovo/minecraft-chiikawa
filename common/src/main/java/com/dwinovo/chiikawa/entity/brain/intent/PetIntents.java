package com.dwinovo.chiikawa.entity.brain.intent;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.intent.impl.CombatIntent;
import com.dwinovo.chiikawa.entity.brain.intent.impl.ConstantIntent;
import com.dwinovo.chiikawa.entity.brain.intent.impl.FollowOwnerIntent;
import com.dwinovo.chiikawa.entity.brain.intent.impl.PlayMusicIntent;
import com.dwinovo.chiikawa.entity.brain.intent.impl.TargetIntent;
import com.dwinovo.chiikawa.init.InitActivity;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.schedule.Activity;
import org.jetbrains.annotations.Nullable;

/**
 * Registry of every pet intent. Generic intents are open to every pet; the others
 * are offered by capabilities through {@code PetCapability#intents()}.
 *
 * <p>Priorities that 0.0.9 hard-coded as if-chains are expressed by base scores:
 * harvest (0.6) over plant (0.55) over deliver (0.5), all work and fights over
 * picking up items (0.4), everything over wandering (0.05).
 */
public final class PetIntents {
    public static final ResourceLocation FOLLOW_OWNER = id("follow_owner");
    public static final ResourceLocation STAY = id("stay");
    public static final ResourceLocation WANDER = id("wander");
    public static final ResourceLocation PICK_UP_ITEM = id("pick_up_item");
    public static final ResourceLocation HARVEST = id("harvest");
    public static final ResourceLocation PLANT = id("plant");
    public static final ResourceLocation DELIVER = id("deliver");
    public static final ResourceLocation MELEE = id("melee");
    public static final ResourceLocation RANGED = id("ranged");
    public static final ResourceLocation PLAY_MUSIC = id("play_music");

    private static final Map<ResourceLocation, PetIntent> BY_ID = new LinkedHashMap<>();

    /** Intents every pet considers, whatever its capability. */
    public static final List<PetIntent> GENERIC = List.of(
        register(new FollowOwnerIntent()),
        register(new ConstantIntent(STAY, IntentCategory.STAY, () -> InitActivity.STAY.get(), 1.0F)),
        register(new ConstantIntent(WANDER, IntentCategory.WANDER, () -> Activity.IDLE, 0.05F)),
        register(new TargetIntent(PICK_UP_ITEM, IntentCategory.PICK_UP, () -> InitActivity.PICK_UP.get(),
            PerceivedTargets::pickableItem, 0.4F, "no_item"))
    );

    static {
        register(new TargetIntent(HARVEST, IntentCategory.WORK, () -> InitActivity.FARMER_HARVEST.get(),
            PerceivedTargets::harvest, 0.6F, "no_crop"));
        register(new TargetIntent(PLANT, IntentCategory.WORK, () -> InitActivity.FARMER_PLANT.get(),
            PerceivedTargets::plant, 0.55F, "no_farmland"));
        register(new TargetIntent(DELIVER, IntentCategory.WORK, () -> InitActivity.DELEVER.get(),
            PerceivedTargets::container, 0.5F, "no_container"));
        register(new CombatIntent(MELEE, () -> InitActivity.FENCER_FIGHT.get(), false));
        register(new CombatIntent(RANGED, () -> InitActivity.ARCHER_SHOOT.get(), true));
        register(new PlayMusicIntent());
    }

    private PetIntents() {
    }

    public static @Nullable PetIntent get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static Collection<PetIntent> all() {
        return BY_ID.values();
    }

    private static PetIntent register(PetIntent intent) {
        if (BY_ID.putIfAbsent(intent.id(), intent) != null) {
            throw new IllegalStateException("Duplicate pet intent " + intent.id());
        }
        return intent;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
