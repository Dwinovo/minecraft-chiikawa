package com.dwinovo.chiikawa.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ToolMaterial;

/**
 * Rakko's sword, its hilt carved with a face as the top four of the subjugation ranking
 * have theirs. The best blade among the pets', a diamond sword's worth, as the top ranker's
 * should be, over Usagi's iron stick; nobody makes one and no shop sells one, so it comes
 * only with a wild Rakko.
 */
public class RakkoSword extends SwordItem {
    public RakkoSword(Item.Properties properties) {
        super(ToolMaterial.DIAMOND, 3.0F, -2.4F, properties);
    }
}
