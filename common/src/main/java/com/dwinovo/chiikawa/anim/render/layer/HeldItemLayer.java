package com.dwinovo.chiikawa.anim.render.layer;

import com.dwinovo.chiikawa.anim.render.PetData;
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
 * {@link com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer#render}).
 * Item models render in <b>block</b> units, so a compensating {@code scale(16)}
 * is applied before rendering. Without it the held item would render at 1/16
 * of its intended size.
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
        // If the target bone — or any ancestor in its chain — is hidden by a
        // visibility rule, the held item disappears too. Otherwise the prop
        // would float in space at the unrendered hand's pivot.
        if (isAnyAncestorHidden(ctx, targetIdx)) return;

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
        itemRenderState.render(ctx.poseStack(), ctx.bufferSource(), ctx.packedLight(),
                OverlayTexture.NO_OVERLAY);
        ctx.poseStack().popPose();
    }

    /**
     * Walks up from {@code boneIdx} to a root, returning {@code true} if any
     * bone in the chain is flagged hidden. Empty/null hidden array short-
     * circuits to {@code false} (common case: no visibility rules registered).
     */
    private static boolean isAnyAncestorHidden(RenderLayerContext ctx, int boneIdx) {
        boolean[] hidden = ctx.state().hiddenBones;
        if (hidden == null || hidden.length != ctx.model().bones.length) return false;
        int idx = boneIdx;
        while (idx >= 0) {
            if (hidden[idx]) return true;
            idx = ctx.model().bones[idx].parentIdx;
        }
        return false;
    }
}
