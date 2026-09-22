package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.manual.ManualPage;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * The generated handbook pages (gameplay doc, "the handbook"): each a four-panel strip the
 * pets act out, one page for each thing there is to know.
 */
public final class ManualData {
    public static final ResourceLocation WORK = id("work");

    private ManualData() {
    }

    /** @return pages by id */
    public static Map<ResourceLocation, ManualPage> all() {
        return Map.of(WORK, work());
    }

    /** Off to work: a board goes up, a pet takes a slip, does the work and gets paid. */
    private static ManualPage work() {
        return new ManualPage(10, "manual.chiikawa.work.title", List.of(
            panel("manual.chiikawa.work.board",
                prop(InitBlocks.LABOR_BOARD.get()).at(0.66F).facing(-25.0F),
                pet(InitEntity.CHIIKAWA_PET).at(0.26F).facing(30.0F).hold(Items.WOODEN_HOE).say("manual.chiikawa.say.wa")),
            panel("manual.chiikawa.work.take",
                prop(InitBlocks.LABOR_BOARD.get()).at(0.74F).facing(-30.0F).scale(0.85F),
                pet(InitEntity.CHIIKAWA_PET).at(0.47F).facing(60.0F).hold(Items.WOODEN_HOE).action(PetAction.PICKUP),
                pet(InitEntity.USAGI_PET).at(0.17F).facing(50.0F).hold(InitItems.USAGI_WEAPON.get()).walk()),
            // Standing in the grass it is pulling up: the tufts at its feet are drawn over it.
            panel("manual.chiikawa.work.do",
                item(Items.SHORT_GRASS).at(0.2F).scale(0.8F),
                item(Items.SHORT_GRASS).at(0.84F).scale(0.7F),
                pet(InitEntity.CHIIKAWA_PET).at(0.5F).facing(20.0F).hold(Items.WOODEN_HOE).action(PetAction.HARVEST).every(24),
                item(Items.SHORT_GRASS).at(0.37F),
                item(Items.SHORT_GRASS).at(0.47F).scale(0.9F),
                item(Items.SHORT_GRASS).at(0.6F),
                item(Items.SHORT_GRASS).at(0.7F).scale(0.8F)),
            panel("manual.chiikawa.work.paid",
                pet(InitEntity.CHIIKAWA_PET).at(0.42F).facing(15.0F).hold(Items.WOODEN_HOE)
                    .play("jump", "tame").every(30).say("manual.chiikawa.say.yay"),
                money().at(0.72F).up(0.45F).bob())));
    }

    private static ManualPage.Panel panel(String caption, Actor... actors) {
        return new ManualPage.Panel(caption, Arrays.stream(actors).map(Actor::build).toList());
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

    /** Whatever money is, as the currency tag has it. */
    private static Actor money() {
        return new Actor().item(new ExtraCodecs.TagOrElementLocation(InitTag.CURRENCY.location(), true));
    }

    private static ExtraCodecs.TagOrElementLocation element(Item item) {
        return new ExtraCodecs.TagOrElementLocation(BuiltInRegistries.ITEM.getKey(item), false);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(Constants.MOD_ID, path);
    }

    /** One actor, said the way a stage direction is. */
    private static final class Actor {
        private Optional<ResourceLocation> pet = Optional.empty();
        private Optional<ResourceLocation> prop = Optional.empty();
        private Optional<ExtraCodecs.TagOrElementLocation> item = Optional.empty();
        private float x = 0.5F;
        private float y;
        private float scale = 1.0F;
        private float facing;
        private Optional<ExtraCodecs.TagOrElementLocation> hold = Optional.empty();
        private boolean walk;
        private Optional<PetAction> action = Optional.empty();
        private List<String> play = List.of();
        private int every = ManualPage.Actor.EVERY;
        private Optional<String> say = Optional.empty();
        private boolean bob;

        Actor pet(ResourceLocation id) {
            pet = Optional.of(id);
            return this;
        }

        Actor prop(ResourceLocation id) {
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

        Actor hold(ItemLike thing) {
            hold = Optional.of(element(thing.asItem()));
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

        Actor say(String key) {
            say = Optional.of(key);
            return this;
        }

        Actor bob() {
            bob = true;
            return this;
        }

        ManualPage.Actor build() {
            return new ManualPage.Actor(pet, prop, item, x, y, scale, facing, hold, Optional.empty(), false, walk,
                action, Optional.empty(), new ManualPage.Motion(play, every, say, bob));
        }
    }
}
