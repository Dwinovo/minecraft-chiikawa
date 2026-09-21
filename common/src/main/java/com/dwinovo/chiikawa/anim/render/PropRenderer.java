package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.api.ModelLibrary;
import com.dwinovo.chiikawa.anim.baked.BakedCube;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.runtime.PoseSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/**
 * Draws a prop: something with a Bedrock model of its own that does not move by itself — a
 * bag, the labor board. The model is {@code models/entity/<id>.json} and the texture
 * {@code textures/entities/<id>.png}, under the id its item and block go by, laid out the
 * way a pet is: its origin where it stands or hangs from, its front towards {@code -Z}.
 *
 * <p>That one model is the thing everywhere: on a pet
 * ({@link com.dwinovo.chiikawa.anim.render.layer.BagLayer}), standing in the world, and as
 * an item. As an item it is put where vanilla would put one of its own of the same kind:
 * a block's item with {@code block/block}'s transforms, standing on the floor of its block;
 * anything else with {@code item/generated}'s, turned to face the way a sprite does and
 * sized to fill as much of a slot as a sprite does.
 */
public final class PropRenderer {
    /** A sprite's fourteen pixels of the sixteen, in blocks. */
    private static final float ITEM_SPAN = 14.0F / 16.0F;
    private static final float PIXEL = 1.0F / 16.0F;
    private static final ModelRenderer MESH = new ModelRenderer();
    /** {@code item/generated}'s display, left hands mirrored the way vanilla mirrors them. */
    private static final Map<ItemDisplayContext, ItemTransform> FLAT_ITEM = Map.of(
        ItemDisplayContext.GROUND, transform(0, 0, 0, 0, 2, 0, 0.5F),
        ItemDisplayContext.HEAD, transform(0, 180, 0, 0, 13, 7, 1.0F),
        ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, transform(0, 0, 0, 0, 3, 1, 0.55F),
        ItemDisplayContext.THIRD_PERSON_LEFT_HAND, transform(0, 0, 0, 0, 3, 1, 0.55F),
        ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, transform(0, -90, 25, 1.13F, 3.2F, 1.13F, 0.68F),
        ItemDisplayContext.FIRST_PERSON_LEFT_HAND, transform(0, -90, 25, 1.13F, 3.2F, 1.13F, 0.68F),
        ItemDisplayContext.FIXED, transform(0, 180, 0, 0, 0, 0, 1.0F));
    /** {@code block/block}'s display, the same way. */
    private static final Map<ItemDisplayContext, ItemTransform> BLOCK_ITEM = Map.of(
        ItemDisplayContext.GUI, transform(30, 225, 0, 0, 0, 0, 0.625F),
        ItemDisplayContext.GROUND, transform(0, 0, 0, 0, 3, 0, 0.25F),
        ItemDisplayContext.FIXED, transform(0, 0, 0, 0, 0, 0, 0.5F),
        ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, transform(75, 45, 0, 0, 2.5F, 0, 0.375F),
        ItemDisplayContext.THIRD_PERSON_LEFT_HAND, transform(75, 45, 0, 0, 2.5F, 0, 0.375F),
        ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, transform(0, 45, 0, 0, 0, 0, 0.4F),
        ItemDisplayContext.FIRST_PERSON_LEFT_HAND, transform(0, 225, 0, 0, 0, 0, 0.4F));

    private PropRenderer() {
    }

    /** The whole prop, with the pose at its origin, in model pixels. */
    public static void draw(ResourceLocation id, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw(id, pose, buffers, light, overlay, bone -> true);
    }

    /**
     * The prop with only some of its bones: those {@code shown} says yes to, and none of
     * what hangs from the others.
     */
    public static void draw(ResourceLocation id, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
            Predicate<String> shown) {
        BakedModel model = ModelLibrary.get(id);
        if (model != null) {
            draw(id, model, pose, buffers, light, overlay, shown);
        }
    }

    /**
     * As an item, from a built-in item renderer: the pose is at the corner of the item's
     * block, as every such renderer is handed it.
     */
    public static void drawItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        BakedModel model = ModelLibrary.get(id);
        if (model == null) {
            return;
        }
        boolean block = stack.getItem() instanceof BlockItem;
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        ItemTransform transform = (block ? BLOCK_ITEM : FLAT_ITEM).get(context);
        if (transform != null) {
            transform.apply(context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND, pose);
        }
        if (block) {
            // Standing on the floor of its block, as it stands in the world.
            pose.translate(0.0F, -0.5F, 0.0F);
            pose.scale(PIXEL, PIXEL, PIXEL);
        } else {
            // A sprite shows its south face; a prop's face is on its north.
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            fitToSprite(model, pose);
        }
        draw(id, model, pose, buffers, light, overlay, bone -> true);
        pose.popPose();
    }

    /** Scales and centres the model to span what a sprite spans. */
    private static void fitToSprite(BakedModel model, PoseStack pose) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (BakedCube cube : model.cubes) {
            minX = Math.min(minX, cube.minX);
            minY = Math.min(minY, cube.minY);
            minZ = Math.min(minZ, cube.minZ);
            maxX = Math.max(maxX, cube.maxX);
            maxY = Math.max(maxY, cube.maxY);
            maxZ = Math.max(maxZ, cube.maxZ);
        }
        float fit = ITEM_SPAN / Math.max(maxX - minX, maxY - minY);
        pose.scale(fit, fit, fit);
        pose.translate(-(minX + maxX) / 2.0F, -(minY + maxY) / 2.0F, -(minZ + maxZ) / 2.0F);
    }

    private static void draw(ResourceLocation id, BakedModel model, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay, Predicate<String> shown) {
        // A prop is not animated: every bone at rest.
        float[] rest = new float[model.bones.length * PoseSampler.FLOATS_PER_BONE];
        PoseSampler.resetIdentity(rest, model.bones.length);
        boolean[] hidden = new boolean[model.bones.length];
        for (int i = 0; i < hidden.length; i++) {
            hidden[i] = !shown.test(model.bones[i].name);
        }
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
            "textures/entities/" + id.getPath() + ".png");
        MESH.render(model, pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay, rest, hidden);
    }

    /** One display entry as a model file would give it: degrees, model pixels, a scale. */
    private static ItemTransform transform(float rotX, float rotY, float rotZ, float x, float y, float z, float scale) {
        return new ItemTransform(new Vector3f(rotX, rotY, rotZ), new Vector3f(x * PIXEL, y * PIXEL, z * PIXEL),
            new Vector3f(scale, scale, scale));
    }
}
