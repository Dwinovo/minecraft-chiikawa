package com.dwinovo.chiikawa.testing;

import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.errand.view.AttackTarget;
import com.dwinovo.chiikawa.entity.brain.errand.view.PerceivedTargets;
import com.dwinovo.chiikawa.entity.brain.errand.view.PickableItem;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * What a pet perceives, built by hand for tests: the farmer's blocks of work are given by the work they are for, in the
 * order the shipped farmer lists them, so that a test says what the pet sees without the map it is kept in.
 */
public final class TestTargets {
    private TestTargets() {
    }

    /**
     * @return what a pet perceives: a foe, and for each work of the farmer a block it sees or none, then an item
     */
    public static PerceivedTargets of(Optional<AttackTarget> attack, Optional<GlobalPos> harvest, Optional<GlobalPos> plant,
            Optional<GlobalPos> container, Optional<GlobalPos> weed, Optional<GlobalPos> mushroom, Optional<PickableItem> item) {
        Map<ResourceLocation, GlobalPos> work = new HashMap<>();
        harvest.ifPresent(where -> work.put(WorkData.HARVEST, where));
        plant.ifPresent(where -> work.put(WorkData.PLANT, where));
        container.ifPresent(where -> work.put(WorkData.DELIVER, where));
        weed.ifPresent(where -> work.put(WorkData.WEED, where));
        mushroom.ifPresent(where -> work.put(WorkData.PICK_MUSHROOM, where));
        return new PerceivedTargets(attack, work, item);
    }
}
