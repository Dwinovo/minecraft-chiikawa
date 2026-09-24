package com.dwinovo.chiikawa.client.render;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.render.PropRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;

/**
 * Draws a prop's item from the prop's own Bedrock model, handing it to
 * {@link PropRenderer#drawItem}. A special model renderer is how the game lets code draw an
 * item, as vanilla draws a chest or a shield; each prop's item definition names this one,
 * with the item it draws ({@code PropItemModelProvider}). Registered by each loader under
 * {@link #ID}.
 */
public final class PropItemRenderer implements NoDataSpecialModelRenderer {
    /** The special model renderer type every prop's item is drawn by. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "prop");

    private final Item item;

    private PropItemRenderer(Item item) {
        this.item = item;
    }

    @Override
    public void render(ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
            boolean foil) {
        PropRenderer.drawItem(item, context, pose, buffers, light, overlay);
    }

    @Override
    public void getExtents(Set<Vector3f> output) {
        PropRenderer.groundExtents(item, output);
    }

    /** @param item the prop's item, as its item definition names it */
    public record Unbaked(Item item) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC =
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").xmap(Unbaked::new, Unbaked::item);

        @Override
        public SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            return new PropItemRenderer(item);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
