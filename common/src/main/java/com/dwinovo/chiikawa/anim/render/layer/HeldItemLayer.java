package com.dwinovo.chiikawa.anim.render.layer;

import com.dwinovo.chiikawa.anim.render.PetData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * {@link RenderLayer} that submits the entity's mainhand {@link ItemStack} at
 * a named bone's pivot.
 *
 * <h2>Convention</h2>
 * The target bone is an empty locator on every pet model (default
 * {@code RightHandLocator}, configurable via the constructor). This mirrors
 * GeckoLib's {@code RightHandItem} convention for hand attachments — the
 * model team K-frames this bone in animations to drive how the held item
 * moves with the body.
 *
 * <h2>Unit conversion</h2>
 * The renderer's PoseStack is in 1/16-scaled pixel space (see
 * {@link com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer#submit}).
 * Item models render in <b>block</b> units, so a compensating {@code scale(16)}
 * is applied before submission. Without it the held item would render at 1/16
 * of its intended size.
 *
 * <h2>Into the fist</h2>
 * An item's third-person display transforms are written for vanilla's hand, which
 * {@code ItemInHandLayer} turns so that the item's up runs out of the front of the fist.
 * The locator's frame is the model's own — up is up, the pet faces {@code -Z} — so the
 * item is turned the same way before it is drawn; see {@link #intoFist}. Without that
 * turn a sword stood on its pommel pointing at the sky, and every swing of the arm swung
 * its blade backwards.
 */
public final class HeldItemLayer implements RenderLayer {

    /** Default locator bone that all chiikawa pet models are expected to expose. */
    public static final String DEFAULT_BONE = "RightHandLocator";

    private final String boneName;
    private final BoneTransformWalker walker = new BoneTransformWalker();

    public HeldItemLayer() {
        this(DEFAULT_BONE);
    }

    public HeldItemLayer(String boneName) {
        this.boneName = boneName;
    }

    @Override
    public void submit(RenderLayerContext ctx) {
        ItemStack stack = ctx.state().get(PetData.HELD_ITEM_STACK);
        if (stack == null || stack.isEmpty()) return;
        Integer targetIdx = ctx.model().boneIndex.get(boneName);
        if (targetIdx == null) return;
        // If the hand is hidden, so is what it holds.
        if (ctx.isHidden(targetIdx)) return;

        // Resolve the item model fresh per submit. The state is small and
        // short-lived so the allocation is cheaper than caching bookkeeping.
        Minecraft mc = Minecraft.getInstance();
        ItemStackRenderState itemRenderState = new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(
                itemRenderState,
                stack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                mc.level,
                null,
                0);
        if (itemRenderState.isEmpty()) return;

        ctx.poseStack().pushPose();
        walker.transformToBone(ctx.model(), ctx.poseBuf(), targetIdx, ctx.poseStack());
        // Cancel the entity-level scale(1/16): items expect block-unit space.
        ctx.poseStack().scale(16f, 16f, 16f);
        intoFist(ctx.poseStack());
        itemRenderState.submit(ctx.poseStack(), ctx.collector(), ctx.packedLight(),
                OverlayTexture.NO_OVERLAY, 0);
        ctx.poseStack().popPose();
    }

    /**
     * From the hand locator, in block units, to where vanilla holds an item: a quarter
     * turn about X so the item's up points out ahead of the fist, then a pixel down into
     * the palm and two ahead of the knuckles.
     *
     * <p>This is what vanilla's {@code ItemInHandLayer} and Touhou Little Maid's held-item
     * layer do — {@code X -90°, Y 180°} and then {@code (0, 0.125, -0.0625)} from a hand
     * bone — written for a frame that is vanilla's model space flipped by
     * {@code scale(-1, -1, 1)}. That flip is a half turn about Z, and a half turn about Z
     * followed by theirs comes to the single quarter turn here.
     */
    static void intoFist(PoseStack pose) {
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        pose.translate(0.0F, 0.125F, -0.0625F);
    }
}
