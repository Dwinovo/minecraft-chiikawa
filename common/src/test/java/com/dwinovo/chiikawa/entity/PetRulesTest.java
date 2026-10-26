package com.dwinovo.chiikawa.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.PetRules.Toggle;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

/** What a pet is told it may do: how it is saved and sent, and that a save from before it is today's behaviour. */
class PetRulesTest {
    private static PetRules read(CompoundTag tag) {
        return PetRules.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    private static CompoundTag write(PetRules rules) {
        return (CompoundTag) PetRules.CODEC.encodeStart(NbtOps.INSTANCE, rules).getOrThrow();
    }

    @Test
    void aPetNobodyHasToldAnythingDoesEverythingAsItAlwaysHas() {
        assertEquals(EnumSet.noneOf(Toggle.class), PetRules.DEFAULT.off());
        assertEquals(CombatStance.CLEAR, PetRules.DEFAULT.stance());
        assertTrue(PetRules.DEFAULT.swapWeapons());
        for (ErrandCategory category : ErrandCategory.values()) {
            assertTrue(PetRules.DEFAULT.permits(category), category.name());
        }
    }

    @Test
    void whatIsSavedComesBackAsItWas() {
        PetRules rules = new PetRules(EnumSet.of(Toggle.SHOP, Toggle.SOCIAL), CombatStance.RETALIATE, false);

        assertEquals(rules, read(write(rules)));
    }

    @Test
    void everyStanceAndEverySwitchRoundTrips() {
        for (CombatStance stance : CombatStance.values()) {
            for (Toggle toggle : Toggle.values()) {
                PetRules rules = PetRules.DEFAULT.withStance(stance).with(toggle, false);
                assertEquals(rules, read(write(rules)));
            }
        }
    }

    @Test
    void aKeyThatIsMissingIsTheDefaultsAndNothingElseIsLost() {
        assertEquals(PetRules.DEFAULT, read(new CompoundTag()));

        CompoundTag onlyTheStance = new CompoundTag();
        onlyTheStance.putString("Stance", "none");
        assertEquals(PetRules.DEFAULT.withStance(CombatStance.NONE), read(onlyTheStance));

        CompoundTag onlyTheSwap = new CompoundTag();
        onlyTheSwap.putBoolean("SwapWeapons", false);
        assertEquals(PetRules.DEFAULT.withSwap(false), read(onlyTheSwap));

        CompoundTag onlyTheSwitches = new CompoundTag();
        ListTag off = new ListTag();
        off.add(StringTag.valueOf("gift"));
        onlyTheSwitches.put("Off", off);
        assertEquals(PetRules.DEFAULT.with(Toggle.GIFT, false), read(onlyTheSwitches));
    }

    @Test
    void theKeysAreTheNamedOnes() {
        CompoundTag tag = write(PetRules.DEFAULT.with(Toggle.PHOTO, false).withStance(CombatStance.RETALIATE).withSwap(false));

        assertEquals(Set.of("Off", "Stance", "SwapWeapons"), tag.getAllKeys());
        assertEquals("retaliate", tag.getString("Stance"));
        assertEquals("photo", tag.getList("Off", Tag.TAG_STRING).getString(0));
    }

    @Test
    void theDefaultsAreSavedAsNothing() {
        assertTrue(write(PetRules.DEFAULT).isEmpty());
    }

    @Test
    void aNameNothingKnowsIsNotTaken() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Stance", "berserk");

        assertTrue(PetRules.CODEC.parse(NbtOps.INSTANCE, tag).error().isPresent());
    }

    @Test
    void whatIsSentArrivesAsItWas() {
        PetRules rules = new PetRules(EnumSet.of(Toggle.PICK_UP), CombatStance.NONE, false);
        ByteBuf buffer = Unpooled.buffer();

        PetRules.STREAM_CODEC.encode(buffer, rules);

        assertEquals(rules, PetRules.STREAM_CODEC.decode(buffer));
    }

    @Test
    void eachSwitchIsOneCategoryAndNoTwoShareOne() {
        assertEquals(Toggle.values().length, Arrays.stream(Toggle.values()).map(Toggle::category).distinct().count());
    }

    @Test
    void changingOneThingLeavesTheRestAndTheOldRulesAlone() {
        PetRules rules = PetRules.DEFAULT.with(Toggle.SHOP, false);

        PetRules changed = rules.withStance(CombatStance.NONE).with(Toggle.GIFT, false).withSwap(false).with(Toggle.SHOP, true);

        assertEquals(EnumSet.of(Toggle.GIFT), changed.off());
        assertEquals(CombatStance.NONE, changed.stance());
        assertFalse(changed.swapWeapons());
        assertEquals(EnumSet.of(Toggle.SHOP), rules.off());
    }

    @Test
    void theSetHandedOutIsACopy() {
        PetRules rules = PetRules.DEFAULT;
        rules.off().add(Toggle.SHOP);

        assertTrue(rules.permits(ErrandCategory.SHOP));
    }
}
