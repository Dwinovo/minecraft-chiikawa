package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.api.ModelLibrary;
import com.dwinovo.chiikawa.anim.baked.BakedCube;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.runtime.PoseSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/**
 * Draws a bag from its own Bedrock model: {@code models/entity/<item>.json} and
 * {@code textures/entities/<item>.png}, one bone, centred on its origin with its face
 * towards {@code -Z}, the way a pet faces.
 *
 * <p>That one model is the bag everywhere. On a pet it hangs from a bone of the pet's own
 * model, which places and turns it; see {@link com.dwinovo.chiikawa.anim.render.layer.BagLayer}.
 * As an item — in a slot, a hand, a frame, on the ground — it stands wherever a flat item
 * would: it borrows {@code item/generated}'s own transforms, turns its face to where a
 * sprite's is, and is sized to fill as much of the slot as a sprite does.
 */
public final class BagRenderer {
    /** A sprite's fourteen pixels of the sixteen, in blocks. */
    private static final float ITEM_SPAN = 14.0F / 16.0F;
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

    private BagRenderer() {
    }

    /** On a pet: the pose is already at the bag's centre, turned, in model pixels. */
    public static void drawWorn(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BakedModel model = modelOf(stack);
        if (model != null) {
            draw(stack, model, pose, buffers, light, overlay);
        }
    }

    /**
     * As an item, from a built-in item renderer: the pose is at the corner of the item's
     * block, as every such renderer is handed it.
     */
    public static void drawItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        BakedModel model = modelOf(stack);
        if (model == null) {
            return;
        }
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

        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        ItemTransform transform = FLAT_ITEM.get(context);
        if (transform != null) {
            transform.apply(context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND, pose);
        }
        // A sprite shows its south face; a bag's face is on its north.
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.scale(fit, fit, fit);
        pose.translate(-(minX + maxX) / 2.0F, -(minY + maxY) / 2.0F, -(minZ + maxZ) / 2.0F);
        draw(stack, model, pose, buffers, light, overlay);
        pose.popPose();
    }

    private static BakedModel modelOf(ItemStack stack) {
        return ModelLibrary.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static void draw(ItemStack stack, BakedModel model, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        // A bag is not animated: every bone at rest.
        float[] rest = new float[model.bones.length * PoseSampler.FLOATS_PER_BONE];
        PoseSampler.resetIdentity(rest, model.bones.length);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
            "textures/entities/" + id.getPath() + ".png");
        MESH.render(model, pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay, rest, null);
    }

    /** One display entry as a model file would give it: degrees, model pixels, a scale. */
    private static ItemTransform transform(float rotX, float rotY, float rotZ, float x, float y, float z, float scale) {
        return new ItemTransform(new Vector3f(rotX, rotY, rotZ), new Vector3f(x / 16.0F, y / 16.0F, z / 16.0F),
            new Vector3f(scale, scale, scale));
    }
}
