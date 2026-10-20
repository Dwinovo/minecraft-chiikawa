package com.dwinovo.chiikawa.testing;

import com.dwinovo.chiikawa.data.ErrandSettingsData;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Errands;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;

/**
 * The errand settings a game has once its data packs are loaded, for a test that decides without a
 * server: the ones the mod ships, as the data generator writes them. And the errands asked what they
 * offer with those settings' tuning, for a test that asks one question of one errand.
 */
public final class TestErrands {
    private TestErrands() {
    }

    /** Makes the shipped settings the ones in force. */
    public static void load() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ErrandSettings.replaceAll(ErrandSettingsData.all());
    }

    /** @return the errand with this id */
    public static ErrandType<?, ?> errand(ResourceLocation id) {
        ErrandType<?, ?> errand = Errands.get(id);
        if (errand == null) {
            throw new IllegalArgumentException("No such errand " + id);
        }
        return errand;
    }

    /** @return what the errand answers when asked what it offers the pet: the offers, or why none */
    public static Offers<?> answer(ResourceLocation id, PetView view) {
        return answerOf(errand(id), view);
    }

    /** @return what the errand offers the pet */
    public static List<? extends Offer<?>> offers(ResourceLocation id, PetView view) {
        return answer(id, view).list();
    }

    /** @return the plan of the errand's only offer, which it must have */
    public static Plan planOf(ResourceLocation id, PetView view) {
        List<? extends Offer<?>> offers = offers(id, view);
        if (offers.size() != 1) {
            throw new IllegalStateException(id + " offers " + offers.size() + " ways, not one");
        }
        return offers.get(0).plan();
    }

    /** @return why the errand offers nothing */
    public static Check whyNoOffer(ResourceLocation id, PetView view) {
        return answer(id, view).check();
    }

    /** @return whether the errand goes on for a pet that set out with this plan */
    public static Check stillWanted(ResourceLocation id, Plan plan, PetView view) {
        return stillWantedOf(errand(id), plan, view);
    }

    /** @return the errand's score for the pet, before bonuses and personality weigh it */
    public static float score(ResourceLocation id, PetView view) {
        return scoreOf(errand(id), view);
    }

    @SuppressWarnings("unchecked")
    private static <P extends Plan, T> Offers<P> answerOf(ErrandType<P, T> type, PetView view) {
        return type.offers(view, (T) settings(type).tuning());
    }

    @SuppressWarnings("unchecked")
    private static <P extends Plan, T> Check stillWantedOf(ErrandType<P, T> type, Plan plan, PetView view) {
        return type.stillWanted((P) plan, view, (T) settings(type).tuning());
    }

    @SuppressWarnings("unchecked")
    private static <P extends Plan, T> float scoreOf(ErrandType<P, T> type, PetView view) {
        return type.score(null, view, (ErrandSettings<T>) settings(type));
    }

    private static ErrandSettings<?> settings(ErrandType<?, ?> type) {
        return ErrandSettings.of(type.id()).orElseThrow();
    }
}
