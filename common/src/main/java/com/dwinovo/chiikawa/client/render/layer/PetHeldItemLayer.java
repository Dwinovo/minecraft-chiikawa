package com.dwinovo.chiikawa.client.render.layer;

import com.dwinovo.chiikawa.client.render.AbstractPetRender;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.mojang.datafixers.util.Either;
import software.bernie.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.model.GeoBone;
import software.bernie.geckolib.cache.model.BakedGeoModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.base.GeoRenderer;

import java.util.List;
// Renders items held by pets.
public class PetHeldItemLayer<T extends AbstractPet> extends BlockAndItemGeoLayer<T, Void, AbstractPetRender.PetRenderState> {
    private static final String RIGHT_HAND_BONE = "RightHandLocator";
    private static final DataTicket<ItemStack> HELD_ITEM = DataTicket.create("chiikawa_held_item", ItemStack.class);
    // Set up the held-item layer.
    public PetHeldItemLayer(GeoRenderer<T, Void, AbstractPetRender.PetRenderState> renderer) {
        super(renderer);
    }
    /**
     * Store the held item so it can be accessed during per-bone rendering.
     * @param animatable the pet
     * @param relatedObject unused
     * @param renderState the render state
     * @param partialTick partial tick time
     */
    @Override
    public void addRenderData(T animatable, Void relatedObject, AbstractPetRender.PetRenderState renderState, float partialTick) {
        renderState.addGeckolibData(HELD_ITEM, animatable.getMainHandItem());
    }
    /**
     * Provide the bones this layer should render for.
     * @param renderState the render state
     * @param model the baked model
     * @return render data per bone
     */
    @Override
    protected List<RenderData<AbstractPetRender.PetRenderState>> getRelevantBones(AbstractPetRender.PetRenderState renderState, BakedGeoModel model) {
        return List.of(new RenderData<>(
            RIGHT_HAND_BONE,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            (bone, state) -> Either.left(state.getOrDefaultGeckolibData(HELD_ITEM, ItemStack.EMPTY))
        ));
    }
    /**
     * Renders the held item.
     * @param poseStack the pose stack
     * @param bone the bone
     * @param stack the item stack
     * @param displayContext the item display context
     * @param renderState the render state
     * @param renderTasks the render task collector
     * @param cameraState the camera render state
     * @param packedLight packed light
     * @param packedOverlay packed overlay
     * @param renderColor render color
     */
    @Override
    protected void submitItemStackRender(PoseStack poseStack, GeoBone bone, ItemStack stack, ItemDisplayContext displayContext,
                                         AbstractPetRender.PetRenderState renderState, SubmitNodeCollector renderTasks,
                                         CameraRenderState cameraState, int packedLight, int packedOverlay, int renderColor) {
        super.submitItemStackRender(poseStack, bone, stack, displayContext, renderState, renderTasks, cameraState, packedLight, packedOverlay, renderColor);
    }
}
