package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.entity.brain.intent.DayPhase;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * The generated pet personalities, one per character (gameplay doc, section 8), with the
 * tools each may spawn holding in the wild (section 5).
 *
 * <p>Values keep a pet's idle wandering well below following its owner and picking up
 * items, so a wandering pet still reacts to those whatever its randomness.
 */
public final class PersonalityData {
    private static final List<Identifier> FARMING = List.of(PetIntents.HARVEST, PetIntents.PLANT, PetIntents.DELIVER,
        PetIntents.WEED, PetIntents.PICK_MUSHROOM);
    private static final List<Identifier> FIGHTING = List.of(PetIntents.MELEE, PetIntents.RANGED);

    private PersonalityData() {
    }

    /** @return personalities by pet entity type id */
    public static Map<Identifier, Personality> all() {
        Map<Identifier, Personality> all = new HashMap<>();
        // Diligent.
        all.put(id(InitEntity.CHIIKAWA_PET.get()), personality()
            .weigh(1.2F, FARMING)
            .weigh(0.8F, PetIntents.WANDER)
            .at(DayPhase.MORNING, 1.1F, FARMING)
            .randomness(0.05F)
            .wildTool(Items.WOODEN_HOE, 6)
            .wildTool(Items.STONE_HOE, 2)
            .wildTool(InitItems.CHIIKAWA_WEAPON.get(), 2)
            // Snacks, and not much else.
            .likes(Items.COOKIE, 3)
            .likes(Items.BREAD, 1)
            // Shy but fond: looks over at its owner often, and quickly away again.
            .idle(habits()
                .lookAtPlayer(3, 6.0F, 20, 40)
                .lookAtCreature(2, 5.0F, 20, 40))
            .build());
        // Cheerful, often playing music.
        all.put(id(InitEntity.HACHIWARE_PET.get()), personality()
            .weigh(1.3F, PetIntents.PLAY_MUSIC)
            .at(DayPhase.EVENING, 1.2F, PetIntents.PLAY_MUSIC)
            .randomness(0.08F)
            .wildTool(Items.WOODEN_HOE, 3)
            .wildTool(Items.STONE_HOE, 1)
            .wildTool(InitItems.HACHIWARE_WEAPON.get(), 4)
            // Things to cook with.
            .likes(Items.SUGAR, 2)
            .likes(Items.EGG, 2)
            .likes(Items.WHEAT, 1)
            // Friendly and curious, camera at the ready: takes a good long look at everyone.
            .idle(habits()
                .lookAtPlayer(3, 8.0F, 60, 100)
                .lookAtCreature(3, 8.0F, 60, 100))
            .build());
        // Very whimsical.
        all.put(id(InitEntity.USAGI_PET.get()), personality()
            .weigh(2.0F, PetIntents.WANDER)
            .at(DayPhase.NIGHT, 1.5F, PetIntents.WANDER)
            .randomness(0.15F)
            .wildTool(InitItems.USAGI_WEAPON.get(), 6)
            .wildTool(Items.WOODEN_HOE, 1)
            // Anything at all, in equal measure.
            .likes(Items.COOKIE, 1)
            .likes(Items.CAKE, 1)
            .likes(Items.PUMPKIN_PIE, 1)
            .likes(Items.SWEET_BERRIES, 1)
            .likes(Items.APPLE, 1)
            // Never still: forever off somewhere, sparing anyone only a glance.
            .idle(habits()
                .lookAtPlayer(1, 5.0F, 15, 30)
                .lookAtCreature(1, 5.0F, 15, 30)
                .stroll(4)
                .rest(1, 15, 30))
            .build());
        // Hard-working.
        all.put(id(InitEntity.SHISA_PET.get()), personality()
            .weigh(1.3F, FARMING)
            .weigh(1.2F, PetIntents.PICK_UP_ITEM)
            .weigh(0.6F, PetIntents.WANDER)
            .at(DayPhase.MORNING, 1.2F, FARMING)
            .randomness(0.05F)
            .wildTool(Items.WOODEN_HOE, 5)
            .wildTool(Items.STONE_HOE, 3)
            // Something bottled to drink.
            .likes(Items.HONEY_BOTTLE, 3)
            .likes(Items.MILK_BUCKET, 1)
            // Polite and friendly: turns to whoever comes by.
            .idle(habits()
                .lookAtPlayer(3, 6.0F, 45, 90)
                .stroll(1))
            .build());
        // Would rather not work.
        all.put(id(InitEntity.MOMONGA_PET.get()), personality()
            .weigh(0.7F, FARMING)
            .weigh(2.0F, PetIntents.WANDER)
            .at(DayPhase.DAY, 1.5F, PetIntents.WANDER)
            .randomness(0.12F)
            .wildTool(Items.AIR, 6)
            .wildTool(Items.WOODEN_HOE, 1)
            .wildTool(Items.WOODEN_SWORD, 1)
            // Small pretty things.
            .likes(Items.POPPY, 2)
            .likes(Items.PINK_TULIP, 2)
            .likes(Items.DANDELION, 1)
            // Wants to be looked at, so looks at you, long and from far off, waiting to be praised.
            .idle(habits()
                .lookAtPlayer(5, 10.0F, 80, 140)
                .lookAtCreature(1, 5.0F, 20, 40))
            .build());
        // Laid-back.
        all.put(id(InitEntity.KURIMANJU_PET.get()), personality()
            .weigh(0.85F, FARMING)
            .weigh(3.0F, PetIntents.WANDER)
            .at(DayPhase.EVENING, 1.5F, PetIntents.WANDER)
            .randomness(0.08F)
            .wildTool(Items.WOODEN_HOE, 3)
            .wildTool(Items.AIR, 2)
            // What goes with a drink, the drink itself left out.
            .likes(Items.COOKED_COD, 2)
            .likes(Items.BAKED_POTATO, 2)
            .likes(Items.DRIED_KELP, 1)
            // In no hurry about anything: sits a long while, a sigh after a drink.
            .idle(habits()
                .lookAtCreature(1, 5.0F, 45, 90)
                .stroll(1)
                .rest(4, 80, 160))
            .build());
        // Loves subjugation.
        all.put(id(InitEntity.RAKKO_PET.get()), personality()
            .weigh(1.3F, FIGHTING)
            .at(DayPhase.NIGHT, 1.2F, FIGHTING)
            .randomness(0.05F)
            // Never without its own sword, the only way anyone comes by one.
            .wildTool(InitItems.RAKKO_SWORD.get(), 1)
            // Sweet things.
            .likes(Items.CAKE, 3)
            .likes(Items.PUMPKIN_PIE, 2)
            .likes(Items.SWEET_BERRIES, 2)
            // The top-ranked subjugator keeps an eye on what moves around it, less on people.
            .idle(habits()
                .lookAtPlayer(1, 5.0F, 30, 60)
                .lookAtCreature(3, 10.0F, 45, 90))
            .build());
        // Quiet.
        all.put(id(InitEntity.FURUHONYA_PET.get()), personality()
            .weigh(0.7F, FIGHTING)
            .weigh(0.5F, PetIntents.WANDER)
            .randomness(0.02F)
            // Books, of course.
            .likes(Items.BOOK, 3)
            .likes(Items.PAPER, 1)
            // Quiet: rests a good while, and when it looks up it looks for a long time.
            .idle(habits()
                .lookAtPlayer(2, 5.0F, 60, 120)
                .lookAtCreature(1, 5.0F, 45, 90)
                .stroll(1)
                .rest(3, 80, 160))
            .build());
        return all;
    }

    private static Identifier id(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

    private static Builder personality() {
        return new Builder();
    }

    /** Starts from the habits every pet has, changing only what a character does otherwise. */
    private static HabitsBuilder habits() {
        return new HabitsBuilder();
    }

    private static final class Builder {
        private final Map<Identifier, Float> multipliers = new HashMap<>();
        private final Map<DayPhase, Map<Identifier, Float>> routine = new EnumMap<>(DayPhase.class);
        private final List<Personality.WeightedItem> wildTools = new ArrayList<>();
        private final List<Personality.WeightedItem> likes = new ArrayList<>();
        private float randomness;
        private IdleHabits idle = IdleHabits.DEFAULT;

        Builder weigh(float factor, Identifier intent) {
            return weigh(factor, List.of(intent));
        }

        Builder weigh(float factor, List<Identifier> intents) {
            intents.forEach(intent -> multipliers.put(intent, factor));
            return this;
        }

        Builder at(DayPhase phase, float factor, Identifier intent) {
            return at(phase, factor, List.of(intent));
        }

        Builder at(DayPhase phase, float factor, List<Identifier> intents) {
            Map<Identifier, Float> multipliersAt = routine.computeIfAbsent(phase, ignored -> new HashMap<>());
            intents.forEach(intent -> multipliersAt.put(intent, factor));
            return this;
        }

        Builder randomness(float randomness) {
            this.randomness = randomness;
            return this;
        }

        Builder wildTool(ItemLike item, int weight) {
            wildTools.add(new Personality.WeightedItem(item.asItem(), weight));
            return this;
        }

        /** Something this kind of pet would buy for itself, given the money and a shop. */
        Builder likes(ItemLike item, int weight) {
            likes.add(new Personality.WeightedItem(item.asItem(), weight));
            return this;
        }

        Builder idle(HabitsBuilder habits) {
            this.idle = habits.build();
            return this;
        }

        Personality build() {
            return new Personality(multipliers, routine, randomness, wildTools, likes, idle);
        }
    }

    private static final class HabitsBuilder {
        private IdleHabits.Glance lookAtPlayer = IdleHabits.DEFAULT.lookAtPlayer();
        private IdleHabits.Glance lookAtCreature = IdleHabits.DEFAULT.lookAtCreature();
        private int stroll = IdleHabits.DEFAULT.stroll();
        private IdleHabits.Pause rest = IdleHabits.DEFAULT.rest();

        HabitsBuilder lookAtPlayer(int weight, float range, int shortest, int longest) {
            lookAtPlayer = new IdleHabits.Glance(weight, range, new InclusiveRange<>(shortest, longest));
            return this;
        }

        HabitsBuilder lookAtCreature(int weight, float range, int shortest, int longest) {
            lookAtCreature = new IdleHabits.Glance(weight, range, new InclusiveRange<>(shortest, longest));
            return this;
        }

        HabitsBuilder stroll(int weight) {
            stroll = weight;
            return this;
        }

        HabitsBuilder rest(int weight, int shortest, int longest) {
            rest = new IdleHabits.Pause(weight, new InclusiveRange<>(shortest, longest));
            return this;
        }

        IdleHabits build() {
            return new IdleHabits(lookAtPlayer, lookAtCreature, stroll, rest);
        }
    }
}
