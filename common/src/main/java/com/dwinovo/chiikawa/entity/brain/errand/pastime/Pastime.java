package com.dwinovo.chiikawa.entity.brain.errand.pastime;

import com.dwinovo.chiikawa.entity.PetCharacters;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * What makes a work a pastime, the two fields of its errand file that only a pastime has: {@code when}, everything that
 * has to be so for a pet to pass the time with it, and {@code cast}, who does, each character in its own way, as the
 * series draws it: a pastime a character has no part in, it does not do.
 *
 * @param when the cues that must all hold
 * @param cast what each character does, by the id of its entity type
 */
public record Pastime(List<PastimeCue> when, Map<ResourceLocation, Stage> cast) {
    /** A character, by the id of its entity type; an id that is nobody's is no part. */
    private static final Codec<ResourceLocation> CHARACTER = ResourceLocation.CODEC.validate(id -> PetCharacters.is(id)
        ? DataResult.success(id) : DataResult.error(() -> id + " is not a character"));
    /** The cast of a pastime: somebody, or it would never be done. */
    public static final Codec<Map<ResourceLocation, Stage>> CAST = Codec.unboundedMap(CHARACTER, Stage.CODEC).validate(cast ->
        cast.isEmpty() ? DataResult.error(() -> "a pastime with nobody in it is never done") : DataResult.success(cast));

    public Pastime {
        when = List.copyOf(when);
        cast = Map.copyOf(cast);
    }

    /**
     * @param view the pet
     * @return what the pet does with the pastime now: its part, when it has one and every cue holds; nothing otherwise
     */
    public Optional<Stage> stageFor(PetView view) {
        return Optional.ofNullable(cast.get(view.species())).filter(stage -> when.stream().allMatch(cue -> cue.holds(view)));
    }
}
