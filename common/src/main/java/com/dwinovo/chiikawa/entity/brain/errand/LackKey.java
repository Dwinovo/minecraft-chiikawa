package com.dwinovo.chiikawa.entity.brain.errand;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * What a pet is short of, as an errand names it when it says why it cannot start: one item, or any of the items of
 * a tag. Held by id and not by the item, so an errand can name it before the registry is there to be asked and a test
 * can build one without the game; the words for it are worked out where somebody reads them, see {@link #translationKey}.
 */
public sealed interface LackKey {
    /** @return the translation key of what it is called, for a game to put into its own language */
    String translationKey();

    /** @return an item to picture it by: the item itself, or the first of a tag's; none when a tag has nothing in it */
    Optional<Identifier> icon();

    /** @return the lack of this item */
    static LackKey item(Identifier item) {
        return new OfItem(item);
    }

    /** @return the lack of any of the items of this tag */
    static LackKey tag(TagKey<Item> tag) {
        return new OfTag(tag.location());
    }

    /**
     * @param item the item's id
     */
    record OfItem(Identifier item) implements LackKey {
        @Override
        public String translationKey() {
            return BuiltInRegistries.ITEM.getOptional(item).map(Item::getDescriptionId)
                .orElseGet(() -> "item." + item.getNamespace() + "." + item.getPath().replace('/', '.'));
        }

        @Override
        public Optional<Identifier> icon() {
            return Optional.of(item);
        }
    }

    /**
     * @param tag the tag's id
     */
    record OfTag(Identifier tag) implements LackKey {
        @Override
        public String translationKey() {
            return "tag.item." + tag.getNamespace() + "." + tag.getPath().replace('/', '.');
        }

        @Override
        public Optional<Identifier> icon() {
            TagKey<Item> key = TagKey.create(Registries.ITEM, tag);
            // Asked of the items and not of the tag, which is not there to be asked until the tags are bound.
            return BuiltInRegistries.ITEM.listElements().filter(holder -> holder.is(key)).findFirst()
                .map(holder -> holder.key().identifier());
        }
    }
}
