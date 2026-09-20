package com.dwinovo.chiikawa.client.ui.mc;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Icon;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;

/**
 * Draws the {@code chiikawa-ui} library in the world, on a surface already turned to face
 * the camera — the same widgets a screen uses, floating over a pet.
 *
 * <p>Both the fills and the text test depth, so a label behind a wall is hidden by it.
 *
 * <p>A screen has no depth: what is drawn later simply covers what came before. Here
 * everything would land on one plane, and two fills on one plane fight — the panel body
 * and the outline under it come out in bands, a row of one and a row of the other,
 * because their depths differ by the last bit of a float. So each call sits a hair nearer
 * the camera than the one before it, and the painter's order a widget expects holds.
 *
 * <p>One surface is made per label, so the layers start over every time and a label never
 * climbs away from the pet it belongs to.
 */
public final class WorldSurface implements DrawSurface {
    /** Labels read the same at night as by day. */
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;
    /**
     * How much nearer each call sits, in text pixels. Wide enough that the depth buffer
     * still tells two layers apart at the far end of a label's range, and far too little
     * for the label to look like it stands off the pet.
     */
    private static final float LAYER_STEP = 0.1F;

    private final PoseStack pose;
    private final MultiBufferSource bufferSource;
    private final Font font;
    private final float alpha;
    private float layer;

    /**
     * @param pose already translated to the label's spot, turned to the camera and scaled
     *             to text pixels, with y running down as on a screen and z towards the camera
     * @param alpha how far faded in the whole surface is, 0 to 1 — its fills, its glyphs and
     *              the items drawn on it give way together
     */
    public WorldSurface(PoseStack pose, MultiBufferSource bufferSource, Font font, float alpha) {
        this.pose = pose;
        this.bufferSource = bufferSource;
        this.font = font;
        this.alpha = alpha;
    }

    @Override
    public void fillRect(int x, int y, int width, int height, int argb) {
        float z = nextLayer();
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.textBackground());
        // Wound as the game winds its own name-tag backdrop, which this render type culls by.
        int faded = faded(argb);
        consumer.addVertex(matrix, x, y + height, z).setColor(faded).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x + width, y + height, z).setColor(faded).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x + width, y, z).setColor(faded).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x, y, z).setColor(faded).setLight(FULL_BRIGHT);
    }

    @Override
    public void drawText(String text, int x, int y, int argb) {
        // drawInBatch takes no z, so the layer goes through the matrix.
        pose.pushPose();
        pose.translate(0.0F, 0.0F, nextLayer());
        font.drawInBatch(text, x, y, faded(argb), false, pose.last().pose(), bufferSource,
            Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
        pose.popPose();
    }

    /**
     * An item, in the box a screen would give it. It is pushed half its own depth towards
     * the camera first: a block's icon is a little cube, and one centred on the card would
     * have its back half swallowed by the card it sits on.
     */
    @Override
    public void drawIcon(Icon icon, int x, int y) {
        if (!(icon instanceof ItemIcon item) || item.stack().isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        pose.pushPose();
        pose.translate(x + UiStyle.ICON / 2.0F, y + UiStyle.ICON / 2.0F, nextLayer() + UiStyle.ICON / 2.0F);
        // The game hands an item a block-wide space with y up; this one is icon-wide with y down.
        pose.scale(UiStyle.ICON, -UiStyle.ICON, UiStyle.ICON);
        minecraft.getItemRenderer().renderStatic(item.stack(), ItemDisplayContext.GUI, FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY, pose, new FadingBufferSource(bufferSource, alpha), minecraft.level, 0);
        pose.popPose();
    }

    @Override
    public int textWidth(String text) {
        return font.width(text);
    }

    @Override
    public int lineHeight() {
        return font.lineHeight;
    }

    /** The colour as it reads through the fade. */
    private int faded(int argb) {
        return Math.round((argb >>> 24) * alpha) << 24 | argb & 0x00FFFFFF;
    }

    private float nextLayer() {
        float z = layer;
        layer += LAYER_STEP;
        return z;
    }
}
