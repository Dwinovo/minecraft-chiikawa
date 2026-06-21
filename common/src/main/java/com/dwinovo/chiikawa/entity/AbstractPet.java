package com.dwinovo.chiikawa.entity;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.api.AnimationLibrary;
import com.dwinovo.chiikawa.anim.api.ChiikawaAnimated;
import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.runtime.AnimationClock;
import com.dwinovo.chiikawa.anim.runtime.PetAnimator;
import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.anim.state.PetAnimContext;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.brain.handler.ArcherJobHandler;
import com.dwinovo.chiikawa.entity.brain.handler.FarmerJobHandler;
import com.dwinovo.chiikawa.entity.brain.handler.FencerJobHandler;
import com.dwinovo.chiikawa.entity.brain.handler.MusicianJobHandler;
import com.dwinovo.chiikawa.entity.brain.handler.NoneJobHandler;
import com.dwinovo.chiikawa.utils.BrainUtils;
import com.dwinovo.chiikawa.entity.interact.PetInteractHandler;
import com.dwinovo.chiikawa.entity.job.api.IPetJob;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.init.InitSensor;
import com.dwinovo.chiikawa.item.PetDollData;
import com.dwinovo.chiikawa.sound.PetSoundCue;
import com.dwinovo.chiikawa.sound.PetSoundKind;
import com.dwinovo.chiikawa.sound.PetSoundSet;
import com.dwinovo.chiikawa.utils.Utils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.ContainerHelper;
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
import net.minecraft.world.entity.schedule.Activity;
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
     * every client watcher, which dispatches a {@link PetAnimator#playOnce}
     * to the relevant controller.
     */
    private static final EntityDataAccessor<Integer> ANIM_TRIGGER = SynchedEntityData.defineId(AbstractPet.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> REACTION_TRIGGER = SynchedEntityData.defineId(AbstractPet.class, EntityDataSerializers.INT);
    /**
     * Synced byte storing the current {@link PetActivity} the pet is in.
     * Server-side Brain behaviors set/clear via {@link #setActivity}; the
     * client-side {@link com.dwinovo.chiikawa.anim.state.PetAnimationResolver}
     * reads it as the highest-priority animation candidate. This is the
     * "level state" channel — orthogonal to the edge-event triggers above.
     * {@link SynchedEntityData}'s delta-on-change suppresses no-op packets,
     * so server can call {@code setActivity} every tick if convenient.
     */
    private static final EntityDataAccessor<Byte> ACTIVITY = SynchedEntityData.defineId(AbstractPet.class, EntityDataSerializers.BYTE);

    /** Legacy animation-id namespace for {@link #ANIM_TRIGGER}'s low byte. */
    public static final int TRIGGER_NONE         = 0;
    public static final int TRIGGER_USE_MAINHAND = 1;
    public static final int TRIGGER_SWORD_ATTACK = 2;

    /** Controller name receiving action triggers — must match {@link com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer#CONTROLLER_ACTION}. */
    private static final String ACTION_CONTROLLER = "action";
    /** Controller name receiving reaction triggers — must match {@link com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer#CONTROLLER_REACTION}. */
    private static final String REACTION_CONTROLLER = "reaction";
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
        InitMemory.PICKABLE_ITEM.get(),
        InitMemory.REQUESTED_COMMAND.get(),
        InitMemory.REQUESTED_MUSIC_TRACK.get(),
        InitMemory.MUSICIAN_LAST_MUSIC_SIGNATURE.get()
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
    private static final Brain.Provider<AbstractPet> BRAIN_PROVIDER =
        Brain.<AbstractPet>provider(MEMORY_TYPES, SENSOR_TYPES, pet -> java.util.List.of());
    /** Lazily allocated on first client-side read; server instances pay nothing. */
    private PetAnimator petAnimator;
    /** Last {@link #ANIM_TRIGGER} sequence number this client handled. Server copy is unused. */
    private int lastSeenTriggerSeq;
    /** Last {@link #REACTION_TRIGGER} sequence number this client handled. Server copy is unused. */
    private int lastSeenReactionSeq;
    private final SimpleContainer backpack = new SimpleContainer(BACKPACK_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            AbstractPet.this.refreshJobFromMainhand();
        }
    };

    protected AbstractPet(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
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

    /**
     * Pick the highest-priority job whose tag matches the pet's mainhand
     * item, and write the result to {@link #PET_JOB}. The brain is
     * <em>not</em> rebuilt — every job's activities live on a single
     * static brain since {@link #makeBrain}, so a job change is just a
     * synced byte flip; the next {@link #customServerAiStep} picks up the
     * new job's {@code tickBrain} and selects different activities.
     *
     * <p>{@code forceRefresh} is reserved for callers that want to re-emit
     * the same job id (e.g. to reset an override). It currently has no
     * side effect since there is no brain rebuild to trigger.
     */
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
            if (newJobId != getPetJobId()) {
                InitRegistry.getJobFromId(getPetJobId()).onDeactivated(this, getBrain());
            }
            setPetJobId(newJobId);
        }
    }

    /**
     * Build the entity's brain once, registering <em>all</em> job activities
     * up front. Activities don't run unless the per-tick activity selector
     * (see {@link #customServerAiStep}) chooses them, so the unused
     * activities cost only their flat memory footprint.
     *
     * <p>This avoids the prior "rebuild on job change" pattern, which had
     * three problems: (1) {@code brain.stopAll} interrupted in-flight
     * behaviors mid-frame causing animation glitches, (2) {@code brain.pack}
     * preserved memories but not behavior internal state, (3) it diverged
     * from Mojang's static-registration convention (Villager registers
     * profession-aware behaviors once and lets activity selection do the
     * filtering).
     *
     * <p>Future jobs (or manual job switching) just append a new
     * {@code <Job>JobHandler.registerActivities(brain)} call here and a
     * branch in {@link #customServerAiStep}'s job dispatch — no plumbing
     * elsewhere needs to touch.
     */
    @Override
    protected Brain<AbstractPet> makeBrain(Brain.Packed packedBrain) {
        Brain<AbstractPet> brain = BRAIN_PROVIDER.makeBrain(this, packedBrain);

        // Universal tasks present in every brain.
        BrainUtils.addCoreTasks(brain);
        BrainUtils.addIdleTasks(brain);

        // Each job's activities — registered once, dormant until that job's
        // tickBrain selects them.
        FarmerJobHandler.registerActivities(brain);
        FencerJobHandler.registerActivities(brain);
        ArcherJobHandler.registerActivities(brain);
        MusicianJobHandler.registerActivities(brain);
        NoneJobHandler.registerActivities(brain); // no-op today; placeholder for symmetry

        brain.setCoreActivities(java.util.Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
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

    /**
     * Widen vanilla's protected {@code getFluidJumpThreshold} to public so the
     * brain-side {@link com.dwinovo.chiikawa.entity.brain.task.tameable.FloatBehavior}
     * — which lives in a different package — can read it to decide when to bob
     * up to the water surface. Behavior is unchanged; this only relaxes access.
     */
    @Override
    public double getFluidJumpThreshold() {
        return super.getFluidJumpThreshold();
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
    public PetAnimContext getAnimContext(float walkSpeed) {
        return PetAnimContext.base(getPetMode(), getPetJobId(), walkSpeed, getActivity());
    }

    /** Current code-bounded loop activity (level state). Synced both directions. */
    public PetActivity getActivity() {
        return PetActivity.fromNetworkId(this.entityData.get(ACTIVITY));
    }

    /**
     * Set the level-state activity. Server-side only — calling on the client
     * is a no-op (synced data writes from the client are dropped by
     * {@link SynchedEntityData}). Equality-guarded: re-setting the same value
     * does not generate a network packet, so callers may invoke this every
     * tick without traffic concerns.
     */
    public void setActivity(PetActivity activity) {
        if (level().isClientSide()) return;
        byte id = (byte) activity.networkId();
        if (this.entityData.get(ACTIVITY) != id) {
            this.entityData.set(ACTIVITY, id);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PET_MODE, (byte) PetMode.FOLLOW.ordinal());
        builder.define(PET_JOB, InitRegistry.NONE_ID);
        builder.define(ANIM_TRIGGER, 0);
        builder.define(REACTION_TRIGGER, 0);
        builder.define(ACTIVITY, (byte) PetActivity.NONE.networkId());
    }

    /**
     * Bumps the synced trigger so all client watchers fire {@code name} once
     * on the pet's animator. Server-only; calling on the client is a no-op
     * (the value would not propagate). Unknown animation names are silently ignored.
     *
     * @param name legacy animation name
     */
    @Deprecated(forRemoval = false)
    public void triggerAnim(String name) {
        triggerAction(PetAction.fromLegacyAnimationName(name));
    }

    /**
     * Bumps the synced action trigger so clients can choose the best available
     * animation candidate for the semantic action.
     *
     * @param action semantic action event
     */
    public void triggerAction(PetAction action) {
        if (level().isClientSide()) return;
        if (action == null || action == PetAction.NONE) return;
        bumpTrigger(ANIM_TRIGGER, action.networkId());
    }

    /**
     * Bumps the synced reaction trigger so clients can play short emotional
     * feedback on its own layer.
     *
     * @param reaction semantic reaction event
     */
    public void triggerReaction(PetReaction reaction) {
        if (level().isClientSide()) return;
        if (reaction == null || reaction == PetReaction.NONE) return;
        bumpTrigger(REACTION_TRIGGER, reaction.networkId());
    }

    private void bumpTrigger(EntityDataAccessor<Integer> accessor, int id) {
        int packed = entityData.get(accessor);
        int seq = ((packed >>> 8) + 1) & 0xFFFFFF;
        // Avoid the wrap-to-zero ambiguity (seq 0 = "never triggered").
        if (seq == 0) seq = 1;
        entityData.set(accessor, (seq << 8) | (id & 0xFF));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!level().isClientSide()) return;
        if (ANIM_TRIGGER.equals(key)) {
            handleActionTrigger();
        } else if (REACTION_TRIGGER.equals(key)) {
            handleReactionTrigger();
        }
    }

    private void handleActionTrigger() {
        int packed = entityData.get(ANIM_TRIGGER);
        int seq = packed >>> 8;
        if (seq == 0 || seq == lastSeenTriggerSeq) return;
        lastSeenTriggerSeq = seq;
        PetAction action = PetAction.fromNetworkId(packed & 0xFF);
        if (action == PetAction.NONE) return;
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(getType());
        BakedAnimation anim = firstAvailableActionAnimation(typeId, action);
        if (anim != null) {
            getPetAnimator().playOnce(ACTION_CONTROLLER, anim, AnimationClock.fromTicks(tickCount, 0f));
        } else {
            Constants.LOG.warn("[chiikawa-anim] no baked animation for action '{}' on {}", action, typeId);
        }
    }

    private void handleReactionTrigger() {
        int packed = entityData.get(REACTION_TRIGGER);
        int seq = packed >>> 8;
        if (seq == 0 || seq == lastSeenReactionSeq) return;
        lastSeenReactionSeq = seq;
        PetReaction reaction = PetReaction.fromNetworkId(packed & 0xFF);
        if (reaction == PetReaction.NONE) return;
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(getType());
        BakedAnimation anim = firstAvailableReactionAnimation(typeId, reaction);
        if (anim != null) {
            getPetAnimator().playOnce(REACTION_CONTROLLER, anim, AnimationClock.fromTicks(tickCount, 0f));
        }
    }

    private BakedAnimation firstAvailableActionAnimation(Identifier typeId, PetAction action) {
        for (String name : action.animationCandidates()) {
            BakedAnimation anim = AnimationLibrary.get(
                    Identifier.fromNamespaceAndPath(typeId.getNamespace(), typeId.getPath() + "/" + name));
            if (anim != null) {
                return anim;
            }
        }
        return null;
    }

    private BakedAnimation firstAvailableReactionAnimation(Identifier typeId, PetReaction reaction) {
        for (String name : reaction.animationCandidates()) {
            BakedAnimation anim = AnimationLibrary.get(
                    Identifier.fromNamespaceAndPath(typeId.getNamespace(), typeId.getPath() + "/" + name));
            if (anim != null) {
                return anim;
            }
        }
        return null;
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
        playPetSound(PetSoundKind.ATTACK, getSoundSet().getAttackCue());
    }

    public void playTameSound() {
        playPetSound(PetSoundKind.TAME, getSoundSet().getTameCue());
    }

    protected boolean playPetSound(PetSoundKind kind, PetSoundCue cue) {
        if (level().isClientSide() || cue == null || isPetSoundSuppressed(kind)) {
            return false;
        }

        SoundEvent sound = cue.resolve();
        if (sound == null) {
            return false;
        }

        playSound(sound, cue.volume(), cue.samplePitch(getRandom()));
        return true;
    }

    protected boolean isPetSoundSuppressed(PetSoundKind kind) {
        return kind.isDaily() && getActivity().suppressesDailyPetSounds();
    }

    @Override
    public int getAmbientSoundInterval() {
        return getSoundSet().getAmbientSoundInterval();
    }

    @Override
    public void playAmbientSound() {
        playPetSound(PetSoundKind.AMBIENT, getSoundSet().pickAmbientCue(getRandom()));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if (!level().isClientSide()) {
            triggerReaction(PetReaction.HURT);
        }
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
