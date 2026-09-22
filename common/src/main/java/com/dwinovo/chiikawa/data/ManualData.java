package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.manual.ManualPage;
import java.util.ArrayList;
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
    private ManualData() {
    }

    /** @return pages by id */
    public static Map<ResourceLocation, ManualPage> all() {
        return Map.of(
            id("meet"), meet(),
            id("work"), work(),
            id("shop"), shop(),
            id("presents"), presents(),
            id("supplies"), supplies(),
            id("upgrade"), upgrade(),
            id("safety"), safety());
    }

    /** A page's title, by the page's name: shared with the wording, which is written to it. */
    public static String titleKey(String page) {
        return "manual.chiikawa." + page + ".title";
    }

    /** The line under a page's panel, counted from 1. */
    public static String captionKey(String page, int panel) {
        return "manual.chiikawa." + page + "." + panel;
    }

    /** Meeting them: out in the wild, tamed with food, three orders, and their screen. */
    private static ManualPage meet() {
        return page("meet", 5,
            panel(pet(InitEntity.CHIIKAWA_PET).at(0.3F).facing(60.0F).hold(Items.WOODEN_HOE).walk(),
                pet(InitEntity.HACHIWARE_PET).at(0.72F).facing(-40.0F).hold(InitItems.HACHIWARE_WEAPON.get()).walk()),
            panel(pet(InitEntity.CHIIKAWA_PET).at(0.42F).facing(-20.0F).play("tame", "eat").say("manual.chiikawa.say.yum"),
                item(Items.BREAD).at(0.72F).up(0.35F).bob()),
            panel(pet(InitEntity.CHIIKAWA_PET).at(0.2F).facing(60.0F).walk(),
                pet(InitEntity.USAGI_PET).at(0.5F).sit(),
                pet(InitEntity.HACHIWARE_PET).at(0.8F).facing(-30.0F).hold(Items.WOODEN_HOE)),
            panel(pet(InitEntity.CHIIKAWA_PET).at(0.35F).facing(20.0F).hold(Items.WOODEN_HOE).bag(InitItems.BACKPACK.get()),
                item(Items.WHEAT).at(0.66F).up(0.5F),
                money().at(0.82F).up(0.3F).bob()));
    }

    /** Off to work: a board goes up, a pet takes a slip, does the work and gets paid. */
    private static ManualPage work() {
        return page("work", 10,
            panel(prop(InitBlocks.LABOR_BOARD.get()).at(0.66F).facing(-25.0F),
                pet(InitEntity.CHIIKAWA_PET).at(0.26F).facing(30.0F).hold(Items.WOODEN_HOE).say("manual.chiikawa.say.wa")),
            panel(prop(InitBlocks.LABOR_BOARD.get()).at(0.74F).facing(-30.0F).scale(0.85F),
                pet(InitEntity.CHIIKAWA_PET).at(0.47F).facing(60.0F).hold(Items.WOODEN_HOE).action(PetAction.PICKUP),
                pet(InitEntity.USAGI_PET).at(0.17F).facing(50.0F).hold(InitItems.USAGI_WEAPON.get()).walk()),
            // Standing in the grass it is pulling up: the tufts at its feet are drawn over it.
            panel(item(Items.SHORT_GRASS).at(0.2F).scale(0.8F),
                item(Items.SHORT_GRASS).at(0.84F).scale(0.7F),
                pet(InitEntity.CHIIKAWA_PET).at(0.5F).facing(20.0F).hold(Items.WOODEN_HOE).action(PetAction.HARVEST).every(24),
                item(Items.SHORT_GRASS).at(0.37F),
                item(Items.SHORT_GRASS).at(0.47F).scale(0.9F),
                item(Items.SHORT_GRASS).at(0.6F),
                item(Items.SHORT_GRASS).at(0.7F).scale(0.8F)),
            panel(pet(InitEntity.CHIIKAWA_PET).at(0.42F).facing(15.0F).hold(Items.WOODEN_HOE)
                    .play("jump", "tame").every(30).say("manual.chiikawa.say.yay"),
                money().at(0.72F).up(0.45F).bob()));
    }

    /** The shop: a pet spends its savings on what it likes, the owner trades, pocket money. */
    private static ManualPage shop() {
        return page("shop", 20,
            panel(prop(InitBlocks.SHOP.get()).at(0.68F).facing(-25.0F),
                pet(InitEntity.CHIIKAWA_PET).at(0.26F).facing(40.0F).walk()),
            panel(prop(InitBlocks.SHOP.get()).at(0.72F).facing(-30.0F).scale(0.9F),
                pet(InitEntity.CHIIKAWA_PET).at(0.38F).facing(50.0F).action(PetAction.PICKUP),
                item(Items.COOKIE).at(0.5F).up(0.55F).bob()),
            panel(item(Items.WHEAT).at(0.18F).up(0.35F),
                prop(InitBlocks.SHOP.get()).at(0.5F),
                money().at(0.82F).up(0.35F).bob()),
            panel(pet(InitEntity.CHIIKAWA_PET).at(0.42F).play("tame", "jump").every(30).say("manual.chiikawa.say.yay"),
                money().at(0.72F).up(0.4F).bob()));
    }

    /** Presents: picked out at the shop, carried over, handed to you, told of in chat. */
    private static ManualPage presents() {
        return page("presents", 30,
            panel(prop(InitBlocks.SHOP.get()).at(0.7F).facing(-25.0F),
                pet(InitEntity.USAGI_PET).at(0.34F).facing(45.0F).action(PetAction.PICKUP),
                item(Items.POPPY).at(0.5F).up(0.55F).bob()),
            panel(pet(InitEntity.USAGI_PET).at(0.5F).facing(-70.0F).hold(Items.POPPY).walk()),
            panel(pet(InitEntity.USAGI_PET).at(0.5F).hold(Items.POPPY).play("open_mouth1").say("manual.chiikawa.say.present")),
            panel(pet(InitEntity.CHIIKAWA_PET).at(0.3F).facing(20.0F).hold(Items.COOKIE),
                pet(InitEntity.USAGI_PET).at(0.7F).facing(-20.0F).hold(Items.POPPY)));
    }

    /** Bags and treats: a rucksack, the three pouches, a simple dish and a name tag. */
    private static ManualPage supplies() {
        return page("supplies", 40,
            // Turned away from the reader, so the rucksack on its back is what shows.
            panel(pet(InitEntity.CHIIKAWA_PET).at(0.5F).facing(150.0F).bag(InitItems.BACKPACK.get())),
            panel(pet(InitEntity.CHIIKAWA_PET).at(0.2F).facing(20.0F).bag(InitItems.BEAR_POUCH.get()),
                pet(InitEntity.HACHIWARE_PET).at(0.5F).bag(InitItems.WHALE_POUCH.get()),
                pet(InitEntity.USAGI_PET).at(0.8F).facing(-20.0F).bag(InitItems.STAR_POUCH.get())),
            panel(pet(InitEntity.HACHIWARE_PET).at(0.42F).facing(20.0F).play("eat"),
                item(InitItems.SIMPLE_DISH.get()).at(0.72F).up(0.2F)),
            panel(pet(InitEntity.CHIIKAWA_PET).at(0.4F).facing(15.0F),
                item(Items.NAME_TAG).at(0.7F).up(0.45F).bob()));
    }

    /** A better board: paid up, it puts up more slips and hunting, which pays best and can fail. */
    private static ManualPage upgrade() {
        return page("upgrade", 50,
            panel(money().at(0.25F).up(0.45F).bob(),
                prop(InitBlocks.LABOR_BOARD.get()).at(0.62F).facing(-15.0F)),
            panel(prop(InitBlocks.LABOR_BOARD.get()).at(0.72F).facing(-30.0F).scale(0.85F),
                pet(InitEntity.RAKKO_PET).at(0.38F).facing(50.0F).hold(Items.IRON_SWORD).walk()),
            panel(pet(InitEntity.RAKKO_PET).at(0.3F).facing(40.0F).hold(Items.IRON_SWORD).action(PetAction.SLASH).every(20),
                pet(InitEntity.MOMONGA_PET).at(0.72F).facing(-40.0F).hold(Items.BOW).action(PetAction.BOW_DRAW).every(30)),
            panel(pet(InitEntity.RAKKO_PET).at(0.5F).hold(Items.IRON_SWORD).play("attacked").every(30)
                .say("manual.chiikawa.say.ouch")));
    }

    /** Falling and coming back: the doll, the cake, backing off, and the bell. */
    private static ManualPage safety() {
        return page("safety", 60,
            panel(item(InitItems.CHIIKAWA_DOLL.get()).at(0.5F).up(0.15F).scale(1.6F).bob()),
            panel(item(Items.CAKE).at(0.3F).scale(1.4F),
                item(InitItems.CHIIKAWA_DOLL.get()).at(0.3F).up(0.45F).bob(),
                pet(InitEntity.CHIIKAWA_PET).at(0.7F).facing(-20.0F).play("tame", "jump").every(30)
                    .say("manual.chiikawa.say.yay")),
            panel(pet(InitEntity.CHIIKAWA_PET).at(0.45F).facing(-70.0F).hold(Items.WOODEN_SWORD).walk()),
            panel(item(InitItems.PET_BELL.get()).at(0.2F).up(0.5F).bob(),
                pet(InitEntity.CHIIKAWA_PET).at(0.55F).facing(-60.0F).walk(),
                pet(InitEntity.USAGI_PET).at(0.82F).facing(-60.0F).walk()));
    }

    private static ManualPage page(String name, int order, Actor[]... panels) {
        List<ManualPage.Panel> built = new ArrayList<>();
        for (int i = 0; i < panels.length; i++) {
            built.add(new ManualPage.Panel(captionKey(name, i + 1), Arrays.stream(panels[i]).map(Actor::build).toList()));
        }
        return new ManualPage(order, titleKey(name), built);
    }

    private static Actor[] panel(Actor... actors) {
        return actors;
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
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
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
        private Optional<ExtraCodecs.TagOrElementLocation> bag = Optional.empty();
        private boolean sit;
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

        Actor say(String key) {
            say = Optional.of(key);
            return this;
        }

        Actor bob() {
            bob = true;
            return this;
        }

        ManualPage.Actor build() {
            return new ManualPage.Actor(pet, prop, item, x, y, scale, facing, hold, bag, sit, walk,
                action, Optional.empty(), new ManualPage.Motion(play, every, say, bob));
        }
    }
}
