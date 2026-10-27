package com.dwinovo.chiikawa.testing;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;

/**
 * Tags where there is no world to load them: the tags of the game's registries are bound when a world loads, and a case in a
 * unit test has none. A thing a case needs a tag of is given it here, and given none again when the case is done.
 */
public final class TestTags {
    private TestTags() {
    }

    /**
     * Makes a tag the one tag of a thing, in place of whatever tags it had, or takes every tag away.
     *
     * @param holder the thing, such as {@code EntityType.ALLAY.builtInRegistryHolder()}
     * @param tag its tag; none to take them away
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void bind(Holder.Reference holder, TagKey tag) {
        try {
            Method bindTags = Holder.Reference.class.getDeclaredMethod("bindTags", Collection.class);
            bindTags.setAccessible(true);
            bindTags.invoke(holder, tag == null ? Set.of() : Set.of(tag));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
