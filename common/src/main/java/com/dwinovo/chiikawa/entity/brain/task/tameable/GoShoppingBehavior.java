package com.dwinovo.chiikawa.entity.brain.task.tameable;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.block.ShopBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.shop.ShopBasket;
import com.dwinovo.chiikawa.shop.ShopCatalog;
import com.dwinovo.chiikawa.shop.Wallet;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Walks to the nearest shop and buys one thing the pet likes, paying out of its own
 * backpack.
 *
 * <p>What it buys is settled at the counter rather than when it sets off: a pet that was
 * paid on the way, or spent on the way, should buy what it can afford when it gets there.
 * Afterwards it rests from shopping for a while, so a pet with a full purse does not stand
 * at the counter buying the shop out one cookie at a time.
 */
public class GoShoppingBehavior extends Behavior<AbstractPet> {
    /** How long a pet leaves the shop alone after buying something. */
    private static final int REST_TICKS = 600;
    private static final float SPEED = 0.7F;
    /** How close the walk gets. Nearer than {@link #BUY_DISTANCE_SQR}, so arriving is enough. */
    private static final int ARRIVE_DISTANCE = 2;
    private static final double BUY_DISTANCE_SQR = 3.0 * 3.0;
    private static final int HAPPY_PARTICLES = 8;
    /** Long enough to walk across a garden, short enough that a stuck pet gives up. */
    private static final int GIVE_UP_TICKS = 1200;

    private boolean bought;

    public GoShoppingBehavior() {
        super(ImmutableMap.of(
            InitMemory.NEAREST_SHOP.get(), MemoryStatus.VALUE_PRESENT,
            InitMemory.SHOP_COOLDOWN.get(), MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED
        ), GIVE_UP_TICKS);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet pet) {
        return wants(level, pet).isPresent();
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        bought = false;
        shop(level, pet).ifPresent(shop ->
            BehaviorUtils.setWalkAndLookTargetMemories(pet, shop.getBlockPos(), SPEED, ARRIVE_DISTANCE));
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return !bought && wants(level, pet).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        Optional<ShopBlockEntity> shop = shop(level, pet);
        if (shop.isEmpty()) {
            return;
        }
        BlockPos counter = shop.get().getBlockPos();
        if (pet.distanceToSqr(Vec3.atCenterOf(counter)) > BUY_DISTANCE_SQR) {
            return;
        }
        buy(level, pet, shop.get().catalog());
    }

    @Override
    protected void stop(ServerLevel level, AbstractPet pet, long gameTime) {
        pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        if (bought) {
            pet.getBrain().setMemoryWithExpiry(InitMemory.SHOP_COOLDOWN.get(), Unit.INSTANCE, REST_TICKS);
        }
    }

    /** Hands the money over and takes the goods. Nothing happens unless both can. */
    private void buy(ServerLevel level, AbstractPet pet, ShopCatalog catalog) {
        Optional<ShopCatalog.Entry> wanted = ShopBasket.wants(
            PetPersonalities.of(pet.getType()), catalog, Wallet.count(pet.getBackpack()), pet.getRandom());
        if (wanted.isEmpty()) {
            return;
        }
        ShopCatalog.Entry entry = wanted.get();
        ItemStack bag = new ItemStack(entry.item());
        // Paid for only once there is somewhere to put it: a pet with a full bag goes home
        // with its money rather than handing it over for nothing.
        if (!pet.getBackpack().canAddItem(bag) || !Wallet.pay(pet.getBackpack(), entry.buy())) {
            return;
        }
        ItemStack remainder = pet.getBackpack().addItem(bag);
        if (!remainder.isEmpty()) {
            pet.spawnAtLocation(remainder);
        }
        bought = true;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pet.getX(), pet.getY() + pet.getBbHeight() * 0.8, pet.getZ(),
            HAPPY_PARTICLES, 0.35, 0.3, 0.35, 0.0);
        pet.triggerReaction(PetReaction.HAPPY);
    }

    /** What the pet would buy at the shop it remembers, if anything. */
    private static Optional<ShopCatalog.Entry> wants(ServerLevel level, AbstractPet pet) {
        return shop(level, pet).flatMap(shop -> ShopBasket.wants(
            PetPersonalities.of(pet.getType()), shop.catalog(), Wallet.count(pet.getBackpack()), pet.getRandom()));
    }

    private static Optional<ShopBlockEntity> shop(ServerLevel level, AbstractPet pet) {
        return pet.getBrain().getMemory(InitMemory.NEAREST_SHOP.get())
            .filter(level::isLoaded)
            .flatMap(pos -> level.getBlockEntity(pos, InitBlockEntities.SHOP.get()));
    }
}
