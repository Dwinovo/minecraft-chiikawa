package com.dwinovo.chiikawa.client.render;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.render.PropRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * The props' items ({@code InitItems#PROPS}), drawn from their own Bedrock models by
 * {@link PropRenderer}: the special item model the game hands every prop item to, as it
 * hands a chest's item to the chest's. Which prop it is, is which item it is.
 */
public final class PropItemRenderer implements SpecialModelRenderer<Item> {
    /** What a prop's item model names this renderer by; each loader registers it under this. */
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "prop");

    @Override
    public void submit(@Nullable Item item, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
            boolean hasFoil, int outlineColor) {
        if (item != null) {
            PropRenderer.drawItem(item, pose, collector, light, overlay);
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        // Nothing to report: a prop as an item is fitted to its slot, never spilling out of it.
    }

    @Override
    public Item extractArgument(ItemStack stack) {
        return stack.getItem();
    }

    /** The renderer as an item model names it: nothing to it but its type. */
    public record Unbaked() implements SpecialModelRenderer.Unbaked<Item> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<Item> bake(SpecialModelRenderer.BakingContext context) {
            return new PropItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
