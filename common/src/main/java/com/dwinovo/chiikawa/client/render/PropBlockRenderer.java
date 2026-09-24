package com.dwinovo.chiikawa.client.render;

import com.dwinovo.chiikawa.anim.render.PropRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Predicate;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a block from a Bedrock model of its own, named after the block, turned the way the
 * block faces. The model stands on the middle of the block's floor and faces north, like the
 * block models of vanilla's facing blocks; a block that shows only some of its bones, as the
 * labor board shows only the plates still hanging, says which.
 */
public class PropBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, PropBlockRenderer.State> {
    private static final float PIXEL = 1.0F / 16.0F;

    /** What is drawn of the block this frame, taken down while the block is at hand. */
    public static class State extends BlockEntityRenderState {
        Identifier model;
        Direction facing = Direction.NORTH;
        Predicate<String> shown = bone -> true;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T block, State state, float partialTick, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(block, state, partialTick, cameraPosition, breakProgress);
        BlockState blockState = block.getBlockState();
        state.model = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        state.facing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        state.shown = shown(block);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - state.facing.toYRot()));
        pose.scale(PIXEL, PIXEL, PIXEL);
        PropRenderer.draw(state.model, pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.shown,
            state.breakProgress);
        pose.popPose();
    }

    /**
     * Which of the model's bones to draw: all of them, unless the block says otherwise.
     * Asked while the render state is taken, so the answer is what the block is now.
     */
    protected Predicate<String> shown(T block) {
        return bone -> true;
    }
}
