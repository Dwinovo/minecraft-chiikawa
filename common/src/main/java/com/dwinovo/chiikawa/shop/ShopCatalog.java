package com.dwinovo.chiikawa.shop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;

/**
 * What one shop sells and what it takes in, loaded from
 * {@code data/<namespace>/shop_catalog/<id>.json} by {@link ShopCatalogLoader}.
 *
 * <p>Prices are in money — whatever the {@code chiikawa:currency} tag says that is, emeralds
 * unless a pack changes it: a pet earns it off a labor board and spends it here, and nothing
 * else in the mod converts one thing into another.
 *
 * @param entries what the shop will deal in, in the order a screen lists them
 */
public record ShopCatalog(List<Entry> entries) {
    /** A shop that deals in nothing: what a block falls back to when its catalog is missing. */
    public static final ShopCatalog EMPTY = new ShopCatalog(List.of());

    public static final Codec<ShopCatalog> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Entry.CODEC.listOf().fieldOf("entries").forGetter(ShopCatalog::entries)
    ).apply(instance, ShopCatalog::new));

    public ShopCatalog {
        entries = List.copyOf(entries);
    }

    /** What the shop asks for one of {@code item}, or nothing when it does not sell it. */
    public Optional<Entry> sale(Item item) {
        return entries.stream().filter(entry -> entry.sells(item)).findFirst();
    }

    /** What the shop pays for one of {@code item}, or nothing when it does not want it. */
    public Optional<Entry> purchase(Item item) {
        return entries.stream().filter(entry -> entry.buys(item)).findFirst();
    }

    /** Everything on sale, for a pet deciding what it can afford. */
    public List<Entry> onSale() {
        return entries.stream().filter(entry -> entry.buy() > 0).toList();
    }

    /**
     * One line of the price list.
     *
     * @param item what is being priced
     * @param buy what a customer pays for one; 0 means the shop does not sell it
     * @param sell what the shop pays for one; 0 means it does not want any
     */
    public record Entry(Item item, int buy, int sell) {
        public static final Codec<Entry> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Entry::item),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("buy", 0).forGetter(Entry::buy),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("sell", 0).forGetter(Entry::sell)
        ).apply(instance, Entry::new)));

        public boolean sells(Item wanted) {
            return buy > 0 && item == wanted;
        }

        public boolean buys(Item offered) {
            return sell > 0 && item == offered;
        }
    }
}
