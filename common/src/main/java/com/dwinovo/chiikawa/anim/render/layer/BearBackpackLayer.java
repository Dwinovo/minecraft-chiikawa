package com.dwinovo.chiikawa.anim.render.layer;

import com.dwinovo.chiikawa.anim.render.PetData;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Puts the bear backpack on the pet's back, so an owner can tell at a glance which of
 * their pets can carry the shopping.
 *
 * <p>Placed against the body rather than on a bone, for the same reason the slip's tag is:
 * every pet model names its bones a little differently. The pose is the entity's own frame
 * in model pixels, where the pets face {@code -Z}.
 */
public final class BearBackpackLayer implements RenderLayer {
    /** Up the back, below the head. */
    private static final float HEIGHT = 5.0F;
    /** Behind the body, just clear of it. */
    private static final float BACK = 4.6F;
    private static final float SIZE = 0.45F;

    @Override
    public void submit(RenderLayerContext ctx) {
        ItemStack bag = ctx.state().get(PetData.WORN_BAG);
        if (bag == null || bag.isEmpty()) {
            return;
        }
        ctx.poseStack().pushPose();
        ctx.poseStack().translate(0.0F, HEIGHT, BACK);
        // The flat item faces the way the pet does, so its front shows on the pet's back.
        ctx.poseStack().mulPose(Axis.YP.rotationDegrees(180.0F));
        // Cancel the entity-level scale(1/16): items expect block-unit space.
        ctx.poseStack().scale(16f * SIZE, 16f * SIZE, 16f * SIZE);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderStatic(
            bag,
            ItemDisplayContext.FIXED,
            ctx.packedLight(),
            OverlayTexture.NO_OVERLAY,
            ctx.poseStack(),
            ctx.bufferSource(),
            minecraft.level,
            0);
        ctx.poseStack().popPose();
    }
}
