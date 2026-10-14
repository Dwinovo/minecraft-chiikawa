package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.block.DeskPart;
import com.dwinovo.chiikawa.block.ExamDeskBlock;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.manual.ManualPage;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

/**
 * The generated handbook chapters (gameplay doc, "the handbook"): six, each one little scene
 * the pets act out, a short text and the page of the website it goes on about.
 *
 * <p>A pet in a scene says one of its own lines, the words it says in the world at that
 * moment ({@link PetVoiceData}): the handbook never puts words in a pet's mouth that the
 * pet would not say. Usagi says "ウラ", not "here you are"; Rakko does not cry out.
 */
public final class ManualData {
    private ManualData() {
    }

    /** @return chapters by id, in the order the handbook lists them: {@link ManualPage#order} */
    public static Map<Identifier, ManualPage> all() {
        return Map.of(
            id("meet"), meet(),
            id("work"), work(),
            id("shop"), shop(),
            id("care"), care(),
            id("camera"), camera(),
            id("licence"), licence());
    }

    /** A chapter's title, by the chapter's name: shared with the wording, which is written to it. */
    public static String titleKey(String chapter) {
        return "manual.chiikawa." + chapter + ".title";
    }

    /** A chapter's text. */
    public static String textKey(String chapter) {
        return "manual.chiikawa." + chapter + ".text";
    }

    /** Meeting them: tamed with food, and it says so. */
    private static ManualPage meet() {
        return chapter("meet", 10, "play/taming",
            pet(InitEntity.CHIIKAWA_PET).at(0.42F).facing(-20.0F).play("tame", "eat").say(VoiceMoment.TAME),
            item(Items.BREAD).at(0.72F).up(0.35F).bob());
    }

    /** Off to work: a pet takes a slip from the board while another walks up. */
    private static ManualPage work() {
        return chapter("work", 20, "play/work",
            prop(InitBlocks.LABOR_BOARD.get()).at(0.74F).facing(-30.0F).scale(0.85F),
            pet(InitEntity.CHIIKAWA_PET).at(0.47F).facing(60.0F).hold(Items.WOODEN_HOE).action(PetAction.PICKUP),
            pet(InitEntity.USAGI_PET).at(0.17F).facing(50.0F).hold(InitItems.USAGI_WEAPON.get()).walk());
    }

    /** The shop and presents: a pet brings a flower and says so. */
    private static ManualPage shop() {
        return chapter("shop", 30, "play/shop",
            pet(InitEntity.USAGI_PET).at(0.5F).hold(Items.POPPY).play("open_mouth1").say(VoiceMoment.GIFT));
    }

    /** Looking after them: the doll on a cake, and the pet back. */
    private static ManualPage care() {
        return chapter("care", 40, "play/care",
            item(Items.CAKE).at(0.3F).scale(1.4F),
            item(InitItems.CHIIKAWA_DOLL.get()).at(0.3F).up(0.45F).bob(),
            pet(InitEntity.CHIIKAWA_PET).at(0.7F).facing(-20.0F).play("tame", "jump").every(30)
                .say(VoiceMoment.REVIVE));
    }

    /**
     * Hachiware's camera: Hachiware lifts it to take a photo, which is the one time its camera
     * shows in a scene, and Chiikawa poses for it.
     */
    private static ManualPage camera() {
        return chapter("camera", 50, "play/camera",
            pet(InitEntity.HACHIWARE_PET).at(0.35F).facing(30.0F).play("photo").every(60),
            pet(InitEntity.CHIIKAWA_PET).at(0.7F).facing(-40.0F).play("photographed").every(60)
                .say(VoiceMoment.PHOTOGRAPHED));
    }

    /** The weeding licence: Chiikawa on the chair of an exam desk, writing. */
    private static ManualPage licence() {
        // On the chair as it sits there in the world, side on, so the paper it bends
        // over and the pencil it writes with show.
        return chapter("licence", 60, "play/licence",
            chair(60.0F).at(0.6F),
            pet(InitEntity.CHIIKAWA_PET).at(0.6F).facing(60.0F).offset(ExamDeskBlock.SEAT).sit()
                .play("exam").every(80),
            prop(InitBlocks.EXAM_DESK.get()).at(0.6F).facing(60.0F));
    }

    /** An exam desk's chair, where it stands behind a desk on the same spot facing the same way. */
    private static Actor chair(float facing) {
        Identifier desk = BuiltInRegistries.ITEM.getKey(InitItems.EXAM_DESK.get());
        return new Actor().prop(DeskPart.CHAIR.model(desk)).facing(facing).offset(new Vec3(0, 0, 16));
    }

    private static ManualPage chapter(String name, int order, String link, Actor... scene) {
        return new ManualPage(order, titleKey(name), new ManualPage.Scene(Arrays.stream(scene).map(Actor::build).toList()),
            textKey(name), link);
    }

    private static Actor pet(Supplier<? extends EntityType<?>> type) {
        return new Actor().pet(BuiltInRegistries.ENTITY_TYPE.getKey(type.get()));
    }

    private static Actor prop(ItemLike thing) {
        return new Actor().prop(BuiltInRegistries.ITEM.getKey(thing.asItem()));
    }

    private static Actor item(Item item) {
        return new Actor().item(element(item));
    }


    private static ExtraCodecs.TagOrElementLocation element(Item item) {
        return new ExtraCodecs.TagOrElementLocation(BuiltInRegistries.ITEM.getKey(item), false);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
    }

    /** One actor, said the way a stage direction is. */
    private static final class Actor {
        private Optional<Identifier> pet = Optional.empty();
        private Optional<Identifier> prop = Optional.empty();
        private Optional<ExtraCodecs.TagOrElementLocation> item = Optional.empty();
        private float x = 0.5F;
        private float y;
        private float scale = 1.0F;
        private float facing;
        private Vec3 offset = Vec3.ZERO;
        private Optional<ExtraCodecs.TagOrElementLocation> hold = Optional.empty();
        private Optional<ExtraCodecs.TagOrElementLocation> bag = Optional.empty();
        private boolean sit;
        private boolean walk;
        private Optional<PetAction> action = Optional.empty();
        private Optional<PetReaction> reaction = Optional.empty();
        private List<String> play = List.of();
        private int every = ManualPage.Actor.EVERY;
        private Optional<String> say = Optional.empty();
        private boolean bob;

        Actor pet(Identifier id) {
            pet = Optional.of(id);
            return this;
        }

        Actor prop(Identifier id) {
            prop = Optional.of(id);
            return this;
        }

        Actor item(ExtraCodecs.TagOrElementLocation ref) {
            item = Optional.of(ref);
            return this;
        }

        Actor at(float across) {
            x = across;
            return this;
        }

        Actor up(float height) {
            y = height;
            return this;
        }

        Actor scale(float size) {
            scale = size;
            return this;
        }

        Actor facing(float degrees) {
            facing = degrees;
            return this;
        }

        /** Off its spot by {@code pixels}, turned the way it faces. */
        Actor offset(Vec3 pixels) {
            offset = pixels;
            return this;
        }

        Actor hold(ItemLike thing) {
            hold = Optional.of(element(thing.asItem()));
            return this;
        }

        Actor bag(ItemLike thing) {
            bag = Optional.of(element(thing.asItem()));
            return this;
        }

        Actor sit() {
            sit = true;
            return this;
        }

        Actor walk() {
            walk = true;
            return this;
        }

        Actor action(PetAction move) {
            action = Optional.of(move);
            return this;
        }

        Actor play(String... animations) {
            play = List.of(animations);
            return this;
        }

        Actor every(int ticks) {
            every = ticks;
            return this;
        }

        Actor reaction(PetReaction face) {
            reaction = Optional.of(face);
            return this;
        }

        /** The pet's first line for the moment: what it says in the world then, in its own words. */
        Actor say(VoiceMoment moment) {
            return say(moment, 1);
        }

        /** One of the pet's own lines for the moment, counted from 1. */
        Actor say(VoiceMoment moment, int line) {
            say = Optional.of(PetVoiceData.lineKey(pet.orElseThrow().getPath(), moment, line));
            return this;
        }

        Actor bob() {
            bob = true;
            return this;
        }

        ManualPage.Actor build() {
            return new ManualPage.Actor(pet, prop, item, x, y, scale, facing, offset, hold, bag, sit, walk,
                action, reaction, new ManualPage.Motion(play, every, say, bob));
        }
    }
}
