package com.dwinovo.chiikawa.entity.brain.errand.work;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Pastime;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.PastimeCue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

/**
 * A {@code pet_errand/<errand>.json} that has an {@code "action"}: the work and, beside it, the errand's
 * {@link ErrandSettings}. {@code action}, {@code category}, {@code counter}, {@code requires}, {@code target} and, of a
 * pastime, {@code when} and {@code cast} are the work's, and the rest of the file is the settings, with the {@code tuning} block in the shape the action gives it.
 * A file whose work cannot be done does not read, such as one with a target its action does not take.
 *
 * @param work what the file says about the work
 * @param settings what it says about how the errand goes
 * @param <P> the shape of the action's plans
 * @param <T> the shape of the action's tuning
 */
public record WorkFile<P extends Plan, T>(Work<P, T> work, ErrandSettings<T> settings) {
    public static final Codec<WorkFile<?, ?>> CODEC = WorkActions.CODEC.<WorkFile<?, ?>>dispatch("action",
        file -> file.work().action(), WorkFile::fieldsOf);

    private static <P extends Plan, T> MapCodec<WorkFile<P, T>> fieldsOf(WorkAction<P, T> action) {
        Function<WorkFile<P, T>, DataResult<WorkFile<P, T>>> sound = file -> file.work().problem()
            .<DataResult<WorkFile<P, T>>>map(problem -> DataResult.error(() -> problem))
            .orElseGet(() -> DataResult.success(file));
        return RecordCodecBuilder.<WorkFile<P, T>>mapCodec(instance -> instance.group(
            ErrandCategory.CODEC.fieldOf("category").forGetter(file -> file.work().category()),
            ResourceLocation.CODEC.optionalFieldOf("counter").forGetter(file -> file.work().counter()),
            // Always written, though a file without it reads as none: it is a list to change.
            WorkRequirement.CODEC.listOf().optionalFieldOf("requires")
                .forGetter(file -> Optional.of(file.work().requires())),
            WorkTarget.CODEC.optionalFieldOf("target").forGetter(file -> file.work().target()),
            PastimeCue.CODEC.listOf().optionalFieldOf("when").forGetter(file -> file.work().pastime().map(Pastime::when)),
            Pastime.CAST.optionalFieldOf("cast").forGetter(file -> file.work().pastime().map(Pastime::cast)),
            ErrandSettings.mapCodec(action.tuningCodec()).forGetter(WorkFile::settings)
        ).apply(instance, (category, counter, requires, target, when, cast, settings) -> new WorkFile<>(
            new Work<>(action, category, counter, requires.orElse(List.of()), target,
                when.isPresent() || cast.isPresent()
                    ? Optional.of(new Pastime(when.orElse(List.of()), cast.orElse(Map.of()))) : Optional.empty()), settings)))
            .flatXmap(sound, sound);
    }
}
