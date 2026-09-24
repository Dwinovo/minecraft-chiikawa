package com.dwinovo.chiikawa.anim.render.layer;

import com.dwinovo.chiikawa.anim.render.ChiikawaRenderState;
import com.dwinovo.chiikawa.anim.render.PetData;
import com.dwinovo.chiikawa.anim.render.PropRenderer;
import com.dwinovo.chiikawa.item.BagItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Hangs the worn bag on the pet, so an owner can tell at a glance which of their pets can
 * carry the shopping.
 *
 * <p>The bag is drawn from its own model at the bone its {@link BagItem.Wear} names — a
 * pouch at the hip, a backpack on the back — and that bone is part of the pet's model,
 * placed and turned by hand to fit that pet's body, and moving with it. The strap is part
 * of the pet's model too, shown by the renderer's visibility rule for the same wear.
 */
public final class BagLayer implements RenderLayer {
    private final BoneTransformWalker walker = new BoneTransformWalker();

    /** How the bag in this snapshot is worn, or {@code null} if the pet wears none. */
    public static BagItem.Wear wearOf(ChiikawaRenderState state) {
        ItemStack bag = state.get(PetData.WORN_BAG);
        return bag != null && bag.getItem() instanceof BagItem item ? item.wear() : null;
    }

    @Override
    public void submit(RenderLayerContext ctx) {
        BagItem.Wear wear = wearOf(ctx.state());
        if (wear == null) {
            return;
        }
        Integer locator = ctx.model().boneIndex.get(wear.locator());
        if (locator == null || ctx.isHidden(locator)) {
            return;
        }
        ItemStack bag = ctx.state().get(PetData.WORN_BAG);
        ctx.poseStack().pushPose();
        walker.transformToBone(ctx.model(), ctx.poseBuf(), locator, ctx.poseStack());
        PropRenderer.draw(BuiltInRegistries.ITEM.getKey(bag.getItem()),
            ctx.poseStack(), ctx.collector(), ctx.packedLight(), ctx.packedOverlay());
        ctx.poseStack().popPose();
    }
}
