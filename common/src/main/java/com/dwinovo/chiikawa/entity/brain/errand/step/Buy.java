package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.block.ShopBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.entity.interact.PetInteractHandler;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.shop.ShopBasket;
import com.dwinovo.chiikawa.shop.ShopCatalog;
import com.dwinovo.chiikawa.shop.Wallet;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Buys one thing the pet likes at the counter it stands by, paying out of its own backpack. What it buys is
 * settled here rather than when the pet set off: a pet that was paid on the way, or spent on the way, buys what it
 * can afford when it gets there. Gives up if the shop is gone, if there is nothing there it wants and can afford,
 * or if its bag has no room for it. Done in the tick it buys, so it is never cut half way: the money is paid and
 * the goods are kept, or neither happened. It does not look how near the pet is; the walk before it is what brings
 * the pet to the counter.
 *
 * <p>Everything it buys is for itself, and goes into its backpack. What it gives its owner it picks from the
 * backpack, later and elsewhere.
 */
public final class Buy implements Step {
    private static final int HAPPY_PARTICLES = 8;

    private final BlockPos counter;

    /** @param counter the shop's block */
    public Buy(BlockPos counter) {
        this.counter = counter;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        if (!(pet.level() instanceof ServerLevel level)) {
            return StepStatus.gaveUp("gone");
        }
        Optional<ShopBlockEntity> shop = level.isLoaded(counter)
            ? level.getBlockEntity(counter, InitBlockEntities.SHOP.get()) : Optional.empty();
        if (shop.isEmpty()) {
            return StepStatus.gaveUp("gone");
        }
        Optional<ShopCatalog.Entry> wanted = ShopBasket.wants(PetPersonalities.of(pet.getType()), shop.get().catalog(),
            Wallet.count(pet.getBackpack()), ShopBasket.carriedIn(pet.getBackpack()), pet.getRandom());
        if (wanted.isEmpty()) {
            return StepStatus.gaveUp("nothing_to_buy");
        }
        ShopCatalog.Entry entry = wanted.get();
        ItemStack goods = new ItemStack(entry.item(), entry.count());
        // Paid for only once there is somewhere to put it: a pet with a full bag goes home
        // with its money rather than handing it over for nothing.
        if (!pet.getBackpack().canAddItem(goods)) {
            return StepStatus.gaveUp("no_room");
        }
        if (!Wallet.pay(pet.getBackpack(), entry.buy())) {
            return StepStatus.gaveUp("cannot_pay");
        }
        keep(pet, goods);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pet.getX(), pet.getY() + pet.getBbHeight() * 0.8, pet.getZ(),
            HAPPY_PARTICLES, 0.35, 0.3, 0.35, 0.0);
        handle.reaction(PetReaction.HAPPY);
        handle.say(VoiceMoment.SHOP);
        return StepStatus.DONE;
    }

    /**
     * Keeps what it bought for itself, and eats it there and then if it is food and the
     * pet is the worse for wear. A pet that buys a cake and carries it about while limping
     * is a pet that has misunderstood what money is for.
     */
    private static void keep(AbstractPet pet, ItemStack goods) {
        if (pet.getHealth() < pet.getMaxHealth() && goods.has(DataComponents.FOOD)) {
            pet.heal(PetInteractHandler.FEED_HEAL);
            return;
        }
        ItemStack remainder = pet.getBackpack().addItem(goods);
        if (!remainder.isEmpty()) {
            pet.spawnAtLocation(remainder);
        }
    }
}
