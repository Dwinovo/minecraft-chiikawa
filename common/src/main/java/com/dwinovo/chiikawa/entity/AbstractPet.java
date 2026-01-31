package com.dwinovo.chiikawa.entity;

import com.dwinovo.chiikawa.entity.interact.PetInteractHandler;
import com.dwinovo.chiikawa.entity.job.api.IPetJob;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.init.InitSensor;
import com.dwinovo.chiikawa.sound.PetSoundSet;
import com.dwinovo.chiikawa.utils.Utils;
import com.mojang.serialization.Dynamic;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Base implementation of a tamable pet with job, inventory, and ranged attack support.
 */
public class AbstractPet extends TamableAnimal implements GeoEntity, RangedAttackMob {
    /** Number of slots available in the pet's backpack inventory. */
    public static final int BACKPACK_SIZE = 16;
    private static final EntityDataAccessor<Byte> PET_MODE = SynchedEntityData.defineId(AbstractPet.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> PET_JOB = SynchedEntityData.defineId(AbstractPet.class, EntityDataSerializers.INT);
    private static final java.util.List<MemoryModuleType<?>> MEMORY_TYPES = java.util.List.of(
        MemoryModuleType.PATH,
        MemoryModuleType.DOORS_TO_CLOSE,
        MemoryModuleType.LOOK_TARGET,
        MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
        MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
        MemoryModuleType.WALK_TARGET,
        MemoryModuleType.ATTACK_TARGET,
        MemoryModuleType.ATTACK_COOLING_DOWN,
        MemoryModuleType.HURT_BY_ENTITY,
        MemoryModuleType.HOME,
        InitMemory.HARVEST_POS.get(),
        InitMemory.PLANT_POS.get(),
        InitMemory.CONTAINER_POS.get(),
        InitMemory.PICKABLE_ITEM.get()
    );
    private static final java.util.List<net.minecraft.world.entity.ai.sensing.SensorType<? extends net.minecraft.world.entity.ai.sensing.Sensor<? super AbstractPet>>> SENSOR_TYPES = java.util.List.of(
        net.minecraft.world.entity.ai.sensing.SensorType.HURT_BY,
        net.minecraft.world.entity.ai.sensing.SensorType.NEAREST_LIVING_ENTITIES,
        InitSensor.PET_ATTACKBLE_ENTITY_SENSOR.get(),
        InitSensor.PET_HARVEST_CROP_SENSOR.get(),
        InitSensor.PET_PLANT_CROP_SENSOR.get(),
        InitSensor.PET_CONTAINER_SENSOR.get(),
        InitSensor.PET_ITEM_ENTITY_SENSOR.get()
    );
    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final SimpleContainer backpack = new SimpleContainer(BACKPACK_SIZE);

    /**
     * Creates a new pet instance tied to its entity type and level.
     *
     * @param entityType the type definition for this pet
     * @param level the level context where the pet spawns and lives
     */
    protected AbstractPet(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        backpack.addListener(new ContainerListener() {
            @Override
            public void containerChanged(Container container) {
                refreshJobFromMainhand();
            }
        });
    }

    /**
     * Returns the container used as the pet's backpack.
     *
     * @return backpack inventory
     */
    public SimpleContainer getBackpack() {
        return backpack;
    }

    /**
     * @return current {@link PetMode} stored in the entity data
     */
    public PetMode getPetMode() {
        return PetMode.fromId(this.entityData.get(PET_MODE));
    }

    /**
     * Updates the mode displayed by the pet.
     *
     * @param mode new mode to persist
     */
    public void setPetMode(PetMode mode) {
        this.entityData.set(PET_MODE, (byte) mode.ordinal());
    }

    /**
     * @return the registered job id currently controlling pet behavior
     */
    public int getPetJobId() {
        return this.entityData.get(PET_JOB);
    }

    /**
     * Stores the provided job id into the pet's entity data.
     *
     * @param jobId identifier of the job to bind to
     */
    public void setPetJobId(int jobId) {
        this.entityData.set(PET_JOB, jobId);
    }

    /**
     * Re-evaluates the pet's active job based on the current main hand item and refreshes the brain if it changes.
     */
    public void refreshJobFromMainhand() {
        refreshJobFromMainhand(false);
    }

    private void refreshJobFromMainhand(boolean forceRefresh) {
        if (level().isClientSide()) {
            return;
        }

        IPetJob best = null;
        int bestPriority = Integer.MIN_VALUE;
        for (IPetJob job : InitRegistry.PET_JOB_REGISTRY) {
            if (!job.canAssume(this)) {
                continue;
            }
            int priority = job.getPriority();
            if (best == null || priority > bestPriority) {
                best = job;
                bestPriority = priority;
            }
        }
        if (best == null) {
            best = InitRegistry.NONE.get();
        }
        int newJobId = best.getId();
        if (newJobId != getPetJobId() || forceRefresh) {
            setPetJobId(newJobId);
            refreshBrain((ServerLevel) this.level());
        }
    }

    private void refreshBrain(ServerLevel serverLevelIn) {
        Brain<AbstractPet> brain = this.getBrain();
        brain.stopAll(serverLevelIn, this);
        // Copy the brain without behaviors.
        Brain<AbstractPet> newBrain = brain.copyWithoutBehaviors();
        this.brain = newBrain;
        // Initialize job behaviors.
        InitRegistry.getJobFromId(getPetJobId()).initBrain(this, newBrain);
    }

    @Override
    protected Brain.Provider<AbstractPet> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        Brain<AbstractPet> brain = (Brain<AbstractPet>) brainProvider().makeBrain(dynamic);
        InitRegistry.getJobFromId(getPetJobId()).initBrain(this, brain);
        return brain;
    }

    @Override
    protected void customServerAiStep() {
        InitRegistry.getJobFromId(getPetJobId()).tickBrain(this, this.getBrain());
        Brain<AbstractPet> brain = (Brain<AbstractPet>) getBrain();
        ServerLevel serverLevel = (ServerLevel) level();
        brain.tick(serverLevel, this);
        super.customServerAiStep();
    }

    public Brain<AbstractPet> getBrain() {
        return (Brain<AbstractPet>) super.getBrain();
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ItemStack weapon = getMainHandItem();
        if (!(weapon.getItem() instanceof ProjectileWeaponItem projectileWeapon)) {
            return;
        }
        ItemStack fallbackAmmo = Utils.getArrow(this);
        ItemStack ammo = ProjectileWeaponItem.getHeldProjectile(this, projectileWeapon.getAllSupportedProjectiles());
        if (ammo.isEmpty()) {
            ammo = fallbackAmmo;
        }
        if (ammo.isEmpty() || !(ammo.getItem() instanceof ArrowItem arrowItem)) {
            return;
        }
        AbstractArrow arrow = arrowItem.createArrow(serverLevel, ammo, this);
        Vec3 from = getEyePosition();
        Vec3 to = target.getEyePosition();
        Vec3 delta = to.subtract(from);
        float inaccuracy = 14 - serverLevel.getDifficulty().getId() * 4;
        arrow.shoot(delta.x, delta.y + Math.sqrt(delta.x * delta.x + delta.z * delta.z) * 0.2F, delta.z, 1.6F, inaccuracy);
        arrow.setOwner(this);
        serverLevel.addFreshEntity(arrow);
        boolean infinite = ammo.is(Items.ARROW)
            && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, weapon) > 0;
        if (!infinite) {
            ammo.shrink(1);
        }
        playAttackSound();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return backpack.getItem(0);
        }
        return super.getItemBySlot(slot);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            backpack.setItem(0, stack);
            refreshJobFromMainhand();
            return;
        }
        super.setItemSlot(slot, stack);
    }

    @Override
    public boolean canMate(Animal other) {
        return false; // Disable breeding.
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null; // Safety: no offspring.
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<AbstractPet> main = new AnimationController<>(this, "main", 5, state -> {
            RawAnimation builder = RawAnimation.begin();
            if (this.getPetMode() == PetMode.SIT) {
                builder.thenLoop("sit");
            } else if (state.isMoving()) {
                builder.thenLoop("run");
            } else {
                builder.thenLoop("idle");
            }
            state.setAndContinue(builder);
            return PlayState.CONTINUE;
        });

        AnimationController<AbstractPet> sub = new AnimationController<>(this, "sub", 1, state -> PlayState.STOP);

        main.triggerableAnim("use_mainhand", RawAnimation.begin().thenPlay("use_mainhand"))
            .triggerableAnim("sword_attack", RawAnimation.begin().thenPlay("sword_attack"));
        controllers.add(main, sub);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableInstanceCache;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PET_MODE, (byte) PetMode.FOLLOW.ordinal());
        this.entityData.define(PET_JOB, InitRegistry.NONE_ID);
    }

    @Override
    public boolean isFood(ItemStack arg0) {
        return false;
    }

    /**
     * Helper to get items as NonNullList from SimpleContainer for 1.20.1 compatibility.
     */
    private NonNullList<ItemStack> getBackpackItems() {
        NonNullList<ItemStack> items = NonNullList.withSize(backpack.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < backpack.getContainerSize(); i++) {
            items.set(i, backpack.getItem(i));
        }
        return items;
    }

    /**
     * Helper to set items from NonNullList to SimpleContainer for 1.20.1 compatibility.
     */
    private void setBackpackItems(NonNullList<ItemStack> items) {
        for (int i = 0; i < Math.min(items.size(), backpack.getContainerSize()); i++) {
            backpack.setItem(i, items.get(i));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        NonNullList<ItemStack> items = getBackpackItems();
        ContainerHelper.saveAllItems(tag.getCompound("Backpack") == null ? new CompoundTag() : tag.getCompound("Backpack"), items);
        tag.put("Backpack", ContainerHelper.saveAllItems(new CompoundTag(), items));
        tag.putInt("PetJob", getPetJobId());
        tag.putByte("PetMode", this.entityData.get(PET_MODE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Backpack", Tag.TAG_COMPOUND)) {
            NonNullList<ItemStack> items = NonNullList.withSize(backpack.getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound("Backpack"), items);
            setBackpackItems(items);
        }
        if (tag.contains("PetJob", Tag.TAG_INT)) {
            setPetJobId(tag.getInt("PetJob"));
        }
        if (tag.contains("PetMode", Tag.TAG_BYTE)) {
            this.entityData.set(PET_MODE, tag.getByte("PetMode"));
        }
        refreshJobFromMainhand(true);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult result = PetInteractHandler.handle(this, player, hand);
        if (result != InteractionResult.PASS) {
            return result;
        }
        return super.mobInteract(player, hand);
    }

    protected PetSoundSet getSoundSet() {
        return PetSoundSet.EMPTY;
    }

    public void playAttackSound() {
        if (!level().isClientSide()) {
            SoundEvent sound = getSoundSet().getAttackSound();
            if (sound != null) {
                playSound(sound, 1.0F, 1.0F);
            }
        }
    }

    public void playTameSound() {
        if (!level().isClientSide()) {
            SoundEvent sound = getSoundSet().getTameSound();
            if (sound != null) {
                playSound(sound, 1.0F, 1.0F);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        SoundEvent sound = getSoundSet().getHurtSound();
        return sound != null ? sound : super.getHurtSound(source);
    }

    @Override
    protected SoundEvent getDeathSound() {
        SoundEvent sound = getSoundSet().getDeathSound();
        return sound != null ? sound : super.getDeathSound();
    }

    @Override
    public float getVoicePitch() {
        return 1.0F;
    }
}
