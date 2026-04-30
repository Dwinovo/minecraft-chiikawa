package com.dwinovo.chiikawa.entity;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.api.AnimationLibrary;
import com.dwinovo.chiikawa.anim.api.ChiikawaAnimated;
import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.runtime.PetAnimator;
import com.dwinovo.chiikawa.entity.interact.PetInteractHandler;
import com.dwinovo.chiikawa.entity.job.api.IPetJob;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.init.InitSensor;
import com.dwinovo.chiikawa.item.PetDollData;
import com.dwinovo.chiikawa.sound.PetSoundSet;
import com.dwinovo.chiikawa.utils.Utils;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class AbstractPet extends TamableAnimal implements RangedAttackMob, ChiikawaAnimated {
    public static final int BACKPACK_SIZE = 16;
    private static final EntityDataAccessor<Byte> PET_MODE = SynchedEntityData.defineId(AbstractPet.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> PET_JOB = SynchedEntityData.defineId(AbstractPet.class, EntityDataSerializers.INT);
    /**
     * Synced packed integer that drives one-shot animation triggers across the
     * server/client boundary. Layout: high 24 bits = monotonic sequence
     * counter, low 8 bits = animation id (see {@code TRIGGER_*}). Bumping the
     * sequence on the server causes {@link #onSyncedDataUpdated} to fire on
     * every client watcher, which in turn calls {@link PetAnimator#trigger}.
     */
    private static final EntityDataAccessor<Integer> ANIM_TRIGGER = SynchedEntityData.defineId(AbstractPet.class, EntityDataSerializers.INT);

    /** Animation-id namespace for {@link #ANIM_TRIGGER}'s low byte. */
    public static final int TRIGGER_NONE         = 0;
    public static final int TRIGGER_USE_MAINHAND = 1;
    public static final int TRIGGER_SWORD_ATTACK = 2;

    /** Layer used by triggered one-shots; main loop owns layer 0. */
    private static final int TRIGGER_LAYER = 1;
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
    /** Lazily allocated on first client-side read; server instances pay nothing. */
    private PetAnimator petAnimator;
    /** Last {@link #ANIM_TRIGGER} sequence number this client handled. Server copy is unused. */
    private int lastSeenTriggerSeq;
    private final SimpleContainer backpack = new SimpleContainer(BACKPACK_SIZE);

    protected AbstractPet(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        backpack.addListener(new ContainerListener() {
            @Override
            public void containerChanged(Container container) {
                refreshJobFromMainhand();
            }
        });
    }

    public SimpleContainer getBackpack() {
        return backpack;
    }

    public PetMode getPetMode() {
        return PetMode.fromId(this.entityData.get(PET_MODE));
    }

    public void setPetMode(PetMode mode) {
        this.entityData.set(PET_MODE, (byte) mode.ordinal());
    }

    public int getPetJobId() {
        return this.entityData.get(PET_JOB);
    }

    public void setPetJobId(int jobId) {
        this.entityData.set(PET_JOB, jobId);
    }

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
    protected void customServerAiStep(ServerLevel level) {
        InitRegistry.getJobFromId(getPetJobId()).tickBrain(this, this.getBrain());
        Brain<AbstractPet> brain = (Brain<AbstractPet>) getBrain();
        brain.tick(level, this);
        super.customServerAiStep(level);
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
        AbstractArrow arrow = arrowItem.createArrow(serverLevel, ammo, this, weapon);
        Vec3 from = getEyePosition();
        Vec3 to = target.getEyePosition();
        Vec3 delta = to.subtract(from);
        float inaccuracy = 14 - serverLevel.getDifficulty().getId() * 4;
        arrow.shoot(delta.x, delta.y + Math.sqrt(delta.x * delta.x + delta.z * delta.z) * 0.2F, delta.z, 1.6F, inaccuracy);
        arrow.setOwner(this);
        serverLevel.addFreshEntity(arrow);
        boolean infinite = ammo.is(Items.ARROW)
            && EnchantmentHelper.getItemEnchantmentLevel(
                serverLevel.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.INFINITY),
                weapon
            ) > 0;
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
    public PetAnimator getPetAnimator() {
        if (petAnimator == null) {
            petAnimator = new PetAnimator();
        }
        return petAnimator;
    }

    @Override
    public String getMainAnimationName(float walkSpeed) {
        if (getPetMode() == PetMode.SIT) return "sit";
        // Small movement threshold: the smoothed walkAnimation.speed decays
        // exponentially toward 0, so
        // a `> 0` check would latch the run state forever.
        if (walkSpeed > 0.15f) return "run";
        return "idle";
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PET_MODE, (byte) PetMode.FOLLOW.ordinal());
        builder.define(PET_JOB, InitRegistry.NONE_ID);
        builder.define(ANIM_TRIGGER, 0);
    }

    /**
     * Bumps the synced trigger so all client watchers fire {@code name} once
     * on the pet's animator. Server-only; calling on the client is a no-op
     * (the value would not propagate). Replaces the old renderer-side
     * trigger hook for AI behaviors. Unknown animation
     * names are silently ignored.
     */
    public void triggerAnim(String name) {
        if (level().isClientSide()) return;
        int id = animIdFor(name);
        if (id == TRIGGER_NONE) return;
        int packed = entityData.get(ANIM_TRIGGER);
        int seq = ((packed >>> 8) + 1) & 0xFFFFFF;
        // Avoid the wrap-to-zero ambiguity (seq 0 = "never triggered").
        if (seq == 0) seq = 1;
        entityData.set(ANIM_TRIGGER, (seq << 8) | (id & 0xFF));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!level().isClientSide() || !ANIM_TRIGGER.equals(key)) return;
        int packed = entityData.get(ANIM_TRIGGER);
        int seq = packed >>> 8;
        if (seq == 0 || seq == lastSeenTriggerSeq) return;
        lastSeenTriggerSeq = seq;
        String name = animNameFor(packed & 0xFF);
        if (name == null) return;
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(getType());
        BakedAnimation anim = AnimationLibrary.get(
                Identifier.fromNamespaceAndPath(typeId.getNamespace(), typeId.getPath() + "/" + name));
        if (anim != null) {
            getPetAnimator().trigger(TRIGGER_LAYER, anim);
        } else {
            Constants.LOG.warn("[chiikawa-anim] no baked animation for trigger '{}' on {}", name, typeId);
        }
    }

    private static int animIdFor(String name) {
        return switch (name) {
            case "use_mainhand" -> TRIGGER_USE_MAINHAND;
            case "sword_attack" -> TRIGGER_SWORD_ATTACK;
            default -> TRIGGER_NONE;
        };
    }

    private static String animNameFor(int id) {
        return switch (id) {
            case TRIGGER_USE_MAINHAND -> "use_mainhand";
            case TRIGGER_SWORD_ATTACK -> "sword_attack";
            default -> null;
        };
    }

    @Override
    public boolean isFood(ItemStack arg0) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        ContainerHelper.saveAllItems(output.child("Backpack"), backpack.getItems());
        output.putInt("PetJob", getPetJobId());
        output.putByte("PetMode", this.entityData.get(PET_MODE));
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.child("Backpack").ifPresent(backpackInput -> ContainerHelper.loadAllItems(backpackInput, backpack.getItems()));
        input.getInt("PetJob").ifPresent(this::setPetJobId);
        this.entityData.set(PET_MODE, input.getByteOr("PetMode", this.entityData.get(PET_MODE)));
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

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);

        Item dollItem = getReviveDollItem();
        if (dollItem == null) {
            return;
        }

        ItemStack dollStack = new ItemStack(dollItem);
        PetDollData.writePetToDoll(dollStack, this);
        this.spawnAtLocation(level, dollStack);
    }

    protected Item getReviveDollItem() {
        return null;
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
