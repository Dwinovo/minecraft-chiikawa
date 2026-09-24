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
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.entity.job.api.PetCapability;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.init.InitSensor;
import com.dwinovo.chiikawa.item.BagItem;
import com.dwinovo.chiikawa.item.PetDollData;
import com.dwinovo.chiikawa.sound.PetSoundCue;
import com.dwinovo.chiikawa.sound.PetSoundKind;
import com.dwinovo.chiikawa.sound.PetSoundSet;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.utils.Utils;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.NonNullList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Base implementation of a tamable pet with job, inventory, and ranged attack support.
 */
public class AbstractPet extends TamableAnimal implements RangedAttackMob, ChiikawaAnimated {
    /**
     * The pet's own pockets: slot 0 is what it holds, slot 1 is what it wears, and the
     * rest is room for things — fifteen, three full rows of five in its screen. The bag's
     * own ten come after them.
     */
    public static final int BACKPACK_SIZE = 17;
    /**
     * What every pet is made of. One builder rather than eight copies of the same four
     * lines, and {@code ATTACK_SPEED} is among them so that a weapon's own speed means
     * something in a pet's hands: vanilla hangs a sword's -2.4 off a base of 4, which is
     * where these numbers come from.
     */
    public static AttributeSupplier.Builder petAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            .add(Attributes.ATTACK_DAMAGE, 4.0D)
            .add(Attributes.ATTACK_SPEED, 4.0D);
    }

    /** How often a pet writes down where it is; see {@link PetRoster}. */
    private static final int ROSTER_TICKS = 100;

    /** How much quicker an eager pet moves. */
    private static final double EAGER_SPEED_BONUS = 0.3;
    /** 1.20.2 names an attribute modifier by a UUID, as vanilla's own do. */
    private static final UUID EAGER_SPEED_ID = UUID.fromString("d23339ca-ef17-3397-8014-84e6b890d0a4");

    /** Slot the held tool lives in; see {@link #getItemBySlot}. */
    public static final int MAINHAND_SLOT = 0;
    /** Slot the worn bag lives in, for the same reason the tool lives in a slot. */
    public static final int BAG_SLOT = 1;
    /** How much room a bag adds. */
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
    /**
     * Bought for the owner and not yet handed over; see {@link #setPendingGift}. Synced,
     * because the owner is shown what is coming before it arrives.
     */
    private static final EntityDataAccessor<ItemStack> GIFT = SynchedEntityData.defineId(AbstractPet.class, EntityDataSerializers.ITEM_STACK);
    /**
     * The game time a good meal wears off at; see {@link #feedDish}. A time rather than a
     * countdown, so it is synced once when the pet is fed rather than every tick after,
     * and the client works out how long is left against its own clock.
     */
    private static final EntityDataAccessor<Long> EAGER_UNTIL = SynchedEntityData.defineId(AbstractPet.class, EntityDataSerializers.LONG);

    /** Legacy animation-id namespace for {@link #ANIM_TRIGGER}'s low byte. */
    public static final int TRIGGER_NONE         = 0;
    public static final int TRIGGER_USE_MAINHAND = 1;
    public static final int TRIGGER_SWORD_ATTACK = 2;

    /** Controller name receiving action triggers — must match {@link com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer#CONTROLLER_ACTION}. */
    private static final String ACTION_CONTROLLER = "action";
    /** Controller name receiving reaction triggers — must match {@link com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer#CONTROLLER_REACTION}. */
    private static final String REACTION_CONTROLLER = "reaction";
    /** Controller name the pet talks on — must match {@link com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer#CONTROLLER_TALK}. */
    private static final String TALK_CONTROLLER = "talk";
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
        InitMemory.INTENT_SWITCH_LOG.get(),
        InitMemory.LAST_SAID.get()
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
    /** What this copy of the pet is saying; see {@link #speak}. Server copy is unused. */
    @Nullable
    private PetSpeech.Heard speech;

    private final SimpleContainer backpack = new SimpleContainer(FULL_BACKPACK_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            AbstractPet.this.refreshJobFromMainhand();
        }
    };

    /**
     * Creates a new pet instance tied to its entity type and level.
     *
     * @param entityType the type definition for this pet
     * @param level the level context where the pet spawns and lives
     */
    protected AbstractPet(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        // Match vanilla FloatGoal's constructor: make pathfinding float-aware so the
        // brain-side FloatBehavior bobs the pet to the surface smoothly instead of
        // sink-fighting the jump (which caused the repeated bouncing). One-time setup.
        this.getNavigation().setCanFloat(true);
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
            if (feet.getY() <= level.getMinBuildHeight()) {
                return false;
            }
            BlockPos below = feet.below();
            ground = level.getBlockState(below);
            if (ground.isSolidRender(level, below)) {
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
            : (CompoundTag) PetTask.CODEC.encodeStart(NbtOps.INSTANCE, task).getOrThrow(false, Constants.LOG::error));
    }

    /**
     * @return what the pet bought for its owner and has not handed over yet
     */
    public ItemStack getPendingGift() {
        return this.entityData.get(GIFT);
    }

    /**
     * Kept apart from the backpack on purpose: a gift is in the pet's paws, not in its
     * luggage. An owner rummaging through the bag cannot take it back before it is given,
     * and the pet cannot spend it or lose track of which of two cakes was meant for whom.
     *
     * @param gift what the pet now means to give, empty for nothing
     */
    public void setPendingGift(ItemStack gift) {
        this.entityData.set(GIFT, gift == null ? ItemStack.EMPTY : gift.copy());
    }

    /**
     * Tells the owner, in chat, something the pet did with the owner out of sight: what it
     * bought and what it gave them. Only the owner hears it, and only while they are about
     * in the pet's world — a line about a pet somewhere else is a line with nothing to look
     * at.
     *
     * <p>In grey italics, the way a book sets what someone does apart from what they say:
     * a pet's doings land in chat among what players say, and should not be taken for it.
     */
    public void tellOwner(Component message) {
        if (getOwner() instanceof ServerPlayer owner) {
            owner.displayClientMessage(message.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
        }
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
        return Services.REGISTRY.getKey(InitRegistry.PET_JOB_KEY, InitRegistry.getCapabilityFromId(getPetJobId()));
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
        PetCapability best = jobFromMainhand();
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

    /**
     * Crossing worlds builds the pet afresh in the new one, so the note is written again
     * there: a pet led through a portal and left behind is exactly what a bell is for.
     */
    @Override
    public Entity changeDimension(ServerLevel destination) {
        return noteCrossing(super.changeDimension(destination));
    }

    /**
     * Writes the note for whatever arrived from a crossing. Forge's teleport with a
     * teleporter of its own does not come through {@link #changeDimension(ServerLevel)} on
     * 1.20.2, so the loader's own dimension change calls this itself.
     */
    public static Entity noteCrossing(Entity moved) {
        if (moved instanceof AbstractPet crossed && crossed.isTame()
                && crossed.level() instanceof ServerLevel server) {
            PetRoster.of(server).note(crossed);
        }
        return moved;
    }

    /**
     * Notes where this pet is every few seconds, so a bell can find it once nobody is
     * loading the chunk it is in. Done from {@code tick} rather than the AI step because
     * a pet with its AI off still has a position worth remembering.
     */
    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && isTame() && tickCount % ROSTER_TICKS == 0) {
            PetRoster.of(server).note(this);
        }
    }

    @Override
    protected void customServerAiStep() {
        ServerLevel serverLevel = (ServerLevel) level();
        if (this.entityData.get(EAGER_UNTIL) != 0L && !isEager()) {
            // Worn off: forget when it was, and take the hurry away with it.
            this.entityData.set(EAGER_UNTIL, 0L);
            applyEagerness();
        }
        IntentSelector.tick(this, serverLevel);
        getBrain().tick(serverLevel, this);
        super.customServerAiStep();
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
        AbstractArrow arrow = arrowItem.createArrow(serverLevel, ammo, this);
        // Aimed as a skeleton aims: a third of the way up the target, from the arrow, with
        // the same lift for the fall. Aimed at the eyes, the lift carried it over a zombie's
        // head at a pet's range.
        double dx = target.getX() - getX();
        double dy = target.getY(1.0 / 3.0) - arrow.getY();
        double dz = target.getZ() - getZ();
        float inaccuracy = 14 - serverLevel.getDifficulty().getId() * 4;
        arrow.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.2F, dz, 1.6F, inaccuracy);
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
            return backpack.getItem(MAINHAND_SLOT);
        }
        if (slot == EquipmentSlot.CHEST) {
            return backpack.getItem(BAG_SLOT);
        }
        return super.getItemBySlot(slot);
    }

    /**
     * The highest-priority capability whose tool the pet holds in its mainhand, or none.
     * Worked out on either side; only the server writes it to the pet, but a picture of a
     * pet that never joins a world can take it straight from here.
     */
    public PetCapability jobFromMainhand() {
        PetCapability best = null;
        for (PetCapability capability : Services.REGISTRY.getRegistry(InitRegistry.PET_JOB_KEY)) {
            if (capability.canAssume(this) && (best == null || capability.priority() > best.priority())) {
                best = capability;
            }
        }
        return best == null ? InitRegistry.NONE.get() : best;
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
        if (inBagSlot.isEmpty() || BagItem.isBag(inBagSlot)) {
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
        long until = Math.max(this.entityData.get(EAGER_UNTIL), level().getGameTime() + ticks);
        this.entityData.set(EAGER_UNTIL, until);
        applyEagerness();
    }

    /** Whether the pet is still in the mood, for the selector and for anything watching. */
    public boolean isEager() {
        return eagerTicksLeft() > 0;
    }

    /** How long the mood has left, in ticks; 0 once it has worn off. */
    public long eagerTicksLeft() {
        return Math.max(0L, this.entityData.get(EAGER_UNTIL) - level().getGameTime());
    }

    /** Keeps the speed bonus in step with the mood, adding or removing it exactly once. */
    private void applyEagerness() {
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean applied = speed.getModifier(EAGER_SPEED_ID) != null;
        if (isEager() && !applied) {
            speed.addTransientModifier(new AttributeModifier(EAGER_SPEED_ID, Constants.MOD_ID + ":eager",
                EAGER_SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_BASE));
        } else if (!isEager() && applied) {
            speed.removeModifier(EAGER_SPEED_ID);
        }
    }

    /** Whether the pet is wearing a bag, which is what the last ten slots wait for. */
    public boolean isWearingBag() {
        return BagItem.isBag(backpack.getItem(BAG_SLOT));
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
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PET_MODE, (byte) PetDirective.FOLLOW.ordinal());
        this.entityData.define(PET_JOB, InitRegistry.NONE_ID);
        this.entityData.define(ANIM_TRIGGER, 0);
        this.entityData.define(REACTION_TRIGGER, 0);
        this.entityData.define(ACTIVITY, (byte) PetActivity.NONE.networkId());
        this.entityData.define(TASK, new CompoundTag());
        this.entityData.define(INTENT, "");
        this.entityData.define(GIFT, ItemStack.EMPTY);
        this.entityData.define(EAGER_UNTIL, 0L);
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
        if (action != PetAction.NONE && !playActionAnimation(action)) {
            Constants.LOG.warn("[chiikawa-anim] no baked animation for action '{}' on {}", action,
                BuiltInRegistries.ENTITY_TYPE.getKey(getType()));
        }
    }

    private void handleReactionTrigger() {
        int packed = entityData.get(REACTION_TRIGGER);
        int seq = packed >>> 8;
        if (seq == 0 || seq == lastSeenReactionSeq) return;
        lastSeenReactionSeq = seq;
        PetReaction reaction = PetReaction.fromNetworkId(packed & 0xFF);
        if (reaction != PetReaction.NONE) {
            playReactionAnimation(reaction);
        }
    }

    /**
     * Plays an action's animation once on this copy of the pet, the first of its candidates
     * this pet has: what a synced trigger does on a player's game, and what a picture of a
     * pet at work does directly. Client side.
     *
     * @return whether the pet had any of them
     */
    public boolean playActionAnimation(PetAction action) {
        return playOnce(ACTION_CONTROLLER, action.animationCandidates());
    }

    /** As {@link #playActionAnimation}, for a reaction, on its own layer. */
    public boolean playReactionAnimation(PetReaction reaction) {
        return playOnce(REACTION_CONTROLLER, reaction.animationCandidates());
    }

    /**
     * Starts saying a line on this copy of the pet: the words go up over its head and its
     * mouth opens. The mouth moves on a layer of its own, above the reactions, so a pet can
     * talk through whatever face it is pulling. Client side; what is said is the server's to
     * decide, see {@link PetSpeech#say}.
     *
     * @param line the translation key of what it says
     */
    public void speak(String line) {
        speech = new PetSpeech.Heard(line, tickCount);
        playOnce(TALK_CONTROLLER, List.of(PetSpeech.mouth(line)));
    }

    /** What this copy of the pet is saying right now, if anything. Client side. */
    public Optional<PetSpeech.Heard> getSpeech() {
        return Optional.ofNullable(speech).filter(heard -> tickCount - heard.since() < PetSpeech.TALK_TICKS);
    }

    /**
     * Plays the first of these animations this pet has, once, on the action layer: for a
     * picture that asks for a move by name, since which moves there are differs from pet
     * to pet. Client side.
     *
     * @return whether the pet had any of them
     */
    public boolean playAnimation(List<String> candidates) {
        return playOnce(ACTION_CONTROLLER, candidates);
    }

    private boolean playOnce(String controller, List<String> candidates) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(getType());
        for (String name : candidates) {
            BakedAnimation anim = AnimationLibrary.get(
                    new ResourceLocation(typeId.getNamespace(), typeId.getPath() + "/" + name));
            if (anim != null) {
                getPetAnimator().playOnce(controller, anim, AnimationClock.fromTicks(tickCount, 0f));
                return true;
            }
        }
        return false;
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
        CompoundTag task = this.entityData.get(TASK);
        if (!task.isEmpty()) {
            tag.put("Task", task);
        }
        ItemStack gift = getPendingGift();
        if (!gift.isEmpty()) {
            tag.put("Gift", gift.save(new CompoundTag()));
        }
        if (isEager()) {
            tag.putLong("EagerUntil", this.entityData.get(EAGER_UNTIL));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Backpack", Tag.TAG_COMPOUND)) {
            NonNullList<ItemStack> items = NonNullList.withSize(backpack.getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound("Backpack"), items);
            setBackpackItems(items);
            clearBagSlotOfOldStorage();
        }
        if (tag.contains("PetJob", Tag.TAG_INT)) {
            setPetJobId(tag.getInt("PetJob"));
        }
        if (tag.contains("PetMode", Tag.TAG_BYTE)) {
            this.entityData.set(PET_MODE, tag.getByte("PetMode"));
        }
        this.entityData.set(TASK, tag.contains("Task", Tag.TAG_COMPOUND) ? tag.getCompound("Task") : new CompoundTag());
        setPendingGift(tag.contains("Gift", Tag.TAG_COMPOUND)
            ? ItemStack.of(tag.getCompound("Gift"))
            : ItemStack.EMPTY);
        this.entityData.set(EAGER_UNTIL, tag.getLong("EagerUntil"));
        applyEagerness();
        refreshJobFromMainhand();
    }

    /**
     * A pet spawning in the wild picks up a tool its personality leans towards, which
     * decides its job, and roams freely around where it spawned.
     */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData, @Nullable CompoundTag dataTag) {
        // One the world found for itself comes with a tool, as a pet met in the wild does.
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            setItemSlot(EquipmentSlot.MAINHAND, PetPersonalities.of(getType()).drawWildTool(level.getRandom()));
        }
        // However it came, a new pet is nobody's yet, and goes its own way until it is tamed.
        setPetDirective(PetDirective.FREE);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, dataTag);
    }

    /** A newly tamed pet follows its owner, keeping everything it carries. */
    @Override
    public void tame(Player player) {
        super.tame(player);
        setPetDirective(PetDirective.FOLLOW);
        // On its owner's roster from the moment it is theirs, so a bell has somewhere to
        // start looking even for a pet nobody has seen since.
        if (level() instanceof ServerLevel server) {
            PetRoster.of(server).note(this);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult result = PetInteractHandler.handle(this, player, hand);
        if (result != InteractionResult.PASS) {
            return result;
        }
        return super.mobInteract(player, hand);
    }

    /** A pet that falls loses the slip it carried: the job failed and pays nothing. */
    @Override
    public void die(DamageSource source) {
        setTask(null);
        if (level() instanceof ServerLevel server && getOwnerUUID() != null) {
            // Its doll is what is left to find now, and that is on the floor, not in a roster.
            PetRoster.of(server).forget(getOwnerUUID(), getUUID());
        }
        super.die(source);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);

        Item dollItem = getReviveDollItem();
        if (dollItem == null) {
            return;
        }

        ItemStack dollStack = new ItemStack(dollItem);
        PetDollData.writePetToDoll(dollStack, this);
        this.spawnAtLocation(dollStack, 0.0F);
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

    /**
     * Hurt and still standing: the pet winces, and may cry out. Here rather than in
     * {@link #getHurtSound}, which the client asks too and which only wants an answer.
     */
    @Override
    protected void playHurtSound(DamageSource source) {
        super.playHurtSound(source);
        triggerReaction(PetReaction.HURT);
        PetSpeech.say(this, VoiceMoment.HURT);
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
