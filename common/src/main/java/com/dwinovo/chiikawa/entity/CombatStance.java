package com.dwinovo.chiikawa.entity;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * How far a pet takes part in a fight, whatever its job: the owner's choice on the pet's screen, apart from
 * {@link PetDirective}, which says where the pet is. Kept in {@link PetRules}.
 */
public enum CombatStance implements StringRepresentable {
    /** Sets on the hostile things it sees nearby, as a pet always has. */
    CLEAR,
    /** Fights only what attacks its owner or what its owner is attacking, as a vanilla wolf does. */
    RETALIATE,
    /** Stays with its owner and does not fight; hurt, it keeps behind them. */
    NONE;

    /** By lower-case name, as a save holds it. */
    public static final Codec<CombatStance> CODEC = StringRepresentable.fromEnum(CombatStance::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
