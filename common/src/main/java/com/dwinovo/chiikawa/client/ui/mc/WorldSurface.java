package com.dwinovo.chiikawa.client.ui.mc;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/**
 * Draws the {@code chiikawa-ui} library in the world, on a surface already turned to face
 * the camera — the same widgets a screen uses, floating over a pet.
 *
 * <p>Both fills and text go through render types that test depth, so a label behind a
 * wall is hidden by that wall. Minecraft's own name-tag backdrop cannot be used for this:
 * it draws in front of its own glyphs and only works see-through.
 *
 * @param pose already translated to the label's spot, turned to the camera and scaled to
 *             text pixels, with y running down as on a screen
 */
public record WorldSurface(PoseStack pose, MultiBufferSource bufferSource, Font font) implements DrawSurface {
    /** Labels read the same at night as by day. */
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;
    /**
     * How far behind the text a fill sits, in text pixels. Both write depth, and which of
     * the two batches the game flushes first is not ours to say, so the panel keeps out of
     * the glyphs' way by depth instead of by luck. Far too little to see.
     */
    private static final float BEHIND_TEXT = -0.1F;

    @Override
    public void fillRect(int x, int y, int width, int height, int argb) {
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.textBackground());
        // Wound as the game winds its own name-tag backdrop, which this render type culls by.
        consumer.addVertex(matrix, x, y + height, BEHIND_TEXT).setColor(argb).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x + width, y + height, BEHIND_TEXT).setColor(argb).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x + width, y, BEHIND_TEXT).setColor(argb).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x, y, BEHIND_TEXT).setColor(argb).setLight(FULL_BRIGHT);
    }

    @Override
    public void drawText(String text, int x, int y, int argb) {
        font.drawInBatch(text, x, y, argb, false, pose.last().pose(), bufferSource,
            Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
    }

    @Override
    public int textWidth(String text) {
        return font.width(text);
    }

    @Override
    public int lineHeight() {
        return font.lineHeight;
    }
}
