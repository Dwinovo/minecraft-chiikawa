package com.dwinovo.chiikawa.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

/**
 * Rakko's sword, its hilt carved with a face as the top four of the subjugation ranking
 * have theirs. The best blade among the pets', a diamond sword's worth, as the top ranker's
 * should be, over Usagi's iron stick; nobody makes one and no shop sells one, so it comes
 * only with a wild Rakko.
 */
public class RakkoSword extends SwordItem {
    public RakkoSword() {
        super(Tiers.DIAMOND, 3, -2.4F, new Item.Properties());
    }
}
