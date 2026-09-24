package com.dwinovo.chiikawa.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Calling every pet you own back to you, wherever it got to. Pets somebody has loaded are
 * found by looking through the levels; the rest come out of {@link PetRoster}, whose entry
 * says which chunk to wake before asking for them by name.
 *
 * <p>Waking a chunk does not hand the pet over on the spot — the game reads its entities
 * back over the next few ticks — so a ring that has anyone to wait for keeps looking for a
 * few seconds and only then tells the owner how it went. That wait is the whole reason
 * this is not a method on the bell.
 *
 * <p>A pet that was told to sit comes too, and is still sitting when it arrives: being
 * called is the owner speaking, and the answer to "stay there" is not "so stay there".
 */
public final class PetRecall {
    /** How long a ring keeps looking for a pet whose chunk it woke. */
    private static final int WAIT_TICKS = 60;
    /**
     * How many sleeping chunks one ring is allowed to wake. A bell that loads a hundred
     * chunks because somebody left pets all over the world is a bell that stalls the
     * server; the ones it did not reach are still named in the message.
     */
    private static final int CHUNKS_PER_RING = 16;

    /** One pet a ring is still waiting on. */
    private record Awaited(UUID pet, String name, ResourceKey<Level> dimension, BlockPos pos) {
    }

    /** One ring, from the moment it is rung until it has something to report. */
    private static final class Ring {
        private final UUID owner;
        private final List<Awaited> awaited;
        private final List<String> missing = new ArrayList<>();
        private final long until;
        private int brought;

        private Ring(UUID owner, List<Awaited> awaited, int brought, List<String> missing, long until) {
            this.owner = owner;
            this.awaited = new ArrayList<>(awaited);
            this.brought = brought;
            this.missing.addAll(missing);
            this.until = until;
        }
    }

    /** Rings being waited on. Transient: a ring lost to a restart is a ring rung again. */
    private static final List<Ring> RINGING = new ArrayList<>();

    private PetRecall() {
    }

    /**
     * Calls every pet {@code owner} owns to where {@code owner} is standing. Pets nobody
     * has loaded take a moment; the owner is told how it went once they have all had
     * their chance to turn up.
     */
    public static void ring(ServerPlayer owner) {
        MinecraftServer server = owner.server;
        Set<UUID> answered = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (AbstractPet pet : level.getEntities(EntityTypeTest.forClass(AbstractPet.class),
                    candidate -> candidate.isAlive() && owner.getUUID().equals(candidate.getOwnerUUID()))) {
                if (bring(pet, owner)) {
                    answered.add(pet.getUUID());
                }
            }
        }

        List<Awaited> awaited = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (PetRoster.Entry entry : PetRoster.of(owner.serverLevel()).pets(owner.getUUID())) {
            if (answered.contains(entry.pet())) {
                continue;
            }
            if (awaited.size() >= CHUNKS_PER_RING || server.getLevel(entry.dimension()) == null) {
                missing.add(entry.name());
                continue;
            }
            awaited.add(new Awaited(entry.pet(), entry.name(), entry.dimension(), entry.pos()));
        }

        Ring ring = new Ring(owner.getUUID(), awaited, answered.size(), missing,
            server.overworld().getGameTime() + WAIT_TICKS);
        if (awaited.isEmpty()) {
            report(server, ring);
            return;
        }
        RINGING.add(ring);
        wake(server, ring);
    }

    /** Registered by both loaders on the server tick, beside the follow check. */
    public static void tickServer(MinecraftServer server) {
        if (RINGING.isEmpty()) {
            return;
        }
        long now = server.overworld().getGameTime();
        Iterator<Ring> rings = RINGING.iterator();
        while (rings.hasNext()) {
            Ring ring = rings.next();
            wake(server, ring);
            if (ring.awaited.isEmpty() || now >= ring.until) {
                for (Awaited late : ring.awaited) {
                    // Woken, given a few seconds and still not there: revived under another
                    // name, or taken out of the world by something that left the note behind.
                    ring.missing.add(late.name());
                    PetRoster.of(server.overworld()).forget(ring.owner, late.pet());
                }
                ring.awaited.clear();
                report(server, ring);
                rings.remove();
            }
        }
    }

    /**
     * Keeps the chunks of the pets a ring is waiting on loaded, and brings over whichever
     * of them the game has read back since the last look.
     */
    private static void wake(MinecraftServer server, Ring ring) {
        ServerPlayer owner = server.getPlayerList().getPlayer(ring.owner);
        if (owner == null) {
            ring.awaited.clear();
            return;
        }
        Iterator<Awaited> waiting = ring.awaited.iterator();
        while (waiting.hasNext()) {
            Awaited awaited = waiting.next();
            ServerLevel level = server.getLevel(awaited.dimension());
            if (level == null) {
                ring.missing.add(awaited.name());
                waiting.remove();
                continue;
            }
            BlockPos pos = awaited.pos();
            level.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
            if (level.getEntity(awaited.pet()) instanceof AbstractPet pet) {
                if (bring(pet, owner)) {
                    ring.brought++;
                } else {
                    ring.missing.add(awaited.name());
                }
                waiting.remove();
            }
        }
    }

    /** Says once how the ring went: how many came, and who did not. */
    private static void report(MinecraftServer server, Ring ring) {
        ServerPlayer owner = server.getPlayerList().getPlayer(ring.owner);
        if (owner == null) {
            return;
        }
        owner.displayClientMessage(ring.brought > 0
            ? Component.translatable("message.chiikawa.pet_bell.came", ring.brought)
            : Component.translatable("message.chiikawa.pet_bell.nobody"), true);
        for (String name : ring.missing) {
            owner.displayClientMessage(
                Component.translatable("message.chiikawa.pet_bell.missing", name).withStyle(ChatFormatting.GRAY),
                false);
        }
    }

    /**
     * Brings one pet to its owner, across worlds if that is where it is.
     *
     * @return whether the pet is now beside the owner
     */
    private static boolean bring(AbstractPet pet, ServerPlayer owner) {
        ServerLevel home = owner.serverLevel();
        AbstractPet arrived = pet;
        if (pet.level() != home) {
            Entity moved = pet.teleport(new TeleportTransition(home, owner.position(), Vec3.ZERO,
                pet.getYRot(), pet.getXRot(), TeleportTransition.DO_NOTHING));
            if (!(moved instanceof AbstractPet crossed)) {
                return false;
            }
            arrived = crossed;
        }
        // A spot beside the owner if there is one; otherwise right where the owner stands,
        // because a bell that sometimes does nothing is worse than a pet underfoot.
        if (!arrived.teleportToOwner(home, owner)) {
            arrived.teleportTo(owner.getX(), owner.getY(), owner.getZ());
        }
        PetRoster.of(home).note(arrived);
        return true;
    }
}
