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
     *         nothing here, cannot afford what it wants, or is saving up for something dearer
     */
    public static Optional<ShopCatalog.Entry> wants(Personality personality, ShopCatalog catalog,
                                                    int money, ToIntFunction<Item> carried, RandomSource random) {
        return WeightedRandom.getRandomItem(random, worthBuying(personality, catalog, money, carried))
            .flatMap(liking -> catalog.sale(liking.item()));
    }

    /**
     * Whether what the pet buys of this is for the pet itself and never a present. A liking
     * the personality does not have is nothing special, so it may be a present.
     */
    public static boolean isKept(Personality personality, Item item) {
        return personality.likes().stream().anyMatch(liking -> liking.item() == item && liking.keep());
    }

    /**
     * Whether there is anything here worth stopping for. Asked while a pet is deciding what
     * to do next, and so asked often — it must not change the world or the pet.
     */
    public static boolean wantsAnything(Personality personality, ShopCatalog catalog, int money,
                                        ToIntFunction<Item> carried) {
        return !worthBuying(personality, catalog, money, carried).isEmpty();
    }

    /**
     * What a container holds, by item: the {@code carried} the two questions above ask of a
     * pet's backpack.
     */
    public static ToIntFunction<Item> carriedIn(Container container) {
        return item -> Payment.count(container, stack -> stack.is(item));
    }

    /**
     * What the pet would buy now: liked, on the shelf and within what it has to spend. Unless
     * it is saving up: while it wants something it is saving for, and this shop has it on the
     * shelf, nothing else is worth buying however cheap, and the thing itself only once it can
     * be paid for.
     */
    private static List<Personality.WeightedItem> worthBuying(Personality personality, ShopCatalog catalog, int money,
                                                              ToIntFunction<Item> carried) {
        List<Personality.WeightedItem> saving = personality.likes().stream()
            .filter(liking -> liking.saveUp() && liking.wantedBy(carried) && catalog.sale(liking.item()).isPresent())
            .toList();
        return (saving.isEmpty() ? personality.likes() : saving).stream()
            .filter(liking -> liking.wantedBy(carried)
                && catalog.sale(liking.item()).filter(entry -> entry.buy() <= money).isPresent())
            .toList();
    }
}
