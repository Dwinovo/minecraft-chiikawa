package com.dwinovo.chiikawa.anim.render.layer;

import com.dwinovo.chiikawa.anim.render.PetData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Hangs a little wooden tag on a pet that carries a slip, so it is plain to see who is
 * working. Rendered at the shoulder locator like {@link HeldItemLayer} renders the held
 * item, only smaller and flat against the body.
 */
public final class SlipTagLayer implements RenderLayer {
    private static final String BONE = "LeftShoulderLocator";
    private static final ItemStack TAG = new ItemStack(Items.OAK_HANGING_SIGN);
    private static final float SIZE = 0.45F;

    private final BoneTransformWalker walker = new BoneTransformWalker();

    @Override
    public void submit(RenderLayerContext ctx) {
        if (!Boolean.TRUE.equals(ctx.state().get(PetData.CARRYING_SLIP))) {
            return;
        }
        Integer targetIdx = ctx.model().boneIndex.get(BONE);
        if (targetIdx == null) {
            return;
        }
        ctx.poseStack().pushPose();
        walker.transformToBone(ctx.model(), ctx.poseBuf(), targetIdx, ctx.poseStack());
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
