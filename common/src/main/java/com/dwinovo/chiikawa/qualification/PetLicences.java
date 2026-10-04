package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Where one pet stands with every licence, kept by the pet and saved with it. A licence the
 * pet has never had anything to do with is {@link Licence#NONE}, and is not saved.
 */
public final class PetLicences {
    private static final String TAG = "Licences";
    private static final Codec<Map<Identifier, Licence>> CODEC = Codec.unboundedMap(Identifier.CODEC, Licence.CODEC);

    private final Map<Identifier, Licence> byId = new HashMap<>();

    /** @return where the pet stands with this licence */
    public Licence get(Identifier qualification) {
        return byId.getOrDefault(qualification, Licence.NONE);
    }

    public void set(Identifier qualification, Licence licence) {
        if (licence.equals(Licence.NONE)) {
            byId.remove(qualification);
        } else {
            byId.put(qualification, licence);
        }
    }

    /**
     * A slip of type {@code task} was finished: one more slip of practice for every licence
     * that slip type is practice for.
     */
    public void practised(Identifier task) {
        Qualifications.all().forEach((id, qualification) -> {
            if (qualification.practiceTask().equals(task)) {
                set(id, get(id).practised());
            }
        });
    }

    /**
     * A pet the world found for itself turns up with whatever grades it happens to hold,
     * drawn by each licence's weights and the pet's leaning.
     */
    public void drawWild(Personality personality, RandomSource random) {
        Qualifications.all().forEach((id, qualification) -> set(id,
            Licence.holding(QualificationExam.wildGrade(qualification, personality.leaning(id), random))));
    }

    public void save(ValueOutput output) {
        if (!byId.isEmpty()) {
            output.store(TAG, CODEC, byId);
        }
    }

    public void load(ValueInput input) {
        byId.clear();
        input.read(TAG, CODEC).ifPresent(byId::putAll);
    }
}
