package com.dwinovo.chiikawa.manual;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

/**
 * One page of the handbook: a strip of four panels, each a little scene and a line under
 * it. Loaded from {@code assets/<namespace>/manual/<id>.json} by {@link ManualLoader}, so a
 * resource pack can add pages or redo the mod's own; the mod's are generated.
 *
 * <p>A scene is not a picture but the pets themselves, acting it out: a panel names who is
 * in it, what they hold and what they do, and the handbook puts them on stage.
 *
 * @param order where the page comes, lowest first
 * @param title the page's name, a translation key
 * @param panels read left to right, top to bottom
 */
public record ManualPage(int order, String title, List<Panel> panels) {
    /** How many panels a page lays out: two rows of two, the way a four-panel strip reads. */
    public static final int PANELS = 4;

    public static final Codec<ManualPage> CODEC = RecordCodecBuilder.<ManualPage>create(instance -> instance.group(
        Codec.INT.optionalFieldOf("order", 0).forGetter(ManualPage::order),
        Codec.STRING.fieldOf("title").forGetter(ManualPage::title),
        ExtraCodecs.nonEmptyList(Panel.CODEC.listOf()).fieldOf("panels").forGetter(ManualPage::panels)
    ).apply(instance, ManualPage::new)).validate(page -> page.panels.size() <= PANELS
        ? DataResult.success(page)
        : DataResult.error(() -> "a page has " + PANELS + " panels at most, not " + page.panels.size()));

    public ManualPage {
        panels = List.copyOf(panels);
    }

    /**
     * @param caption the line under the panel, a translation key
     * @param actors who and what is in it, drawn in this order
     */
    public record Panel(String caption, List<Actor> actors) {
        public static final Codec<Panel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("caption").forGetter(Panel::caption),
            Actor.CODEC.listOf().optionalFieldOf("actors", List.of()).forGetter(Panel::actors)
        ).apply(instance, Panel::new));

        public Panel {
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
     * @param hold what a pet holds, which is also the job it looks the part for
     * @param bag the bag a pet wears
     * @param sit whether a pet sits
     * @param walk whether a pet walks on the spot
     * @param action a pet's action, as it does it at work, played over and over
     * @param reaction a pet's reaction, played over and over
     * @param motion what it does over and over, and how often
     */
    public record Actor(Optional<Identifier> pet, Optional<Identifier> prop,
                        Optional<ExtraCodecs.TagOrElementLocation> item, float x, float y, float scale, float facing,
                        Optional<ExtraCodecs.TagOrElementLocation> hold, Optional<ExtraCodecs.TagOrElementLocation> bag,
                        boolean sit, boolean walk, Optional<PetAction> action, Optional<PetReaction> reaction,
                        Motion motion) {
        public static final int EVERY = 40;

        private static final Codec<PetAction> ACTION = named(PetAction.values(), PetAction.NONE);
        private static final Codec<PetReaction> REACTION = named(PetReaction.values(), PetReaction.NONE);

        public static final Codec<Actor> CODEC = RecordCodecBuilder.<Actor>create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("pet").forGetter(Actor::pet),
            Identifier.CODEC.optionalFieldOf("prop").forGetter(Actor::prop),
            ExtraCodecs.TAG_OR_ELEMENT_ID.optionalFieldOf("item").forGetter(Actor::item),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("x", 0.5F).forGetter(Actor::x),
            Codec.FLOAT.optionalFieldOf("y", 0.0F).forGetter(Actor::y),
            Codec.floatRange(0.0F, 4.0F).optionalFieldOf("scale", 1.0F).forGetter(Actor::scale),
            Codec.FLOAT.optionalFieldOf("facing", 0.0F).forGetter(Actor::facing),
            ExtraCodecs.TAG_OR_ELEMENT_ID.optionalFieldOf("hold").forGetter(Actor::hold),
            ExtraCodecs.TAG_OR_ELEMENT_ID.optionalFieldOf("bag").forGetter(Actor::bag),
            Codec.BOOL.optionalFieldOf("sit", false).forGetter(Actor::sit),
            Codec.BOOL.optionalFieldOf("walk", false).forGetter(Actor::walk),
            ACTION.optionalFieldOf("action").forGetter(Actor::action),
            REACTION.optionalFieldOf("reaction").forGetter(Actor::reaction),
            Motion.CODEC.forGetter(Actor::motion)
        ).apply(instance, Actor::new)).validate(Actor::oneThing);

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
            Codec.STRING.listOf().optionalFieldOf("play", List.of()).forGetter(Motion::play),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("every", Actor.EVERY).forGetter(Motion::every),
            Codec.STRING.optionalFieldOf("say").forGetter(Motion::say),
            Codec.BOOL.optionalFieldOf("bob", false).forGetter(Motion::bob)
        ).apply(instance, Motion::new));

        public Motion {
            play = List.copyOf(play);
        }
    }
}
