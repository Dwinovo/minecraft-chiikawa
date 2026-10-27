package com.dwinovo.chiikawa.entity;

import com.dwinovo.chiikawa.Constants;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * The characters, by the id of their entity type: the one list of who the pets are, which needs no registry to be asked, so
 * that data written before the game is up (a pastime's cast) and tests outside a game can name them and be held to them.
 * {@code InitEntity} registers an entity type for each.
 */
public final class PetCharacters {
    public static final Identifier USAGI = id("usagi");
    public static final Identifier HACHIWARE = id("hachiware");
    public static final Identifier CHIIKAWA = id("chiikawa");
    public static final Identifier SHISA = id("shisa");
    public static final Identifier MOMONGA = id("momonga");
    public static final Identifier KURIMANJU = id("kurimanju");
    public static final Identifier RAKKO = id("rakko");
    public static final Identifier FURUHONYA = id("furuhonya");

    private static final List<Identifier> ALL = List.of(USAGI, HACHIWARE, CHIIKAWA, SHISA, MOMONGA, KURIMANJU, RAKKO,
        FURUHONYA);

    private PetCharacters() {
    }

    /** @return every character */
    public static List<Identifier> all() {
        return ALL;
    }

    /** @return whether this is the id of a character's entity type */
    public static boolean is(Identifier id) {
        return ALL.contains(id);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
