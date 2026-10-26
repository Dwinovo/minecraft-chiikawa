package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkActions;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkRequirement;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget;
import com.dwinovo.chiikawa.init.InitTag;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The generated works, by the id of the errand each is: what the pet does, what kind of errand it is, what it
 * counts for a slip and what it needs. Written beside the errand's settings, see {@link ErrandFiles}. The ids of
 * the works are for this generator and for tests: nothing in the game names a work, it has what its file says.
 */
public final class WorkData {
    /** Reaping the ripe crop. */
    public static final ResourceLocation HARVEST = id("harvest");
    /** Planting a seed in bare soil. */
    public static final ResourceLocation PLANT = id("plant");
    /** Putting the crop away in a container. */
    public static final ResourceLocation DELIVER = id("deliver");
    /** Pulling up a wild plant. */
    public static final ResourceLocation WEED = id("weed");
    /** Picking a mushroom. */
    public static final ResourceLocation PICK_MUSHROOM = id("pick_mushroom");
    /** Fighting the foe with a sword. */
    public static final ResourceLocation MELEE = id("melee");
    /** Fighting the foe with a bow. */
    public static final ResourceLocation RANGED = id("ranged");
    /** Playing the song on the held music box. */
    public static final ResourceLocation PLAY_MUSIC = id("play_music");
    /** One second of a performance, which a street performance slip counts. */
    public static final ResourceLocation PLAY_MUSIC_SECOND = id("play_music_second");
    /** One weed pulled up. */
    public static final ResourceLocation WEED_PULLED = id("weed");
    /** One mushroom picked. */
    public static final ResourceLocation MUSHROOM_PICKED = id("pick_mushroom");

    private WorkData() {
    }

    /** @return works by errand id */
    public static Map<ResourceLocation, Work<?, ?>> all() {
        // Fights name no counter: the slay is reported by the death hook, and one here would give them the slip bonus.
        Optional<WorkTarget> foe = Optional.of(new WorkTarget.Foe("no_attack_target"));
        return Map.of(
            HARVEST, new Work<>(WorkActions.HARVEST_BLOCK, ErrandCategory.WORK, Optional.empty(), List.of(),
                Optional.of(new WorkTarget.RipeCrop("no_crop"))),
            PLANT, new Work<>(WorkActions.PLANT_BLOCK, ErrandCategory.WORK, Optional.empty(), List.of(),
                Optional.of(new WorkTarget.PlantableSoil("no_farmland"))),
            DELIVER, new Work<>(WorkActions.DEPOSIT, ErrandCategory.WORK, Optional.empty(), List.of(),
                Optional.of(new WorkTarget.Container(InitTag.ENTITY_DELEVER_CONTAINER, InitTag.ENTITY_DELIVER_ITEMS,
                    "no_container"))),
            // A wild pet forages on its own; an owned one only on a slip that counts it, since on its own it would
            // strip the grass and mushrooms its owner keeps around the base.
            WEED, new Work<>(WorkActions.HARVEST_BLOCK, ErrandCategory.FORAGE, Optional.of(WEED_PULLED),
                List.of(WorkRequirement.WILD_OR_SLIP), Optional.of(new WorkTarget.Blocks(InitTag.ENTITY_WEEDS, "no_weed"))),
            PICK_MUSHROOM, new Work<>(WorkActions.HARVEST_BLOCK, ErrandCategory.FORAGE, Optional.of(MUSHROOM_PICKED),
                List.of(WorkRequirement.WILD_OR_SLIP, WorkRequirement.AT_NIGHT),
                Optional.of(new WorkTarget.Blocks(InitTag.ENTITY_MUSHROOMS, "no_mushroom"))),
            MELEE, new Work<>(WorkActions.STRIKE, ErrandCategory.COMBAT, Optional.empty(), List.of(), foe),
            RANGED, new Work<>(WorkActions.SHOOT, ErrandCategory.COMBAT, Optional.empty(), List.of(), foe),
            PLAY_MUSIC, new Work<>(WorkActions.BUSK, ErrandCategory.WORK, Optional.of(PLAY_MUSIC_SECOND), List.of(),
                Optional.empty()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
