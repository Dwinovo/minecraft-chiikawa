package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.PetRecall;
import com.dwinovo.chiikawa.entity.PetRoster;
import com.dwinovo.chiikawa.entity.PetUnloadFollow;
import com.dwinovo.chiikawa.init.InitItems;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The bell, and the note of where each pet was that lets it ring for one nobody has
 * loaded in a week.
 *
 * <p>A ring is not over when it is rung: a pet whose chunk had to be woken is read back
 * over the next few ticks, so the cases ring and then watch, the way the bell does.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecallGameTests {
    private static final String BATCH = "chiikawa_recall";
    private static final int STAND = 2;
    private static final BlockPos HERE = new BlockPos(4, STAND, 4);
    /** Across the floor: nowhere a pet at heel would walk back from on its own. */
    private static final BlockPos YONDER = new BlockPos(30, STAND, 30);
    /** Near enough to count as having come when called. */
    private static final double ARRIVED_SQR = 100.0;
    /** Longer than a ring keeps looking, so a case sees the end of one. */
    private static final int RUNG_OUT = 100;
    /** A pet writes down where it is every hundred ticks; this waits one of those out. */
    private static final int NOTE_TICKS = 140;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** Rung, and the pet is here. */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = 300)
    public static void a_bell_calls_a_pet_from_across_the_field(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        AbstractPet pet = pet(helper, owner);
        pet.setPetDirective(PetDirective.STAY);
        yonder(helper, pet);

        PetRecall.ring(owner);

        helper.assertTrue(pet.distanceToSqr(owner) < ARRIVED_SQR, "the pet did not come when it was called");
        helper.assertTrue(pet.getPetDirective() == PetDirective.STAY,
            "being called changed what the pet had been told to do");
        helper.succeed();
    }

    /** Somebody else's bell is not your pet's business. */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = 300)
    public static void a_bell_does_not_call_somebody_elses_pet(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        ServerPlayer stranger = owner(helper);
        AbstractPet pet = pet(helper, owner);
        pet.setPetDirective(PetDirective.STAY);
        yonder(helper, pet);
        BlockPos before = pet.blockPosition();

        PetRecall.ring(stranger);

        helper.runAtTickTime(RUNG_OUT, () -> {
            helper.assertTrue(pet.blockPosition().equals(before), "the pet moved for somebody it does not know");
            helper.succeed();
        });
    }

    /** And out of another world, which is the half a plain teleport cannot do. */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = 600)
    public static void a_bell_reaches_into_another_dimension(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "this server has no nether to call a pet out of");
        ServerPlayer owner = owner(helper);
        AbstractPet pet = pet(helper, owner);
        UUID id = pet.getUUID();
        Entity moved = pet.changeDimension(new DimensionTransition(nether, new Vec3(8.5, 70.0, 8.5), Vec3.ZERO,
            0.0F, 0.0F, DimensionTransition.DO_NOTHING));
        helper.assertTrue(moved instanceof AbstractPet, "the pet would not go to the nether to begin with");

        PetRecall.ring(owner);

        helper.succeedWhen(() -> helper.assertTrue(
            helper.getLevel().getEntity(id) instanceof AbstractPet back && back.distanceToSqr(owner) < ARRIVED_SQR,
            "the bell did not reach into the nether"));
    }

    /** A pet writes down where it is while it goes about its day. */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = 300)
    public static void a_pet_writes_down_where_it_is(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        AbstractPet pet = pet(helper, owner);
        pet.setPetDirective(PetDirective.STAY);

        helper.runAtTickTime(NOTE_TICKS, () -> {
            List<PetRoster.Entry> noted = PetRoster.of(helper.getLevel()).pets(owner.getUUID());
            helper.assertTrue(noted.size() == 1, "the roster has " + noted.size() + " of this owner's pets, not one");
            helper.assertTrue(noted.get(0).pos().closerThan(pet.blockPosition(), 2.0),
                "the roster has the pet somewhere it has not been");
            helper.succeed();
        });
    }

    /**
     * And writes it down once more on the way out with its chunk — the one moment that
     * matters, because everything after it happens with the pet gone from the world. This
     * calls the same hook the game calls from the entity manager.
     */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = 200)
    public static void a_pet_leaves_a_note_as_its_chunk_is_put_away(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        AbstractPet pet = pet(helper, owner);
        pet.setPetDirective(PetDirective.STAY);
        yonder(helper, pet);

        PetUnloadFollow.onChunkPreUnload(List.of(pet));

        List<PetRoster.Entry> noted = PetRoster.of(helper.getLevel()).pets(owner.getUUID());
        helper.assertTrue(noted.size() == 1, "the roster has " + noted.size() + " of this owner's pets, not one");
        helper.assertTrue(noted.get(0).pos().equals(pet.blockPosition()),
            "the note says " + noted.get(0).pos() + ", and the pet went away from " + pet.blockPosition());
        helper.succeed();
    }

    /**
     * A note nobody can make good on is dropped rather than kept: the pet was revived
     * under another name, or taken out of the world by something else. The bell waits for
     * it, gives up, and stops asking about it.
     */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = 300)
    public static void a_pet_that_is_not_where_it_was_noted_is_forgotten(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        AbstractPet pet = pet(helper, owner);
        PetRoster roster = PetRoster.of(helper.getLevel());
        roster.note(pet);
        pet.discard();

        PetRecall.ring(owner);

        helper.runAtTickTime(RUNG_OUT, () -> {
            helper.assertTrue(roster.pets(owner.getUUID()).isEmpty(), "the roster kept a pet that is not there");
            helper.succeed();
        });
    }

    /**
     * What a ring costs. Nothing is spent and nothing is worn out, so the only thing
     * keeping a bell from being a way to drag pets about on demand is the half minute
     * before it can be rung again.
     */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = 200)
    public static void ringing_the_bell_puts_it_away_for_a_while(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        pet(helper, owner);
        ItemStack bell = new ItemStack(InitItems.PET_BELL.get());
        owner.setItemInHand(InteractionHand.MAIN_HAND, bell);

        bell.use(helper.getLevel(), owner, InteractionHand.MAIN_HAND);

        helper.assertTrue(owner.getCooldowns().isOnCooldown(InitItems.PET_BELL.get()),
            "the bell can be rung again on the next tick");
        helper.assertTrue(owner.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
            "ringing the bell used it up");
        helper.succeed();
    }

    /** Somebody to own the pets, standing in the test area. */
    private static ServerPlayer owner(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        owner.setPos(helper.absoluteVec(HERE.getCenter()));
        return owner;
    }

    /** That player's pet. */
    private static AbstractPet pet(GameTestHelper helper, ServerPlayer owner) {
        AbstractPet pet = wildPet(helper, HERE);
        pet.tame(owner);
        return pet;
    }

    /** Puts the pet at the far corner of the floor. */
    private static void yonder(GameTestHelper helper, AbstractPet pet) {
        BlockPos there = helper.absolutePos(YONDER);
        pet.teleportTo(there.getX() + 0.5, there.getY(), there.getZ() + 0.5);
    }
}
