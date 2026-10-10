package com.dwinovo.chiikawa.shop;

import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;

/**
 * What a pet would buy, given what it likes, what the shop has and what is in its pocket.
 *
 * <p>Pure: no entity, no level, no side effects — a shop trip is decided here and carried
 * out elsewhere, so what a pet would buy can be asked without anything happening.
 *
 * <p>Wanting comes first, which depends on what the pet already carries, and affording second. A pet does not walk into a shop and buy the
 * most expensive thing it can afford; it wants a cookie, and buys one if it has the money.
 */
public final class ShopBasket {
    private ShopBasket() {
    }

    /**
     * @param personality what this kind of pet likes, by weight
     * @param catalog what the shop deals in
     * @param money what the pet is carrying
     * @param carried how many of an item the pet is carrying, for the likings that depend on it
     * @param random the pet's own random source
     * @return the line of the price list it would buy from, or nothing when it wants for
     *         nothing here or cannot afford what it wants
     */
    public static Optional<ShopCatalog.Entry> wants(Personality personality, ShopCatalog catalog,
                                                    int money, ToIntFunction<Item> carried, RandomSource random) {
        List<Personality.WeightedItem> affordable = personality.likes().stream()
            .filter(liking -> worthBuying(liking, catalog, money, carried))
            .toList();
        return WeightedRandom.getRandomItem(random, affordable)
            .flatMap(liking -> catalog.sale(liking.item()));
    }

    /**
     * Whether there is anything here worth stopping for. Asked while a pet is deciding what
     * to do next, and so asked often — it must not change the world or the pet.
     */
    public static boolean wantsAnything(Personality personality, ShopCatalog catalog, int money,
                                        ToIntFunction<Item> carried) {
        return personality.likes().stream().anyMatch(liking -> worthBuying(liking, catalog, money, carried));
    }

    /**
     * What a container holds, by item: the {@code carried} the two questions above ask of a
     * pet's backpack.
     */
    public static ToIntFunction<Item> carriedIn(Container container) {
        return item -> Payment.count(container, stack -> stack.is(item));
    }

    /** Liked now, on the shelf, and within what the pet has to spend. */
    private static boolean worthBuying(Personality.WeightedItem liking, ShopCatalog catalog, int money,
                                       ToIntFunction<Item> carried) {
        return liking.wantedBy(carried)
            && catalog.sale(liking.item()).filter(entry -> entry.buy() <= money).isPresent();
    }
}
