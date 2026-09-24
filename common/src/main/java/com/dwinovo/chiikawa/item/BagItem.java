package com.dwinovo.chiikawa.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A bag a pet wears: ten more slots, whichever bag it is. Two kinds, worn two ways. The
 * grey backpack is the one everybody carries out to work and to a hunt in the anime; the
 * pouches are the three friends' own — Chiikawa's pink bear, Hachiware's blue whale,
 * Usagi's yellow star — slung across the body on a white strap. Any pet may wear any.
 *
 * <p>Each is drawn from a Bedrock model of the same name as the item, in a hand and on a
 * pet alike; on a pet it hangs from the bones its {@link Wear} names, which every pet's
 * own model places to fit its body.
 */
public class BagItem extends Item {
    private final Wear wear;

    public BagItem(Properties properties, Wear wear) {
        super(properties.stacksTo(1));
        this.wear = wear;
    }

    /** Whether this is something a pet can wear as its bag. */
    public static boolean isBag(ItemStack stack) {
        return stack.getItem() instanceof BagItem;
    }

    public Wear wear() {
        return wear;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.chiikawa.bag").withStyle(ChatFormatting.GRAY));
    }

    /**
     * How a bag is worn, and so which two bones of a pet's model it needs: one the bag
     * hangs from, and the strap that holds it there, shown only while such a bag is worn.
     */
    public enum Wear {
        /** Across the body, the bag at the hip. */
        SLUNG("PouchLocator", "PouchStrap"),
        /** On the back, a strap over each shoulder. */
        ON_BACK("PackLocator", "PackStrap");

        private final String locator;
        private final String strap;

        Wear(String locator, String strap) {
            this.locator = locator;
            this.strap = strap;
        }

        /** The bone the bag's own model is drawn at, turned the way it should face. */
        public String locator() {
            return locator;
        }

        /** The bone holding the strap, part of the pet's own model. */
        public String strap() {
            return strap;
        }
    }
}
