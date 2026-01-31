package com.dwinovo.chiikawa.client.render.layer;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.GeoBone;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

// Renders items held by pets.
public class PetHeldItemLayer<T extends AbstractPet> extends BlockAndItemGeoLayer<T> {
    private static final String RIGHT_HAND_BONE = "RightHandLocator";
    // Set up the held-item layer.
    public PetHeldItemLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, T animatable) {
        if (RIGHT_HAND_BONE.equals(bone.getName())) {
            return animatable.getMainHandItem();
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, T animatable) {
        return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    /**
     * Renders the held item.
     * @param poseStack the pose stack
     * @param bone the bone
     * @param stack the item stack
     * @param animatable the pet
     */
    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, T animatable,
                                      MultiBufferSource bufferSource, float partialTick,
                                      int packedLight, int packedOverlay) {
        // Apply base scale and item-specific transforms.
        poseStack.scale(0.80f, 0.80f, 0.80f);
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.HOES)) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
        }
        if (stack.is(ItemTags.ARROWS) || stack.getItem() instanceof net.minecraft.world.item.BowItem) {
            poseStack.translate(
                    0.10F,
                    -0.20F,
                    -0.10F
            );
            poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
        }
        super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
    }
}
