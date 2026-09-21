package com.dwinovo.chiikawa.client.render;

import com.dwinovo.chiikawa.anim.render.PropRenderer;
import com.dwinovo.chiikawa.block.LaborBoardBlock;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a labor board from its own model, with a plate on each hook whose slip is still
 * waiting: the plates a pet takes down come down on the board too, so how much work is
 * left shows from across the garden. Each plate is a bone of the model, {@code Slip0} on.
 */
public final class LaborBoardRenderer implements BlockEntityRenderer<LaborBoardBlockEntity> {
    /** What the plate bones are called, before the place they hang at. */
    public static final String PLATE_BONE = "Slip";
    private static final float PIXEL = 1.0F / 16.0F;

    @Override
    public void render(LaborBoardBlockEntity board, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        ResourceLocation model = BuiltInRegistries.BLOCK.getKey(board.getBlockState().getBlock());
        int hanging = board.hanging();
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        // The model faces north; the other ways round turn it about the middle of the block.
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - board.getBlockState().getValue(LaborBoardBlock.FACING).toYRot()));
        pose.scale(PIXEL, PIXEL, PIXEL);
        PropRenderer.draw(model, pose, buffers, light, overlay, bone -> {
            if (!bone.startsWith(PLATE_BONE)) {
                return true;
            }
            int place = Integer.parseInt(bone.substring(PLATE_BONE.length()));
            return (hanging & 1 << place) != 0;
        });
        pose.popPose();
    }
}
