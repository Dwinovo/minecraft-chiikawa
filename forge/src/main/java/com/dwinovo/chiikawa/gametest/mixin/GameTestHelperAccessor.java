package com.dwinovo.chiikawa.gametest.mixin;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * The case's own floor, in the level's coordinates: {@code GameTestHelper.getBounds}, which
 * is public from 1.20.4 on and private on 1.20.1.
 */
@Mixin(GameTestHelper.class)
public interface GameTestHelperAccessor {
    @Invoker("getBounds")
    AABB chiikawa$getBounds();
}
