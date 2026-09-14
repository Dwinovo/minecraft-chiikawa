package com.dwinovo.chiikawa.mixin;

import com.dwinovo.chiikawa.entity.PetUnloadFollow;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.Visibility;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands the entities of every chunk that is about to unload to
 * {@link PetUnloadFollow}, so following pets can catch up with their owner first.
 * Only locates the entities; all rules live in {@link PetUnloadFollow}.
 */
@Mixin(PersistentEntitySectionManager.class)
public abstract class PersistentEntitySectionManagerMixin<T extends EntityAccess> {
    @Shadow
    @Final
    EntitySectionStorage<T> sectionStorage;

    @Shadow
    @Final
    private Long2ObjectMap<Visibility> chunkVisibility;

    @Shadow
    @Final
    private LongSet chunksToUnload;

    @Shadow
    public abstract boolean areEntitiesLoaded(long chunkPos);

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;processUnloads()V"
        )
    )
    private void chiikawa$beforeProcessUnloads(CallbackInfo ci) {
        if (this.chunksToUnload.isEmpty()) {
            return;
        }
        // Iterate a copy: teleporting moves entities between sections and may load the
        // destination chunk, both of which can re-enter this manager and change the set.
        for (long chunkPos : this.chunksToUnload.toLongArray()) {
            // Same filter processUnloads() uses to decide the chunk really unloads now.
            if (this.chunkVisibility.get(chunkPos) != Visibility.HIDDEN || !this.areEntitiesLoaded(chunkPos)) {
                continue;
            }
            List<T> entities = this.sectionStorage.getExistingSectionsInChunk(chunkPos)
                .flatMap(EntitySection::getEntities)
                .toList();
            if (!entities.isEmpty()) {
                PetUnloadFollow.onChunkPreUnload(entities);
            }
        }
    }
}
