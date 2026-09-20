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
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.utils.BrainUtils;
import com.dwinovo.chiikawa.entity.interact.PetInteractHandler;
import com.dwinovo.chiikawa.entity.job.api.PetCapability;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.init.InitSensor;
import com.dwinovo.chiikawa.item.PetDollData;
import com.dwinovo.chiikawa.sound.PetSoundCue;
import com.dwinovo.chiikawa.sound.PetSoundKind;
import com.dwinovo.chiikawa.sound.PetSoundSet;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.task.TaskTracker;
import com.dwinovo.chiikawa.utils.Utils;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

public class AbstractPet extends TamableAnimal implements RangedAttackMob, ChiikawaAnimated {
    /**
     * The pet's own pockets: slot 0 is what it holds, slot 1 is what it wears, and the
     * rest is room for things. The bag's own ten come after them.
     */
    public static final int BACKPACK_SIZE = 16;
    /** How much quicker an eager pet moves. */
    private static final double EAGER_SPEED_BONUS = 0.3;
    private static final ResourceLocation EAGER_SPEED_ID =
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "eager");

    /** Slot the held tool lives in; see {@link #getItemBySlot}. */
    public static final int MAINHAND_SLOT = 0;
    /** Slot the worn bag lives in, for the same reason the tool lives in a slot. */
    public static final int BAG_SLOT = 1;
    /** How much room a bear backpack adds. */
    public static final int BAG_SIZE = 10;
    /** Everything: pockets plus whatever the bag adds when one is worn. */
    public static final int FULL_BACKPACK_SIZE = BACKPACK_SIZE + BAG_SIZE;
    /** Synced {@link PetDirective} ordinal; same slot and {@code PetMode} save key as 0.0.9. */
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
    /** The slip the pet carries, empty when none. Synced so the client can show and hang it. */
    private static final EntityDataAccessor<CompoundTag> TASK = SynchedEntityData.defineId(AbstractPet.class, EntityDataSerializers.COMPOUND_TAG);
    /** Id of the intent the pet is following, empty when none. Synced for the backpack screen. */
    private static final EntityDataAccessor<String> INTENT = SynchedEntityData.defineId(AbstractPet.class, EntityDataSerializers.STRING);

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
        InitMemory.WEED_POS.get(),
        InitMemory.MUSHROOM_POS.get(),
        InitMemory.PICKABLE_ITEM.get(),
        InitMemory.MUSICIAN_LAST_MUSIC_SIGNATURE.get(),
        InitMemory.NEAREST_BOARD.get(),
        InitMemory.TAKE_TASK_COOLDOWN.get(),
        InitMemory.NEAREST_SHOP.get(),
        InitMemory.SHOP_COOLDOWN.get(),
        InitMemory.CURRENT_INTENT.get(),
        InitMemory.INTENT_REEVALUATE.get(),
        InitMemory.INTENT_SWITCH_LOG.get()
    );
    private static final java.util.List<net.minecraft.world.entity.ai.sensing.SensorType<? extends net.minecraft.world.entity.ai.sensing.Sensor<? super AbstractPet>>> SENSOR_TYPES = java.util.List.of(
        net.minecraft.world.entity.ai.sensing.SensorType.HURT_BY,
        net.minecraft.world.entity.ai.sensing.SensorType.NEAREST_LIVING_ENTITIES,
        InitSensor.PET_ATTACKBLE_ENTITY_SENSOR.get(),
        InitSensor.PET_FARMER_WORK_SENSOR.get(),
        InitSensor.PET_ITEM_ENTITY_SENSOR.get(),
        InitSensor.PET_PLACES_SENSOR.get()
    );
    /** Lazily allocated on first client-side read; server instances pay nothing. */
    private PetAnimator petAnimator;
    /** Last {@link #ANIM_TRIGGER} sequence number this client handled. Server copy is unused. */
    private int lastSeenTriggerSeq;
    /** Last {@link #REACTION_TRIGGER} sequence number this client handled. Server copy is unused. */
    private int lastSeenReactionSeq;
    /** Bought for the owner and not yet handed over; see {@link #setPendingGift}. */
    private ItemStack pendingGift = ItemStack.EMPTY;
    /** Ticks of eagerness left after a good meal; see {@link #feedDish}. */
    private int eagerTicks;

    private final SimpleContainer backpack = new SimpleContainer(FULL_BACKPACK_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            AbstractPet.this.refreshJobFromMainhand();
        }
    };

    protected AbstractPet(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        // Match vanilla FloatGoal's constructor: make pathfinding float-aware so the
        // brain-side FloatBehavior bobs the pet to the surface smoothly instead of
        // sink-fighting the jump (which caused the repeated bouncing). One-time setup.
        this.getNavigation().setCanFloat(true);
    }

    public SimpleContainer getBackpack() {
        return backpack;
    }

    /**
     * Computes a block's loot-table drops (using {@code tool} as the breaking
     * tool, so enchantments like Fortune apply) and inserts them straight into
     * this pet's backpack. Anything that doesn't fit pops onto the ground at
     * {@code pos}. Mirrors the vanilla block-break drop path so harvested
     * produce never has to be picked up off the floor.
     *
     * @param state the block being broken
     * @param level the server level
     * @param pos the block position
     * @param blockEntity the block's block entity, or {@code null}
     * @param tool the tool used (affects loot); may be empty
     */
    public void dropResourcesToPetInv(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity, ItemStack tool) {
        for (ItemStack drop : Block.getDrops(state, level, pos, blockEntity, this, tool)) {
            ItemStack remainder = backpack.addItem(drop);
            if (!remainder.isEmpty()) {
                Block.popResource(level, pos, remainder);
            }
        }
        state.spawnAfterBreak(level, pos, tool, true);
    }

    /** How long a work target stays blacklisted after being found unreachable. */
    private static final long REACH_BLACKLIST_TICKS = 200L;
    /** Transient (not saved): work-target positions recently found unreachable → expiry game-time. */
    private final java.util.Map<Long, Long> reachBlacklist = new java.util.HashMap<>();

    /**
     * Marks a work-target position as currently unreachable so the farmer sensor
     * stops re-selecting it for a while. This keeps the (expensive) reachability
     * pathfind out of the per-candidate scan: the sensor picks targets by cheap
     * checks, the walk-to behavior does the single pathfind, and a failure lands
     * the position here so the next scan moves on.
     * @param pos the unreachable position
     */
    public void blacklistUnreachable(BlockPos pos) {
        if (reachBlacklist.size() > 64) {
            reachBlacklist.clear();
        }
        reachBlacklist.put(pos.asLong(), level().getGameTime() + REACH_BLACKLIST_TICKS);
    }

    /**
     * @param pos a candidate position
     * @return whether it's currently blacklisted as unreachable (expired entries are pruned)
     */
    public boolean isReachBlacklisted(BlockPos pos) {
        Long expiry = reachBlacklist.get(pos.asLong());
        if (expiry == null) {
            return false;
        }
        if (level().getGameTime() >= expiry) {
            reachBlacklist.remove(pos.asLong());
            return false;
        }
        return true;
    }

    /** Random landing spots tried per {@link #teleportToOwner} call before giving up. */
    private static final int TELEPORT_ATTEMPTS = 10;

    /**
     * Teleports this pet to a random safe spot within a few blocks of {@code owner}.
     * Shared by the in-AI follow behavior and the pre-chunk-unload hook, so callers
     * are responsible for deciding whether the pet should follow at all.
     *
     * <p>A spot is only used when its block column is loaded, it stands on solid
     * ground that isn't harmful, the pet's box there has no collision, no liquid
     * and no harmful blocks, and it isn't right on top of the owner. When no spot
     * qualifies the pet stays where it is.
     *
     * @param level the pet's (and owner's) server level
     * @param owner the entity to teleport next to
     * @return whether the pet was moved
     */
    public boolean teleportToOwner(ServerLevel level, LivingEntity owner) {
        BlockPos base = owner.blockPosition();
        for (int i = 0; i < TELEPORT_ATTEMPTS; i++) {
            int dx = Mth.nextInt(getRandom(), -3, 3);
            int dy = Mth.nextInt(getRandom(), -1, 1);
            int dz = Mth.nextInt(getRandom(), -3, 3);
            if (tryTeleportNear(level, owner, base.offset(dx, dy, dz))) {
                return true;
            }
        }
        return false;
    }

    private boolean tryTeleportNear(ServerLevel level, LivingEntity owner, BlockPos start) {
        double x = start.getX() + 0.5;
        double z = start.getZ() + 0.5;
        // Don't land on the owner's head/feet.
        if (Math.abs(x - owner.getX()) < 2.0 && Math.abs(z - owner.getZ()) < 2.0) {
            return false;
        }
        if (!level.isLoaded(start)) {
            return false;
        }
        // Drop down onto the first solid block below the candidate.
        BlockPos feet = start;
        BlockState ground;
        while (true) {
            if (feet.getY() <= level.getMinY()) {
                return false;
            }
            BlockPos below = feet.below();
            ground = level.getBlockState(below);
            if (ground.isSolidRender()) {
                break;
            }
            feet = below;
        }
        if (isHarmfulTeleportBlock(ground)) {
            return false;
        }
        double y = feet.getY();
        AABB box = getBoundingBox().move(x - getX(), y - getY(), z - getZ());
        if (!level.noCollision(this, box)
                || level.containsAnyLiquid(box)
                || level.getBlockStatesIfLoaded(box).anyMatch(AbstractPet::isHarmfulTeleportBlock)) {
            return false;
        }
        teleportTo(x, y, z);
        resetFallDistance();
        getNavigation().stop();
        // Forget where we were heading so the pet doesn't try to walk back.
        Brain<AbstractPet> brain = getBrain();
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
        brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        brain.eraseMemory(MemoryModuleType.PATH);
        return true;
    }

    private static boolean isHarmfulTeleportBlock(BlockState state) {
        return state.is(Blocks.MAGMA_BLOCK)
            || state.is(BlockTags.FIRE)
            || state.is(BlockTags.CAMPFIRES)
            || state.is(Blocks.CACTUS)
            || state.is(Blocks.SWEET_BERRY_BUSH)
            || state.is(Blocks.POWDER_SNOW)
            || state.is(Blocks.LAVA_CAULDRON)
            || state.getFluidState().is(FluidTags.LAVA);
    }

    /**
     * @return current {@link PetDirective} stored in the entity data
     */
    public PetDirective getPetDirective() {
        return PetDirective.fromId(this.entityData.get(PET_MODE));
    }

    /**
     * Gives the pet a new directive and the home that goes with it, see {@link #settleHome}.
     *
     * @param directive new directive to persist
     */
    public void setPetDirective(PetDirective directive) {
        this.entityData.set(PET_MODE, (byte) directive.ordinal());
        settleHome();
        IntentSelector.requestReevaluate(this);
    }

    /**
     * Gives the pet the home its directive calls for, where it stands now. A free pet
     * (as is every wild pet with a home) makes this spot its home, the center it roams
     * around; a following pet has no home; a staying pet keeps the home it had. Called
     * whenever the directive is given and when a pet comes back to life somewhere else.
     */
    public void settleHome() {
        switch (getPetDirective()) {
            case FREE -> getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(level().dimension(), blockPosition()));
            case FOLLOW -> getBrain().eraseMemory(MemoryModuleType.HOME);
            case STAY -> {
            }
        }
    }

    /**
     * @return the slip the pet carries; readable on both sides
     */
    public Optional<PetTask> getTask() {
        CompoundTag tag = this.entityData.get(TASK);
        return tag.isEmpty() ? Optional.empty() : PetTask.CODEC.parse(NbtOps.INSTANCE, tag).result();
    }

    /**
     * @param task the slip the pet now carries, {@code null} for none
     */
    public void setTask(@Nullable PetTask task) {
        this.entityData.set(TASK, task == null
            ? new CompoundTag()
            : (CompoundTag) PetTask.CODEC.encodeStart(NbtOps.INSTANCE, task).getOrThrow());
    }

    /**
     * @return what the pet bought for its owner and has not handed over yet
     */
    public ItemStack getPendingGift() {
        return pendingGift;
    }

    /**
     * Kept apart from the backpack on purpose: a gift is in the pet's paws, not in its
     * luggage. An owner rummaging through the bag cannot take it back before it is given,
     * and the pet cannot spend it or lose track of which of two cakes was meant for whom.
     *
     * @param gift what the pet now means to give, empty for nothing
     */
    public void setPendingGift(ItemStack gift) {
        this.pendingGift = gift == null ? ItemStack.EMPTY : gift;
    }

    /**
     * @return the id of the intent the pet is following; readable on both sides
     */
    public Optional<ResourceLocation> getIntent() {
        String id = this.entityData.get(INTENT);
        return id.isEmpty() ? Optional.empty() : Optional.ofNullable(ResourceLocation.tryParse(id));
    }

    /**
     * @param intent the intent the pet now follows, {@code null} for none
     */
    public void setIntent(@Nullable ResourceLocation intent) {
        this.entityData.set(INTENT, intent == null ? "" : intent.toString());
    }

    /**
     * @return the registry id of the pet's current capability, such as {@code chiikawa:farmer}
     */
    public ResourceLocation getCapabilityId() {
        return InitRegistry.PET_JOB_REGISTRY.getKey(InitRegistry.getCapabilityFromId(getPetJobId()));
    }

    public int getPetJobId() {
        return this.entityData.get(PET_JOB);
    }

    public void setPetJobId(int jobId) {
        this.entityData.set(PET_JOB, jobId);
    }

    /**
     * Pick the highest-priority capability whose tool the pet holds in its
     * mainhand, and write its id to {@link #PET_JOB}. The brain is <em>not</em>
     * rebuilt — every job's activities live on a single static brain since
     * {@link #makeBrain}, so a job change is just a synced value flip plus a
     * request for the intent selector to re-pick right away.
     */
    public void refreshJobFromMainhand() {
        if (level().isClientSide()) {
            return;
        }

        PetCapability best = null;
        for (PetCapability capability : InitRegistry.PET_JOB_REGISTRY) {
            if (capability.canAssume(this) && (best == null || capability.priority() > best.priority())) {
                best = capability;
            }
        }
        if (best == null) {
            best = InitRegistry.NONE.get();
        }
        if (best.id() != getPetJobId()) {
            setPetJobId(best.id());
            IntentSelector.requestReevaluate(this);
        }
    }

    @Override
    protected Brain.Provider<AbstractPet> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    /**
     * Build the entity's brain once, registering <em>all</em> activities up
     * front. Activities don't run unless the intent selector (see
     * {@link #customServerAiStep}) chooses them, so the unused activities cost
     * only their flat memory footprint.
     *
     * <p>This avoids the prior "rebuild on job change" pattern, which had
     * three problems: (1) {@code brain.stopAll} interrupted in-flight
     * behaviors mid-frame causing animation glitches, (2) {@code brain.pack}
     * preserved memories but not behavior internal state, (3) it diverged
     * from Mojang's static-registration convention (Villager registers
     * profession-aware behaviors once and lets activity selection do the
     * filtering).
     *
     * <p>Future jobs just append a new
     * {@code <Job>JobHandler.registerActivities(brain)} call here and list
     * their intents on the capability — no plumbing elsewhere needs to touch.
     */
    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        Brain<AbstractPet> brain = (Brain<AbstractPet>) brainProvider().makeBrain(dynamic);

        // Universal activities present in every brain.
        BrainUtils.addCoreTasks(brain);
        BrainUtils.addFollowOwnerTasks(brain);
        BrainUtils.addStayTasks(brain);
        BrainUtils.addIdleTasks(brain);
        BrainUtils.addPickUpTasks(brain);
        BrainUtils.addTakeTaskTasks(brain);
        BrainUtils.addShopTasks(brain);
        BrainUtils.addGiftTasks(brain);

        // Each job's activities — registered once, dormant until the intent
        // selector picks one of that job's intents.
        FarmerJobHandler.registerActivities(brain);
        FencerJobHandler.registerActivities(brain);
        ArcherJobHandler.registerActivities(brain);
        MusicianJobHandler.registerActivities(brain);

        brain.setCoreActivities(java.util.Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        return brain;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (eagerTicks > 0) {
            eagerTicks--;
            if (eagerTicks == 0) {
                applyEagerness();
            }
        }
        IntentSelector.tick(this, level);
        getBrain().tick(level, this);
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
            return backpack.getItem(MAINHAND_SLOT);
        }
        if (slot == EquipmentSlot.CHEST) {
            return backpack.getItem(BAG_SLOT);
        }
        return super.getItemBySlot(slot);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            backpack.setItem(MAINHAND_SLOT, stack);
            refreshJobFromMainhand();
            return;
        }
        if (slot == EquipmentSlot.CHEST) {
            backpack.setItem(BAG_SLOT, stack);
            return;
        }
        super.setItemSlot(slot, stack);
    }

    /**
     * The bag slot used to be ordinary storage. A pet saved before pets could wear a bag
     * may have anything in it, so whatever is there moves into the first free pocket —
     * or onto the floor if there is none. Either way the owner can find it again.
     */
    private void clearBagSlotOfOldStorage() {
        ItemStack inBagSlot = backpack.getItem(BAG_SLOT);
        if (inBagSlot.isEmpty() || inBagSlot.is(InitItems.BEAR_BACKPACK.get())) {
            return;
        }
        backpack.setItem(BAG_SLOT, ItemStack.EMPTY);
        for (int slot = BAG_SLOT + 1; slot < BACKPACK_SIZE; slot++) {
            if (backpack.getItem(slot).isEmpty()) {
                backpack.setItem(slot, inBagSlot);
                return;
            }
        }
        spawnAtLocation(inBagSlot);
    }

    /**
     * A proper meal puts a pet in the mood to work: it moves quicker and picks work over
     * pottering about for a while. Work itself is instant — a pet pulls a weed the moment
     * it can reach one — so "faster" can only mean getting there sooner and dawdling less,
     * and those are the two things this changes.
     */
    public void feedDish(int ticks) {
        eagerTicks = Math.max(eagerTicks, ticks);
        applyEagerness();
    }

    /** Whether the pet is still in the mood, for the selector and for anything watching. */
    public boolean isEager() {
        return eagerTicks > 0;
    }

    /** Keeps the speed bonus in step with the mood, adding or removing it exactly once. */
    private void applyEagerness() {
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean applied = speed.getModifier(EAGER_SPEED_ID) != null;
        if (isEager() && !applied) {
            speed.addTransientModifier(new AttributeModifier(EAGER_SPEED_ID, EAGER_SPEED_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else if (!isEager() && applied) {
            speed.removeModifier(EAGER_SPEED_ID);
        }
    }

    /** Whether the pet is wearing a bag, which is what the last ten slots wait for. */
    public boolean isWearingBag() {
        return backpack.getItem(BAG_SLOT).is(InitItems.BEAR_BACKPACK.get());
    }

    /**
     * Empties the bag's own slots onto the floor. Called when the bag comes off: what was
     * in it has to go somewhere a player can see, rather than vanishing or being quietly
     * stuffed into pockets that were already full.
     */
    public void dropBagContents() {
        for (int slot = BACKPACK_SIZE; slot < backpack.getContainerSize(); slot++) {
            ItemStack stack = backpack.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) {
                spawnAtLocation(stack);
            }
        }
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
        return PetAnimContext.base(getPetDirective(), getPetJobId(), walkSpeed, getActivity());
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
        builder.define(PET_MODE, (byte) PetDirective.FOLLOW.ordinal());
        builder.define(PET_JOB, InitRegistry.NONE_ID);
        builder.define(ANIM_TRIGGER, 0);
        builder.define(REACTION_TRIGGER, 0);
        builder.define(ACTIVITY, (byte) PetActivity.NONE.networkId());
        builder.define(TASK, new CompoundTag());
        builder.define(INTENT, "");
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
        CompoundTag task = this.entityData.get(TASK);
        if (!task.isEmpty()) {
            output.store("Task", CompoundTag.CODEC, task);
        }
        if (!pendingGift.isEmpty()) {
            output.store("Gift", ItemStack.CODEC, pendingGift);
        }
        if (eagerTicks > 0) {
            output.putInt("Eager", eagerTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.child("Backpack").ifPresent(backpackInput -> {
            ContainerHelper.loadAllItems(backpackInput, backpack.getItems());
            clearBagSlotOfOldStorage();
        });
        input.getInt("PetJob").ifPresent(this::setPetJobId);
        this.entityData.set(PET_MODE, input.getByteOr("PetMode", this.entityData.get(PET_MODE)));
        this.entityData.set(TASK, input.read("Task", CompoundTag.CODEC).orElseGet(CompoundTag::new));
        pendingGift = input.read("Gift", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        eagerTicks = input.getIntOr("Eager", 0);
        applyEagerness();
        refreshJobFromMainhand();
    }

    /**
     * A pet spawning in the wild picks up a tool its personality leans towards, which
     * decides its job, and roams freely around where it spawned.
     */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            setItemSlot(EquipmentSlot.MAINHAND, PetPersonalities.of(getType()).drawWildTool(level.getRandom()));
            setPetDirective(PetDirective.FREE);
        }
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    /** A newly tamed pet follows its owner, keeping everything it carries. */
    @Override
    public void tame(Player player) {
        super.tame(player);
        setPetDirective(PetDirective.FOLLOW);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult result = PetInteractHandler.handle(this, player, hand);
        if (result != InteractionResult.PASS) {
            return result;
        }
        return super.mobInteract(player, hand);
    }

    /**
     * A hunting slip counts what the pet puts down. The game hands the credit for a kill
     * to whoever dealt it, arrows included, so this is the one place both a sword and a
     * bow report through.
     */
    @Override
    public void awardKillScore(Entity killed, int score, DamageSource source) {
        super.awardKillScore(killed, score, source);
        if (killed instanceof Enemy) {
            TaskTracker.advance(this, PetWorkCounters.SLAY, 1);
        }
    }

    /** A pet that falls loses the slip it carried: the job failed and pays nothing. */
    @Override
    public void die(DamageSource source) {
        setTask(null);
        super.die(source);
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

    /**
     * Nothing a pet carries falls out when it dies; it all stays in the doll. Vanilla
     * would otherwise drop each equipment slot by chance, including the main hand tool,
     * which is the first backpack slot.
     */
    @Override
    protected float getEquipmentDropChance(EquipmentSlot slot) {
        return 0.0F;
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
