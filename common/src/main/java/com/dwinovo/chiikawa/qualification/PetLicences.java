package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * Where one pet stands with every licence, kept by the pet and saved with it. A licence the
 * pet has never had anything to do with is {@link Licence#NONE}, and is not saved.
 */
public final class PetLicences {
    private static final String TAG = "Licences";
    private static final Codec<Map<ResourceLocation, Licence>> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Licence.CODEC);

    private final Map<ResourceLocation, Licence> byId = new HashMap<>();

    /** @return where the pet stands with this licence */
    public Licence get(ResourceLocation qualification) {
        return byId.getOrDefault(qualification, Licence.NONE);
    }

    public void set(ResourceLocation qualification, Licence licence) {
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
    public void practised(ResourceLocation task) {
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

    public void save(CompoundTag tag) {
        if (!byId.isEmpty()) {
            CODEC.encodeStart(NbtOps.INSTANCE, byId).ifSuccess(licences -> tag.put(TAG, licences));
        }
    }

    public void load(CompoundTag tag) {
        byId.clear();
        if (tag.contains(TAG, Tag.TAG_COMPOUND)) {
            CODEC.parse(NbtOps.INSTANCE, tag.get(TAG))
                .ifSuccess(byId::putAll)
                .ifError(error -> Constants.LOG.warn("[chiikawa-licence] a pet's licences did not load: {}", error.message()));
        }
    }
}
