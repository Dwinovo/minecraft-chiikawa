package com.dwinovo.chiikawa.entity.brain.errand.work;

import com.dwinovo.chiikawa.entity.brain.errand.view.AttackTarget;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.util.StringRepresentable;

/**
 * What a work is done to, as its file names it: {@code "target": {"type": "foe", "missing": "no_attack_target"}}.
 * Each kind says, in its {@code missing}, why the pet has nothing to do the work to, which is the key of the reason
 * shown for the errand ({@code intent.chiikawa.fail.*}). A work whose action does not take a target has no
 * {@code target}, and the file of an action that cannot use the one it names is turned off, see
 * {@link WorkAction#refuses}.
 */
public sealed interface WorkTarget {
    Codec<WorkTarget> CODEC = Kind.CODEC.dispatch("type", WorkTarget::kind, Kind::codec);

    /** @return which kind of target this is */
    Kind kind();

    /**
     * @param action the action, as its refusal names it
     * @param kind the kind of target the action takes
     * @param target what a file names
     * @return why a work of that action cannot be done to that target, or nothing
     */
    static Optional<String> neededBy(String action, Kind kind, Optional<WorkTarget> target) {
        if (target.isEmpty()) {
            return Optional.of(action + " needs a " + kind.getSerializedName() + " target");
        }
        return target.get().kind() == kind ? Optional.empty()
            : Optional.of(action + " takes a " + kind.getSerializedName() + " target, not a " + target.get().kind().getSerializedName());
    }

    /**
     * The foe the pet's attack sensor names.
     *
     * @param missing the reason key when it names none
     */
    record Foe(String missing) implements WorkTarget {
        static final MapCodec<Foe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("missing").forGetter(Foe::missing)
        ).apply(instance, Foe::new));

        /** @return the foe the pet's attack sensor names, as long as it is the pet's to fight, see {@link AttackTarget} */
        public Optional<AttackTarget> in(PetView view) {
            return view.targets().attackTarget();
        }

        @Override
        public Kind kind() {
            return Kind.FOE;
        }
    }

    /** The kinds of target a file can name, by their {@code type}. */
    enum Kind implements StringRepresentable {
        FOE(Foe.CODEC);

        static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final MapCodec<? extends WorkTarget> codec;

        Kind(MapCodec<? extends WorkTarget> codec) {
            this.codec = codec;
        }

        MapCodec<? extends WorkTarget> codec() {
            return codec;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
