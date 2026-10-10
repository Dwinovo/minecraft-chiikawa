package com.dwinovo.chiikawa;

import com.dwinovo.chiikawa.client.screen.SettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Puts the settings screen behind the config button Mod Menu shows beside the mod. Only
 * Mod Menu ever loads this class: the mod does not need it, and runs without it.
 */
public class ChiikawaModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SettingsScreen::new;
    }
}
