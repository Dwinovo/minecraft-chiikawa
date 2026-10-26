package com.dwinovo.chiikawa.entity.brain.errand.work;

import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.work.action.BuskAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.action.DepositAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.action.HarvestBlockAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.action.PlantBlockAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.action.ShootAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.action.StrikeAction;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Collection;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * The actions an errand file can name, by id: {@code "action": "chiikawa:busk"}. The names are fixed here and checked
 * when an errand file is loaded: a file naming one that is not on this list is turned off. The actions register what
 * their rests point at once, as this class loads, so that a reload does not do it again.
 */
public final class WorkActions {
    /** Playing the song on the held music box. */
    public static final BuskAction BUSK = new BuskAction();

    /** Fighting the foe with a sword. */
    public static final StrikeAction STRIKE = new StrikeAction();
    /** Fighting the foe with a bow. */
    public static final ShootAction SHOOT = new ShootAction();

    /** Breaking a ripe crop, a weed or a mushroom into the bag. */
    public static final HarvestBlockAction HARVEST_BLOCK = new HarvestBlockAction();
    /** Planting a seed from the bag in bare soil. */
    public static final PlantBlockAction PLANT_BLOCK = new PlantBlockAction();
    /** Putting what the pet has to deliver in a container. */
    public static final DepositAction DEPOSIT = new DepositAction();

    private static final Map<ResourceLocation, WorkAction<?, ?>> BY_ID = Map.of(
        BuskAction.ID, BUSK, StrikeAction.ID, STRIKE, ShootAction.ID, SHOOT, HarvestBlockAction.ID, HARVEST_BLOCK,
        PlantBlockAction.ID, PLANT_BLOCK, DepositAction.ID, DEPOSIT);

    /** Reads and writes an action as its id; an id no action has does not read. */
    public static final Codec<WorkAction<?, ?>> CODEC = ResourceLocation.CODEC.comapFlatMap(id -> {
        WorkAction<?, ?> action = BY_ID.get(id);
        return action == null ? DataResult.error(() -> id + " is no work action") : DataResult.success(action);
    }, WorkAction::id);

    static {
        BY_ID.values().forEach(action -> action.restSources().forEach(RestSources::register));
    }

    private WorkActions() {
    }

    /** @return every action */
    public static Collection<WorkAction<?, ?>> all() {
        return BY_ID.values();
    }
}
