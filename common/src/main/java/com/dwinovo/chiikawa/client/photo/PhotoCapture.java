package com.dwinovo.chiikawa.client.photo;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.mixin.CameraAccessor;
import com.dwinovo.chiikawa.mixin.MinecraftAccessor;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.Screenshot;
import net.minecraft.client.TextureFilteringMethod;
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
 * sits there and where the shaders place the terrain from, that the lens is the camera's own,
 * and that the interface, the hand and the block outline are not drawn. The game's own way of
 * switching whose eyes it looks through is not used, because it also resets the screen effect
 * the player has on.
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
     * @param done told the frame once it has been read back, on the game's thread, a few frames
     *             after this returns; never told when the frame could not be drawn
     */
    public static void take(Entity viewpoint, int fov, Consumer<Frame> done) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        if (minecraft.level == null || minecraft.player == null || width <= 0 || height <= 0) {
            return;
        }
        GameRenderer renderer = minecraft.gameRenderer;
        CameraAccessor eyes = (CameraAccessor) renderer.getMainCamera();
        MinecraftAccessor view = (MinecraftAccessor) minecraft;
        Options options = minecraft.options;

        Entity cameraEntity = minecraft.getCameraEntity();
        float eyeHeight = eyes.chiikawa$getEyeHeight();
        float eyeHeightOld = eyes.chiikawa$getEyeHeightOld();
        boolean hideGui = options.hideGui;
        CameraType cameraType = options.getCameraType();
        // Drawing the level looks at whatever the cursor is on, from wherever it is drawn from.
        HitResult hit = minecraft.hitResult;
        Entity pointedAt = minecraft.crosshairPickEntity;

        RenderTarget target = new TextureTarget("Chiikawa photo", width, height, true);
        boolean read = false;
        try {
            RenderSystem.getDevice().createCommandEncoder()
                .clearColorAndDepthTextures(target.getColorTexture(), 0, target.getDepthTexture(), 1.0);
            inUse = target;

            options.hideGui = true;
            options.setCameraType(CameraType.FIRST_PERSON);
            lens = fov;
            renderer.setRenderBlockOutline(false);
            view.chiikawa$setCameraEntity(viewpoint);
            // At the new eyes' height at once, not eased to it over the next few ticks.
            seat(minecraft, viewpoint.getEyeHeight(), viewpoint.getEyeHeight());
            renderer.renderLevel(minecraft.getDeltaTracker());

            // The game hands the pixels over once the graphics card has finished with them, a few frames on.
            Screenshot.takeScreenshot(target, image -> {
                try (image) {
                    done.accept(new Frame(image.getPixelsABGR(), image.getWidth(), image.getHeight()));
                } finally {
                    target.destroyBuffers();
                }
            });
            read = true;
        } catch (RuntimeException ex) {
            Constants.LOG.error("[chiikawa-photo] Could not draw a photo", ex);
        } finally {
            inUse = null;
            if (!read) {
                target.destroyBuffers();
            }
            view.chiikawa$setCameraEntity(cameraEntity);
            seat(minecraft, eyeHeight, eyeHeightOld);
            options.hideGui = hideGui;
            options.setCameraType(cameraType);
            lens = Double.NaN;
            renderer.setRenderBlockOutline(true);
            minecraft.hitResult = hit;
            minecraft.crosshairPickEntity = pointedAt;
        }
    }

    /**
     * Sets the game's camera up on its camera entity afresh, as the game does it: set up for a
     * frame, as the panorama does, then ticked, which is what has the camera read the sky and
     * fog from where it now is. Without the tick it has no surroundings to read them from and
     * the sky is drawn black, in the photo and on the player's screen after it.
     *
     * <p>The shaders are told where the camera is, as the game does once at the start of every
     * frame: the terrain is placed relative to that position and not to the camera the level is
     * drawn with. Left at the player's eyes, the terrain of a photo is shifted against the
     * entities by as much as the pet's eyes are from the player's, and a wall behind the owner is
     * drawn in front of their body.
     */
    private static void seat(Minecraft minecraft, float eyeHeight, float eyeHeightOld) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        CameraAccessor eyes = (CameraAccessor) camera;
        eyes.chiikawa$setEyeHeight(eyeHeight);
        eyes.chiikawa$setEyeHeightOld(eyeHeightOld);
        camera.reset();
        minecraft.gameRenderer.updateCamera(minecraft.getDeltaTracker());
        camera.tick();
        // The tick eases the eye height too, and these are not to move.
        eyes.chiikawa$setEyeHeight(eyeHeight);
        eyes.chiikawa$setEyeHeightOld(eyeHeightOld);
        Options options = minecraft.options;
        minecraft.gameRenderer.getGlobalSettingsUniform().update(
            minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(),
            options.glintStrength().get(), minecraft.level.getGameTime(), minecraft.getDeltaTracker(),
            options.getMenuBackgroundBlurriness(), camera,
            options.textureFiltering().get() == TextureFilteringMethod.RGSS);
    }
}
