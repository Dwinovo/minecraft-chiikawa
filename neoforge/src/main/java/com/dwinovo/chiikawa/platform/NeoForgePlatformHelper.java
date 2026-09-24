package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.platform.services.IPlatformHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;

public class NeoForgePlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.isProduction();
    }

    @Override
    public void submitPictureInPicture(GuiGraphics graphics, PictureInPictureRenderState state) {
        graphics.submitPictureInPictureRenderState(state);
    }
}
