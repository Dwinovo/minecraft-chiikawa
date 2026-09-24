package com.dwinovo.chiikawa.client.render;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.render.PropRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The special item renderer every prop's item goes through, handing it to
 * {@link PropRenderer#drawItem}: how the game lets code draw an item, as vanilla draws a
 * chest or a shield. Each loader registers it under {@link #ID}, and each prop's item
 * definition names it ({@code PropItemModelProvider}).
 */
public final class PropItemRenderer implements SpecialModelRenderer<ItemStack> {
    /** The special model type the props' item definitions name. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "prop");

    @Override
    public void render(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay, boolean foil) {
        if (stack != null) {
            PropRenderer.drawItem(stack, context, pose, buffers, light, overlay);
        }
    }

    @Override
    public ItemStack extractArgument(ItemStack stack) {
        return stack;
    }

    /** Nothing to configure: which prop to draw is the item being drawn. */
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            return new PropItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
