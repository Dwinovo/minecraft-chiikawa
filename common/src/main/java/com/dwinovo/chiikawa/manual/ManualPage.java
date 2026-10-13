package com.dwinovo.chiikawa.manual;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.utils.ModCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.Vec3;

/**
 * One chapter of the handbook: a title, one little scene and a short text. Loaded from
 * {@code assets/<namespace>/manual/<id>.json} by {@link ManualLoader}, so a resource pack can
 * add chapters or redo the mod's own; the mod's are generated.
 *
 * <p>A scene is not a picture but the pets themselves, acting it out: it names who is in it,
 * what they hold and what they do, and the handbook puts them on stage.
 *
 * @param order where the chapter comes, lowest first
 * @param title the chapter's name, a translation key
 * @param scene the illustration
 * @param text what to say about it, a translation key
 * @param link the chapter's page on the website, such as {@code play/work}
 */
public record ManualPage(int order, String title, Scene scene, String text, String link) {
    public static final Codec<ManualPage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ModCodecs.strictOptionalField(Codec.INT, "order", 0).forGetter(ManualPage::order),
        Codec.STRING.fieldOf("title").forGetter(ManualPage::title),
        Scene.CODEC.fieldOf("scene").forGetter(ManualPage::scene),
        Codec.STRING.fieldOf("text").forGetter(ManualPage::text),
        Codec.STRING.fieldOf("link").forGetter(ManualPage::link)
    ).apply(instance, ManualPage::new));

    /**
     * @param actors who and what is in it, drawn in this order
     */
    public record Scene(List<Actor> actors) {
        public static final Codec<Scene> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ModCodecs.strictOptionalField(Actor.CODEC.listOf(), "actors", List.of()).forGetter(Scene::actors)
        ).apply(instance, Scene::new));

        public Scene {
            actors = List.copyOf(actors);
        }
    }

    /**
     * Something in a scene: a pet, a prop (a model of the mod's, such as the labor board)
     * or an item. Exactly one of the three is named; the pet's own fields are ignored for
     * the others.
     *
     * @param pet a pet's entity type
     * @param prop a model of the mod's, by its id
     * @param item an item, or {@code #tag} for the first item in it — {@code #chiikawa:currency}
     *             is whatever money is
     * @param x across the panel, 0 at the left edge and 1 at the right
     * @param y up from the ground, as a share of the panel's height
     * @param scale how big, 1 being the usual size
     * @param facing degrees it is turned from facing the reader, towards the panel's right
     * @param offset how far from its spot it is, in pixels, turned the way it faces: two
     *               actors on the same spot and facing alike stand as they would in the
     *               world, such as a pet on the chair of an exam desk
     * @param hold what a pet holds, which is also the job it looks the part for
     * @param bag the bag a pet wears
     * @param sit whether a pet sits
     * @param walk whether a pet walks on the spot
     * @param action a pet's action, as it does it at work, played over and over
     * @param reaction a pet's reaction, played over and over
     * @param motion what it does over and over, and how often
     */
    public record Actor(Optional<ResourceLocation> pet, Optional<ResourceLocation> prop,
                        Optional<ExtraCodecs.TagOrElementLocation> item, float x, float y, float scale, float facing,
                        Vec3 offset, Optional<ExtraCodecs.TagOrElementLocation> hold, Optional<ExtraCodecs.TagOrElementLocation> bag,
                        boolean sit, boolean walk, Optional<PetAction> action, Optional<PetReaction> reaction,
                        Motion motion) {
        public static final int EVERY = 40;

        private static final Codec<PetAction> ACTION = named(PetAction.values(), PetAction.NONE);
        private static final Codec<PetReaction> REACTION = named(PetReaction.values(), PetReaction.NONE);

        public static final Codec<Actor> CODEC = ExtraCodecs.validate(RecordCodecBuilder.<Actor>create(instance -> instance.group(
            ModCodecs.strictOptionalField(ResourceLocation.CODEC, "pet").forGetter(Actor::pet),
            ModCodecs.strictOptionalField(ResourceLocation.CODEC, "prop").forGetter(Actor::prop),
            ModCodecs.strictOptionalField(ExtraCodecs.TAG_OR_ELEMENT_ID, "item").forGetter(Actor::item),
            ModCodecs.strictOptionalField(Codec.floatRange(0.0F, 1.0F), "x", 0.5F).forGetter(Actor::x),
            ModCodecs.strictOptionalField(Codec.FLOAT, "y", 0.0F).forGetter(Actor::y),
            ModCodecs.strictOptionalField(Codec.floatRange(0.0F, 4.0F), "scale", 1.0F).forGetter(Actor::scale),
            ModCodecs.strictOptionalField(Codec.FLOAT, "facing", 0.0F).forGetter(Actor::facing),
            ModCodecs.strictOptionalField(ModCodecs.VEC3, "offset", Vec3.ZERO).forGetter(Actor::offset),
            ModCodecs.strictOptionalField(ExtraCodecs.TAG_OR_ELEMENT_ID, "hold").forGetter(Actor::hold),
            ModCodecs.strictOptionalField(ExtraCodecs.TAG_OR_ELEMENT_ID, "bag").forGetter(Actor::bag),
            ModCodecs.strictOptionalField(Codec.BOOL, "sit", false).forGetter(Actor::sit),
            ModCodecs.strictOptionalField(Codec.BOOL, "walk", false).forGetter(Actor::walk),
            ModCodecs.strictOptionalField(ACTION, "action").forGetter(Actor::action),
            ModCodecs.strictOptionalField(REACTION, "reaction").forGetter(Actor::reaction),
            Motion.CODEC.forGetter(Actor::motion)
        ).apply(instance, Actor::new)), Actor::oneThing);

        private static DataResult<Actor> oneThing(Actor actor) {
            long named = (actor.pet.isPresent() ? 1 : 0) + (actor.prop.isPresent() ? 1 : 0) + (actor.item.isPresent() ? 1 : 0);
            return named == 1 ? DataResult.success(actor)
                : DataResult.error(() -> "an actor is exactly one of a pet, a prop or an item, not " + named);
        }

        /** Enum values by their lower-case names, leaving out the one that means nothing. */
        private static <E extends Enum<E>> Codec<E> named(E[] values, E none) {
            Function<String, DataResult<E>> read = name -> Arrays.stream(values)
                .filter(value -> value != none && value.name().equalsIgnoreCase(name))
                .findFirst()
                .map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "no such move: " + name));
            return Codec.STRING.comapFlatMap(read, value -> value.name().toLowerCase(Locale.ROOT));
        }
    }

    /**
     * What an actor does over and over, apart from where it stands. Its own record because
     * a record codec takes sixteen fields at most, and this is the part that is about time.
     *
     * @param play animations by name, the first this pet has, for a move only some pets have
     * @param every ticks between one playing and the next
     * @param say what a pet says in a bubble, a translation key
     * @param bob whether an item bobs up and down
     */
    public record Motion(List<String> play, int every, Optional<String> say, boolean bob) {
        public static final MapCodec<Motion> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ModCodecs.strictOptionalField(Codec.STRING.listOf(), "play", List.of()).forGetter(Motion::play),
            ModCodecs.strictOptionalField(ExtraCodecs.POSITIVE_INT, "every", Actor.EVERY).forGetter(Motion::every),
            ModCodecs.strictOptionalField(Codec.STRING, "say").forGetter(Motion::say),
            ModCodecs.strictOptionalField(Codec.BOOL, "bob", false).forGetter(Motion::bob)
        ).apply(instance, Motion::new));

        public Motion {
            play = List.copyOf(play);
        }
    }
}
