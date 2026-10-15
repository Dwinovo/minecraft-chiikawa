package com.dwinovo.chiikawa.mixin;

import com.dwinovo.chiikawa.client.photo.PhotoCapture;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Draws the player in the photos taken of them. The game does not draw the player's own body
 * unless the player is whose eyes the world is drawn from, and a photo is drawn from a pet's
 * eyes, so the owner would be missing from every photo of them. NeoForge patches that rule
 * itself to draw the player whenever the player is not a spectator; Fabric runs the game's, and
 * this does the same for the time a photo is being drawn. The rest of the time this does
 * nothing, and the player's own screen and spectating are the game's.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    /**
     * The game's test for the player's own body, which it keeps out of the frame unless it is the
     * camera's. A photo being drawn says the body is not that, so the player is drawn like anyone.
     */
    @Definition(id = "LocalPlayer", type = LocalPlayer.class)
    @Expression("? instanceof LocalPlayer")
    @ModifyExpressionValue(method = "extractVisibleEntities", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean chiikawa$ownerInPhoto(boolean isLocalPlayer) {
        boolean inPhoto = PhotoCapture.isDrawing() && !Minecraft.getInstance().player.isSpectator();
        return isLocalPlayer && !inPhoto;
    }
}
