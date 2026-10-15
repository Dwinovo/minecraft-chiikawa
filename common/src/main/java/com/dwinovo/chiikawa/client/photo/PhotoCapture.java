package com.dwinovo.chiikawa.client.photo;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.mixin.CameraAccessor;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the world once from an entity's eyes, without the player seeing any of it: into a
 * render target of its own, the size of the window, which the game and a shader pack are
 * told is the one they draw into. The player's screen does not flicker or jump, and what
 * comes out is the frame the game draws, shaders and all.
 *
 * <p>Everything about the player's game that the frame has to differ in is changed, and put
 * back before this returns, whatever happens in between: whose eyes it is, how the camera
 * sits there, that the lens is the camera's own, and that the interface, the hand and the block outline are not
 * drawn. The game's own way of switching whose eyes it looks through is not used, because it
 * also resets the screen effect the player has on.
 */
public final class PhotoCapture {
    /** The target the frame is being drawn into, while it is. */
    private static volatile RenderTarget inUse;
    /** The lens it is being drawn through, in degrees, while it is. */
    private static volatile double lens = Double.NaN;

    /** What was drawn, as the game keeps pixels: one int each, alpha highest, then blue, green, red. */
    public record Frame(int[] abgr, int width, int height) {
    }

    private PhotoCapture() {
    }

    /** @return the render target the game is to draw into now, or nothing when no photo is being taken */
    @Nullable
    public static RenderTarget targetInUse() {
        return inUse;
    }

    /** @return whether a photo is being drawn now */
    public static boolean isDrawing() {
        return inUse != null;
    }

    /**
     * @return the field of view the game is to draw with now, from top to bottom of the picture,
     *         or nothing when no photo is being taken and the player's own is used
     */
    public static OptionalDouble lensInUse() {
        double fov = lens;
        return Double.isNaN(fov) ? OptionalDouble.empty() : OptionalDouble.of(fov);
    }

    /**
     * @param viewpoint whose eyes the world is drawn from
     * @param fov how wide the lens sees, in degrees from top to bottom
     * @return the frame, or nothing when it could not be drawn
     */
    public static Optional<Frame> take(Entity viewpoint, int fov) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        if (minecraft.level == null || minecraft.player == null || width <= 0 || height <= 0) {
            return Optional.empty();
        }
        GameRenderer renderer = minecraft.gameRenderer;
        Camera camera = renderer.getMainCamera();
        CameraAccessor eyes = (CameraAccessor) camera;
        Options options = minecraft.options;

        Entity cameraEntity = minecraft.cameraEntity;
        float eyeHeight = eyes.chiikawa$getEyeHeight();
        float eyeHeightOld = eyes.chiikawa$getEyeHeightOld();
        boolean hideGui = options.hideGui;
        CameraType cameraType = options.getCameraType();
        // Drawing the level looks at whatever the cursor is on, from wherever it is drawn from.
        HitResult hit = minecraft.hitResult;
        Entity pointedAt = minecraft.crosshairPickEntity;

        RenderTarget target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        try {
            target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            target.clear(Minecraft.ON_OSX);
            inUse = target;

            options.hideGui = true;
            options.setCameraType(CameraType.FIRST_PERSON);
            lens = fov;
            renderer.setRenderBlockOutline(false);
            minecraft.cameraEntity = viewpoint;
            // At the new eyes' height at once, not eased to it over the next few ticks.
            eyes.chiikawa$setEyeHeight(viewpoint.getEyeHeight());
            eyes.chiikawa$setEyeHeightOld(viewpoint.getEyeHeight());
            camera.reset();

            minecraft.levelRenderer.graphicsChanged();
            target.bindWrite(false);
            renderer.renderLevel(minecraft.getTimer());

            NativeImage image = Screenshot.takeScreenshot(target);
            try {
                return Optional.of(new Frame(image.getPixelsRGBA(), image.getWidth(), image.getHeight()));
            } finally {
                image.close();
            }
        } catch (RuntimeException ex) {
            Constants.LOG.error("[chiikawa-photo] Could not draw a photo", ex);
            return Optional.empty();
        } finally {
            inUse = null;
            target.destroyBuffers();
            minecraft.cameraEntity = cameraEntity;
            eyes.chiikawa$setEyeHeight(eyeHeight);
            eyes.chiikawa$setEyeHeightOld(eyeHeightOld);
            camera.reset();
            options.hideGui = hideGui;
            options.setCameraType(cameraType);
            lens = Double.NaN;
            renderer.setRenderBlockOutline(true);
            minecraft.hitResult = hit;
            minecraft.crosshairPickEntity = pointedAt;
            minecraft.levelRenderer.graphicsChanged();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }
}
