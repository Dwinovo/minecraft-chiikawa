package com.dwinovo.chiikawa.entity;

import com.dwinovo.chiikawa.Constants;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * The characters, by the id of their entity type: the one list of who the pets are, which needs no registry to be asked, so
 * that data written before the game is up (a pastime's cast) and tests outside a game can name them and be held to them.
 * {@code InitEntity} registers an entity type for each.
 */
public final class PetCharacters {
    public static final ResourceLocation USAGI = id("usagi");
    public static final ResourceLocation HACHIWARE = id("hachiware");
    public static final ResourceLocation CHIIKAWA = id("chiikawa");
    public static final ResourceLocation SHISA = id("shisa");
    public static final ResourceLocation MOMONGA = id("momonga");
    public static final ResourceLocation KURIMANJU = id("kurimanju");
    public static final ResourceLocation RAKKO = id("rakko");
    public static final ResourceLocation FURUHONYA = id("furuhonya");

    private static final List<ResourceLocation> ALL = List.of(USAGI, HACHIWARE, CHIIKAWA, SHISA, MOMONGA, KURIMANJU, RAKKO,
        FURUHONYA);

    private PetCharacters() {
    }

    /** @return every character */
    public static List<ResourceLocation> all() {
        return ALL;
    }

    /** @return whether this is the id of a character's entity type */
    public static boolean is(ResourceLocation id) {
        return ALL.contains(id);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
