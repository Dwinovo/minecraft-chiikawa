package com.dwinovo.chiikawa.entity.brain.errand.work;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.combat.Ground;
import com.dwinovo.chiikawa.entity.brain.errand.view.AttackTarget;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.utils.Deliveries;
import com.dwinovo.chiikawa.utils.Utils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * What a work is done to, as its file names it: {@code "target": {"type": "foe", "missing": "no_attack_target",
 * "grounds": ["self", "owner", "nearby"]}}.
 * Each kind says, in its {@code missing}, why the pet has nothing to do the work to, which is the key of the reason
 * shown for the errand ({@code intent.chiikawa.fail.*}). A work whose action does not take a target has no
 * {@code target}, and the file of an action that cannot use the one it names is turned off, see
 * {@link WorkAction#refuses}.
 */
public sealed interface WorkTarget {
    Codec<WorkTarget> CODEC = Kind.CODEC.dispatch("type", WorkTarget::kind, Kind::codec);

    /** @return which kind of target this is */
    Kind kind();

    /** @return the key of the reason there is nothing to do the work to */
    String missing();

    /**
     * @param action the action, as its refusal names it
     * @param target what a file names
     * @param kinds the kinds of target the action takes
     * @return why a work of that action cannot be done to that target, or nothing
     */
    static Optional<String> neededBy(String action, Optional<WorkTarget> target, Kind... kinds) {
        String taken = Stream.of(kinds).map(Kind::getSerializedName).collect(Collectors.joining(" or "));
        if (target.isEmpty()) {
            return Optional.of(action + " needs a " + taken + " target");
        }
        return Stream.of(kinds).anyMatch(kind -> kind == target.get().kind()) ? Optional.empty()
            : Optional.of(action + " takes a " + taken + " target, not a " + target.get().kind().getSerializedName());
    }

    /**
     * The foe the pet's attack sensor names, when the pet fights it for one of these reasons.
     *
     * @param missing the reason key when it names none
     * @param grounds the grounds the work is for; the pet's foe is its own if it is on one of them
     */
    record Foe(String missing, Set<Ground> grounds) implements WorkTarget {
        static final MapCodec<Foe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("missing").forGetter(Foe::missing),
            ExtraCodecs.nonEmptyList(Ground.CODEC.listOf()).fieldOf("grounds").forGetter(foe -> List.copyOf(foe.grounds()))
        ).apply(instance, (missing, grounds) -> new Foe(missing, Set.copyOf(grounds))));

        public Foe {
            EnumSet<Ground> kept = EnumSet.noneOf(Ground.class);
            kept.addAll(grounds);
            grounds = Collections.unmodifiableSet(kept);
        }

        /**
         * @return the foe the pet's attack sensor names, as long as it is the pet's to fight and for a reason this work is
         *         for, see {@link AttackTarget}
         */
        public Optional<AttackTarget> in(PetView view) {
            return view.targets().attackTarget().filter(foe -> foe.grounds().stream().anyMatch(grounds::contains));
        }

        @Override
        public Kind kind() {
            return Kind.FOE;
        }
    }

    /**
     * A block in the world that the work sensor finds for the pet: what makes a block one, and whether there is any
     * use in looking for one. Cheap block checks only, no pathfinding, since the sensor asks them of every block in
     * sight and again of the blocks it remembers.
     */
    sealed interface BlockTarget extends WorkTarget {
        /** @return whether the block is one of this kind, for the pet as it is */
        boolean holds(ServerLevel level, AbstractPet pet, BlockPos pos);

        /** @return whether there is any use in looking for one, for the pet as it is */
        boolean worthFinding(AbstractPet pet);
    }

    /**
     * A crop that is ripe.
     *
     * @param missing the reason key when the pet has seen none
     */
    record RipeCrop(String missing) implements BlockTarget {
        static final MapCodec<RipeCrop> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("missing").forGetter(RipeCrop::missing)
        ).apply(instance, RipeCrop::new));

        @Override
        public boolean holds(ServerLevel level, AbstractPet pet, BlockPos pos) {
            return Utils.canHarvesr(level, pos);
        }

        @Override
        public boolean worthFinding(AbstractPet pet) {
            return true;
        }

        @Override
        public Kind kind() {
            return Kind.RIPE_CROP;
        }
    }

    /**
     * Soil a seed in the pet's bag can be planted on, so none to look for while the pet has no seed.
     *
     * @param missing the reason key when the pet has seen none
     */
    record PlantableSoil(String missing) implements BlockTarget {
        static final MapCodec<PlantableSoil> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("missing").forGetter(PlantableSoil::missing)
        ).apply(instance, PlantableSoil::new));

        @Override
        public boolean holds(ServerLevel level, AbstractPet pet, BlockPos pos) {
            return Utils.isPlantableBase(level, pet, pos);
        }

        @Override
        public boolean worthFinding(AbstractPet pet) {
            return !Utils.getSeed(pet).isEmpty();
        }

        @Override
        public Kind kind() {
            return Kind.PLANTABLE_SOIL;
        }
    }

    /**
     * A block of a tag, such as a wild plant to pull up or a mushroom to pick. A two-block-tall plant counts only at
     * its lower half, which is where its loot comes from and which takes the top half with it.
     *
     * @param blocks the tag of the blocks
     * @param missing the reason key when the pet has seen none
     */
    record Blocks(TagKey<Block> blocks, String missing) implements BlockTarget {
        static final MapCodec<Blocks> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TagKey.hashedCodec(Registries.BLOCK).fieldOf("blocks").forGetter(Blocks::blocks),
            Codec.STRING.fieldOf("missing").forGetter(Blocks::missing)
        ).apply(instance, Blocks::new));

        @Override
        public boolean holds(ServerLevel level, AbstractPet pet, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return state.is(blocks)
                && (!state.hasProperty(DoublePlantBlock.HALF) || state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER);
        }

        @Override
        public boolean worthFinding(AbstractPet pet) {
            return true;
        }

        @Override
        public Kind kind() {
            return Kind.BLOCKS;
        }
    }

    /**
     * A container that would take something from the pet's bag, so none to look for while the bag has nothing to
     * deliver.
     *
     * @param containers the tag of the blocks the pet delivers to
     * @param items the tag of the things in the bag that it delivers
     * @param missing the reason key when the pet has seen none
     */
    record Container(TagKey<Block> containers, TagKey<Item> items, String missing) implements BlockTarget {
        static final MapCodec<Container> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TagKey.hashedCodec(Registries.BLOCK).fieldOf("blocks").forGetter(Container::containers),
            TagKey.hashedCodec(Registries.ITEM).fieldOf("items").forGetter(Container::items),
            Codec.STRING.fieldOf("missing").forGetter(Container::missing)
        ).apply(instance, Container::new));

        /** @return what a delivery to these containers is about */
        public Deliveries deliveries() {
            return new Deliveries(containers, items);
        }

        @Override
        public boolean holds(ServerLevel level, AbstractPet pet, BlockPos pos) {
            return deliveries().worthVisiting(level, pos, pet.getBackpack());
        }

        @Override
        public boolean worthFinding(AbstractPet pet) {
            return deliveries().hasItems(pet.getBackpack());
        }

        @Override
        public Kind kind() {
            return Kind.CONTAINER;
        }
    }

    /** The kinds of target a file can name, by their {@code type}. */
    enum Kind implements StringRepresentable {
        FOE(Foe.CODEC),
        RIPE_CROP(RipeCrop.CODEC),
        PLANTABLE_SOIL(PlantableSoil.CODEC),
        BLOCKS(Blocks.CODEC),
        CONTAINER(Container.CODEC);

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
