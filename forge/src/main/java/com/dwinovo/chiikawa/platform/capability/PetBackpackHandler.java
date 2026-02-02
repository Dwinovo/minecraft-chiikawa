package com.dwinovo.chiikawa.platform.capability;

import net.minecraft.world.Container;
import net.minecraftforge.items.wrapper.InvWrapper;

public class PetBackpackHandler extends InvWrapper implements IPetBackpackHandler {
    public PetBackpackHandler(Container container) {
        super(container);
    }
}
