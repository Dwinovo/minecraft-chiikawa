package com.dwinovo.chiikawa.anim.render.layer;

import com.dwinovo.chiikawa.anim.render.PetData;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Hangs a little wooden tag on the chest of a pet that carries a slip, so it is plain to
 * see who is working.
 *
 * <p>Placed against the body rather than on a bone: every pet model names its bones a
 * little differently, and the shoulder locators sit on top of the head. The pose is the
 * entity's own frame in model pixels, where the pets face {@code -Z}.
 */
public final class SlipTagLayer implements RenderLayer {
    /** Chest height in model pixels, low enough to clear the big heads. */
    private static final float HEIGHT = 4.5F;
    /** In front of the body, just clear of it. */
    private static final float FRONT = -4.8F;
    private static final float SIZE = 0.3F;
    private static final ItemStack TAG = new ItemStack(Items.OAK_HANGING_SIGN);

    @Override
    public void submit(RenderLayerContext ctx) {
        if (!Boolean.TRUE.equals(ctx.state().get(PetData.CARRYING_SLIP))) {
            return;
        }
        ctx.poseStack().pushPose();
        ctx.poseStack().translate(0.0F, HEIGHT, FRONT);
        // Turn the flat item around so it faces the way the pet does.
        ctx.poseStack().mulPose(Axis.YP.rotationDegrees(180.0F));
        // Cancel the entity-level scale(1/16): items expect block-unit space.
        ctx.poseStack().scale(16f * SIZE, 16f * SIZE, 16f * SIZE);
        Minecraft mc = Minecraft.getInstance();
        mc.getItemRenderer().renderStatic(
                TAG,
                ItemDisplayContext.FIXED,
                ctx.packedLight(),
                OverlayTexture.NO_OVERLAY,
                ctx.poseStack(),
                ctx.bufferSource(),
                mc.level,
                0);
        ctx.poseStack().popPose();
    }
}
