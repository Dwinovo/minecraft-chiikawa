package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.api.ModelLibrary;
import com.dwinovo.chiikawa.anim.baked.BakedCube;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.runtime.PoseSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Draws a prop: something with a Bedrock model of its own that does not move by itself — a
 * bag, the labor board. The model is {@code models/entity/<id>.json} and the texture
 * {@code textures/entities/<id>.png}, under the id its item and block go by, laid out the
 * way a pet is: its origin where it stands or hangs from, its front towards {@code -Z}.
 *
 * <p>That one model is the thing everywhere: on a pet
 * ({@link com.dwinovo.chiikawa.anim.render.layer.BagLayer}), standing in the world, and as
 * an item. As an item it is put where vanilla would put one of its own of the same kind:
 * its item model borrows {@code block/block}'s transforms for a block's item, standing it
 * on the floor of its block; {@code item/handheld}'s for a sword or a tool, laid corner to
 * corner as a sword's sprite is drawn; and {@code item/generated}'s for anything else.
 * Either of the last two is turned to face the way a sprite does and sized to fill as much
 * of a slot as a sprite does.
 *
 * <p>A sword or a tool is modelled standing up, the end it is held by at the bottom and its
 * front towards {@code -Z} like any prop's.
 */
public final class PropRenderer {
    /** A sprite's fourteen pixels of the sixteen, in blocks. */
    private static final float ITEM_SPAN = 14.0F / 16.0F;
    private static final float PIXEL = 1.0F / 16.0F;
    private static final ModelRenderer MESH = new ModelRenderer();
    /** How far a sword's sprite leans: its blade runs from the bottom left corner to the top right. */
    private static final float HANDHELD_LEAN = 45.0F;

    private PropRenderer() {
    }

    /** The whole prop, with the pose at its origin, in model pixels. */
    public static void draw(Identifier id, PoseStack pose, SubmitNodeCollector collector, int light, int overlay) {
        draw(id, pose, collector, light, overlay, bone -> true);
    }

    /**
     * The prop with only some of its bones: those {@code shown} says yes to, and none of
     * what hangs from the others.
     */
    public static void draw(Identifier id, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
            Predicate<String> shown) {
        BakedModel model = ModelLibrary.get(id);
        if (model != null) {
            draw(id, model, pose, collector, light, overlay, shown);
        }
    }

    /**
     * As an item, from its special item renderer: the pose is at the corner of the item's
     * block, already carried by the item model's transforms, as every such renderer is
     * handed it.
     */
    public static void drawItem(Item item, PoseStack pose, SubmitNodeCollector collector, int light, int overlay) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        BakedModel model = ModelLibrary.get(id);
        if (model == null) {
            return;
        }
        pose.pushPose();
        placeItem(item, model, pose);
        draw(id, model, pose, collector, light, overlay, bone -> true);
        pose.popPose();
    }

    /** Where the item's model reaches, for the game to fit it into a slot or onto the ground. */
    public static void itemExtents(Item item, Consumer<Vector3fc> output) {
        BakedModel model = ModelLibrary.get(BuiltInRegistries.ITEM.getKey(item));
        if (model == null) {
            return;
        }
        PoseStack pose = new PoseStack();
        placeItem(item, model, pose);
        for (BakedCube cube : model.cubes) {
            for (int corner = 0; corner < 8; corner++) {
                output.accept(pose.last().pose().transformPosition(
                    (corner & 1) == 0 ? cube.minX : cube.maxX,
                    (corner & 2) == 0 ? cube.minY : cube.maxY,
                    (corner & 4) == 0 ? cube.minZ : cube.maxZ,
                    new Vector3f()));
            }
        }
    }

    /**
     * Whether vanilla would draw the item with its handheld model: its swords and its tools,
     * the items a tool material makes, which carry a tool's rules.
     */
    public static boolean isHandheld(Item item) {
        return item.components().has(DataComponents.TOOL);
    }

    /** From the corner of the item's block to the prop's origin, in model pixels. */
    private static void placeItem(Item item, BakedModel model, PoseStack pose) {
        pose.translate(0.5F, 0.5F, 0.5F);
        if (item instanceof BlockItem) {
            // Standing on the floor of its block, as it stands in the world.
            pose.translate(0.0F, -0.5F, 0.0F);
            pose.scale(PIXEL, PIXEL, PIXEL);
        } else {
            intoSprite(model, isHandheld(item) ? HANDHELD_LEAN : 0.0F, pose);
        }
    }

    /**
     * From a flat item's frame to the model's: facing the way a sprite faces, leaning
     * {@code lean} degrees clockwise as the viewer sees it, and scaled and centred so that,
     * leaning so, it spans what a sprite spans.
     */
    static void intoSprite(BakedModel model, float lean, PoseStack pose) {
        // A sprite shows its south face; a prop's face is on its north.
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        // Anticlockwise about the model's Z, which the viewer, on its other side, sees as clockwise.
        pose.mulPose(Axis.ZP.rotationDegrees(lean));
        fitToSprite(model, lean, pose);
    }

    /** Scales and centres the model to span what a sprite spans, leaning {@code lean} degrees. */
    private static void fitToSprite(BakedModel model, float lean, PoseStack pose) {
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
        float cos = (float) Math.abs(Math.cos(Math.toRadians(lean)));
        float sin = (float) Math.abs(Math.sin(Math.toRadians(lean)));
        float width = maxX - minX;
        float height = maxY - minY;
        // How wide and how tall the model stands once it leans.
        float fit = ITEM_SPAN / Math.max(width * cos + height * sin, width * sin + height * cos);
        pose.scale(fit, fit, fit);
        pose.translate(-(minX + maxX) / 2.0F, -(minY + maxY) / 2.0F, -(minZ + maxZ) / 2.0F);
    }

    private static void draw(Identifier id, BakedModel model, PoseStack pose, SubmitNodeCollector collector,
            int light, int overlay, Predicate<String> shown) {
        // A prop is not animated: every bone at rest.
        float[] rest = new float[model.bones.length * PoseSampler.FLOATS_PER_BONE];
        PoseSampler.resetIdentity(rest, model.bones.length);
        boolean[] hidden = new boolean[model.bones.length];
        for (int i = 0; i < hidden.length; i++) {
            hidden[i] = !shown.test(model.bones[i].name);
        }
        Identifier texture = Identifier.fromNamespaceAndPath(id.getNamespace(),
            "textures/entities/" + id.getPath() + ".png");
        collector.submitCustomGeometry(pose, RenderTypes.entityCutoutNoCull(texture),
            (drawPose, consumer) -> MESH.render(model, drawPose, consumer, light, overlay, rest, hidden));
    }

    /**
     * A prop's item, as the game draws an item it has no flat picture of: a special item
     * model of type {@code chiikawa:prop} naming the prop, over a base model that carries
     * the transforms. Each loader registers the type its own way.
     */
    public static final class ItemRenderer implements NoDataSpecialModelRenderer {
        /** What the item models call this renderer. */
        public static final Identifier TYPE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "prop");

        private final Item item;

        private ItemRenderer(Item item) {
            this.item = item;
        }

        @Override
        public void submit(ItemDisplayContext context, PoseStack pose, SubmitNodeCollector collector, int light,
                int overlay, boolean hasFoil, int outlineColor) {
            drawItem(item, pose, collector, light, overlay);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {
            itemExtents(item, output);
        }

        /** @param prop the prop's id, its item's */
        public record Unbaked(Identifier prop) implements SpecialModelRenderer.Unbaked {
            public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Identifier.CODEC.fieldOf("prop").forGetter(Unbaked::prop)
            ).apply(instance, Unbaked::new));

            @Override
            public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
                return new ItemRenderer(BuiltInRegistries.ITEM.getValue(prop));
            }

            @Override
            public MapCodec<Unbaked> type() {
                return MAP_CODEC;
            }
        }
    }
}
