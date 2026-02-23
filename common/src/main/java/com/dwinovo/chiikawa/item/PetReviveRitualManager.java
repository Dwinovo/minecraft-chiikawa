package com.dwinovo.chiikawa.item;

import com.dwinovo.chiikawa.entity.AbstractPet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.ProblemReporter;
import org.jetbrains.annotations.Nullable;

public final class PetReviveRitualManager {
    private static final int PARTICLE_INTERVAL_TICKS = 4;
    private static final Map<LevelKey, List<PendingRevive>> PENDING_REVIVES = new HashMap<>();

    private PetReviveRitualManager() {
    }

    public static boolean tryStartRitual(
        ServerLevel level,
        BlockPos cakePos,
        BlockState cakeState,
        @Nullable Player player,
        ItemStack sourceStack,
        Supplier<? extends EntityType<? extends AbstractPet>> entityType
    ) {
        if (!CakeReviveRitual.isOfferingBlock(cakeState)) {
            return false;
        }

        consumeCakeSlice(level, cakePos, cakeState, player);
        ItemStack preservedDoll = sourceStack.copyWithCount(1);
        CompoundTag petData = PetDollData.readPetData(sourceStack).map(CompoundTag::copy).orElse(null);
        float yRot = player != null ? player.getYRot() : 0.0F;
        float xRot = player != null ? player.getXRot() : 0.0F;
        Vec3 spawnPos = Vec3.atBottomCenterOf(cakePos).add(0.0D, 1.0D, 0.0D);

        PendingRevive revive = new PendingRevive(
            entityType,
            spawnPos,
            yRot,
            xRot,
            petData,
            preservedDoll,
            level.getGameTime() + CakeReviveRitual.REVIVE_DELAY_TICKS
        );
        PENDING_REVIVES.computeIfAbsent(LevelKey.from(level), ignored -> new ArrayList<>()).add(revive);
        return true;
    }

    public static void tickServer(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            tickLevel(level);
        }
    }

    private static void tickLevel(ServerLevel level) {
        LevelKey key = LevelKey.from(level);
        List<PendingRevive> revives = PENDING_REVIVES.get(key);
        if (revives == null || revives.isEmpty()) {
            return;
        }

        Iterator<PendingRevive> iterator = revives.iterator();
        while (iterator.hasNext()) {
            PendingRevive revive = iterator.next();
            BlockPos blockPos = BlockPos.containing(revive.spawnPos);
            if (!level.isLoaded(blockPos)) {
                continue;
            }

            long now = level.getGameTime();
            if (now < revive.reviveAtGameTime) {
                if ((revive.reviveAtGameTime - now) % PARTICLE_INTERVAL_TICKS == 0) {
                    sendRitualParticles(level, revive.spawnPos);
                }
                continue;
            }

            if (!spawnRevivedPet(level, revive)) {
                dropPreservedDoll(level, revive);
            }
            iterator.remove();
        }

        if (revives.isEmpty()) {
            PENDING_REVIVES.remove(key);
        }
    }

    private static void consumeCakeSlice(ServerLevel level, BlockPos cakePos, BlockState cakeState, @Nullable Player player) {
        if (CakeReviveRitual.shouldRemoveCakeAfterOffering(cakeState)) {
            level.removeBlock(cakePos, false);
        } else {
            int nextBites = CakeReviveRitual.nextBitesAfterOffering(cakeState);
            level.setBlock(cakePos, cakeState.setValue(CakeBlock.BITES, nextBites), 3);
        }
        level.gameEvent(GameEvent.BLOCK_CHANGE, cakePos, GameEvent.Context.of(player, cakeState));
        level.playSound(null, cakePos, SoundEvents.GENERIC_EAT.value(), SoundSource.BLOCKS, 0.9F, 1.0F);
    }

    private static void sendRitualParticles(ServerLevel level, Vec3 center) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y + 0.3D, center.z, 6, 0.35D, 0.2D, 0.35D, 0.02D);
        level.sendParticles(ParticleTypes.HEART, center.x, center.y + 0.6D, center.z, 1, 0.25D, 0.2D, 0.25D, 0.0D);
    }

    private static boolean spawnRevivedPet(ServerLevel level, PendingRevive revive) {
        AbstractPet pet = revive.entityType.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (pet == null) {
            return false;
        }

        if (revive.petData != null) {
            CompoundTag petDataCopy = revive.petData.copy();
            PetDollData.sanitizeForRevive(petDataCopy);
            pet.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), petDataCopy));
        }

        pet.snapTo(revive.spawnPos.x, revive.spawnPos.y, revive.spawnPos.z, revive.yRot, revive.xRot);
        pet.setHealth(pet.getMaxHealth());

        if (!level.noCollision(pet)) {
            return false;
        }
        if (!level.addFreshEntity(pet)) {
            return false;
        }

        level.playSound(null, pet.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.NEUTRAL, 0.8F, 1.15F);
        sendRitualParticles(level, revive.spawnPos);
        return true;
    }

    private static void dropPreservedDoll(ServerLevel level, PendingRevive revive) {
        ItemStack refund = revive.preservedDoll.copy();
        ItemEntity item = new ItemEntity(level, revive.spawnPos.x, revive.spawnPos.y, revive.spawnPos.z, refund);
        level.addFreshEntity(item);
    }

    private record PendingRevive(
        Supplier<? extends EntityType<? extends AbstractPet>> entityType,
        Vec3 spawnPos,
        float yRot,
        float xRot,
        @Nullable CompoundTag petData,
        ItemStack preservedDoll,
        long reviveAtGameTime
    ) {
    }

    private record LevelKey(String location) {
        private static LevelKey from(ServerLevel level) {
            return new LevelKey(level.dimension().location().toString());
        }
    }
}
