package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.api.ModelLibrary;
import com.dwinovo.chiikawa.anim.baked.BakedCube;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.runtime.PoseSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.math.Axis;
import java.util.function.Predicate;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

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
 * sized to fill as much of a slot as a sprite does. The transforms are its item model's,
 * which the game applies before handing it over; see {@code PropItemModelProvider}.
 */
public final class PropRenderer {
    /** A sprite's fourteen pixels of the sixteen, in blocks. */
    private static final float ITEM_SPAN = 14.0F / 16.0F;
    private static final float PIXEL = 1.0F / 16.0F;
    private static final ModelRenderer MESH = new ModelRenderer();

    private PropRenderer() {
    }

    /** The whole prop, with the pose at its origin, in model pixels. */
    public static void draw(Identifier id, PoseStack pose, SubmitNodeCollector collector, int light, int overlay) {
        draw(id, pose, collector, light, overlay, bone -> true, null);
    }

    /**
     * The prop with only some of its bones: those {@code shown} says yes to, and none of
     * what hangs from the others; and the cracks of a block being broken over it, as the
     * game draws them over a block's own model.
     */
    public static void draw(Identifier id, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
            Predicate<String> shown, ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BakedModel model = ModelLibrary.get(id);
        if (model != null) {
            draw(id, model, pose, collector, light, overlay, shown, breaking);
        }
    }

    /**
     * As an item, from its special item model: the game hands the pose over at the middle
     * of the item's block, with the display its item model gives already applied.
     */
    public static void drawItem(Item item, PoseStack pose, SubmitNodeCollector collector, int light, int overlay) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        BakedModel model = ModelLibrary.get(id);
        if (model == null) {
            return;
        }
        pose.pushPose();
        // The item model's display leaves the pose at the corner of the item's block, where
        // the game draws an item's own shapes; a prop is drawn about its middle.
        pose.translate(0.5F, 0.5F, 0.5F);
        if (item instanceof BlockItem) {
            // Standing on the floor of its block, as it stands in the world.
            pose.translate(0.0F, -0.5F, 0.0F);
            pose.scale(PIXEL, PIXEL, PIXEL);
        } else {
            // A sprite shows its south face; a prop's face is on its north.
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            fitToSprite(model, pose);
        }
        draw(id, model, pose, collector, light, overlay, bone -> true, null);
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

    private static void draw(Identifier id, BakedModel model, PoseStack pose, SubmitNodeCollector collector,
            int light, int overlay, Predicate<String> shown, ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        // A prop is not animated: every bone at rest.
        float[] rest = new float[model.bones.length * PoseSampler.FLOATS_PER_BONE];
        PoseSampler.resetIdentity(rest, model.bones.length);
        boolean[] hidden = new boolean[model.bones.length];
        for (int i = 0; i < hidden.length; i++) {
            hidden[i] = !shown.test(model.bones[i].name);
        }
        Identifier texture = Identifier.fromNamespaceAndPath(id.getNamespace(),
            "textures/entities/" + id.getPath() + ".png");
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture),
            (at, consumer) -> MESH.render(model, at, consumer, light, overlay, rest, hidden));
        if (breaking != null) {
            collector.submitCustomGeometry(pose, ModelBakery.DESTROY_TYPES.get(breaking.progress()),
                (at, consumer) -> MESH.render(model, at,
                    new SheetedDecalTextureGenerator(consumer, breaking.cameraPose(), 1.0F), light, overlay, rest, hidden));
        }
    }
}
