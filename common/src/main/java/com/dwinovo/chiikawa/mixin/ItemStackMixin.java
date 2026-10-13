package com.dwinovo.chiikawa.mixin;

import com.dwinovo.chiikawa.item.FilmItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A roll of film with a shot on it is one to a stack, whatever the film it was made from
 * stacks to. Later versions let a stack say how many it holds; 1.20.1 has only the item's
 * own number, so the roll says it here.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void chiikawa$aRollIsOneToAStack(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() instanceof FilmItem && FilmItem.rollOf(stack).started()) {
            cir.setReturnValue(1);
        }
    }
}
