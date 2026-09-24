package com.dwinovo.chiikawa.client.manual;

import com.dwinovo.chiikawa.platform.ClientServices;
import com.dwinovo.chiikawa.ui.Rect;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;

/**
 * What a page of the handbook hands the game to draw its pets and props in: a picture in
 * picture, which the game draws into a texture of its own and then onto the screen, the
 * way it draws the player in the inventory. A screen cannot draw a model itself.
 *
 * <p>The game keeps one texture for every picture of a kind, so a page puts all its panels
 * in one picture, each clipped to its own frame, rather than one picture a panel.
 *
 * @param panels the panels of the page, each with what stands in it
 */
public record ManualStageRenderState(
    List<Panel> panels,
    int x0,
    int y0,
    int x1,
    int y1,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public ManualStageRenderState {
        panels = List.copyOf(panels);
    }

    /** Hands the game one picture for these panels, spanning all of them. */
    public static void submit(GuiGraphics graphics, List<Panel> panels) {
        if (panels.isEmpty()) {
            return;
        }
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
        for (Panel panel : panels) {
            x0 = Math.min(x0, panel.clip().x());
            y0 = Math.min(y0, panel.clip().y());
            x1 = Math.max(x1, panel.clip().right());
            y1 = Math.max(y1, panel.clip().bottom());
        }
        ScreenRectangle scissor = ClientServices.GUI.scissorArea(graphics);
        ClientServices.GUI.submitPicture(graphics, new ManualStageRenderState(panels, x0, y0, x1, y1, scissor,
            PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissor)));
    }

    /** Drawn in gui pixels, as the screen is laid out. */
    @Override
    public float scale() {
        return 1.0F;
    }

    /**
     * One panel of the strip.
     *
     * @param clip the panel's frame on the screen; nothing in it is drawn outside
     * @param actors what stands in it, in the order they are drawn
     */
    public record Panel(Rect clip, List<Actor> actors) {
        public Panel {
            actors = List.copyOf(actors);
        }
    }

    /** Something with a model, drawn into the picture. */
    public interface Actor {
        /**
         * @param pose gui pixels, with the origin at the screen's top left corner and y down,
         *             already turned in depth the way the game turns an entity it draws in a
         *             screen
         */
        void draw(PoseStack pose, MultiBufferSource buffers);
    }
}
