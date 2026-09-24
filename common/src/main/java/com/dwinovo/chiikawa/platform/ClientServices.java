package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.platform.services.IGuiHelper;

/**
 * The loader services only a game client has. Kept apart from {@link Services} so that a
 * dedicated server, which has no screens to draw, never goes looking for them.
 */
public final class ClientServices {
    public static final IGuiHelper GUI = Services.load(IGuiHelper.class);

    private ClientServices() {
    }
}
