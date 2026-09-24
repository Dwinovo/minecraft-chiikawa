package com.dwinovo.chiikawa.client.render;

import com.dwinovo.chiikawa.anim.render.PropRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Predicate;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a block from a Bedrock model of its own, named after the block, turned the way the
 * block faces. The model stands on the middle of the block's floor and faces north, like the
 * block models of vanilla's facing blocks; a block that shows only some of its bones, as the
 * labor board shows only the plates still hanging, says which.
 */
public class PropBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final float PIXEL = 1.0F / 16.0F;

    @Override
    public void render(T block, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
            Vec3 cameraPos) {
        BlockState state = block.getBlockState();
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()));
        pose.scale(PIXEL, PIXEL, PIXEL);
        PropRenderer.draw(BuiltInRegistries.BLOCK.getKey(state.getBlock()), pose, buffers, light, overlay, shown(block));
        pose.popPose();
    }

    /** Which of the model's bones to draw: all of them, unless the block says otherwise. */
    protected Predicate<String> shown(T block) {
        return bone -> true;
    }
}
