package com.dwinovo.chiikawa.anim.runtime;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;

/**
 * Where an {@link Animator} finds the baked animation a name stands for: the lookup of the
 * kind of entity being drawn, handed in with every frame. Names are the short names an
 * animation file gives its animations ({@code "idle"}, {@code "harvest"}); which file and
 * which resource they come from is the caller's business, and may differ between entities,
 * which is why entity code asks for animations by name and never holds one.
 */
@FunctionalInterface
public interface AnimationSource {

    /** @return the animation of that name, or {@code null} when there is none */
    BakedAnimation find(String name);

    /** @return the first of these names that has an animation, or {@code null} when none has */
    default BakedAnimation firstOf(Iterable<String> names) {
        for (String name : names) {
            BakedAnimation animation = find(name);
            if (animation != null) return animation;
        }
        return null;
    }
}
