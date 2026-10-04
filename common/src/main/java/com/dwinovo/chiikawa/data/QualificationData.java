package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.qualification.ExamFee;
import com.dwinovo.chiikawa.qualification.Qualification;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.item.Items;

/**
 * The generated licences: for now the weeding licence, grade 5 to grade 1, practised by
 * weeding slips, and sat whenever an owner pays a diamond at a board to open the exam.
 */
public final class QualificationData {
    public static final ResourceLocation WEEDING = new ResourceLocation(Constants.MOD_ID, "weeding");

    private QualificationData() {
    }

    /** @return licences by id */
    public static Map<ResourceLocation, Qualification> all() {
        return Map.of(WEEDING, new Qualification(
            5,
            new ExamFee(Items.DIAMOND, 1),
            PetTaskTypeData.WEEDING,
            1,
            // Grade 5, the first, is passed more often than not by a pet that has practised;
            // grade 1 takes years, as it does in the series.
            List.of(0.40F, 0.30F, 0.22F, 0.15F, 0.08F),
            0.03F,
            0.30F,
            0.05F,
            0.20F,
            InitItems.WEEDING_BOOK.get(),
            0.25F,
            0.95F,
            // About 26 seconds over the paper, a couple either way, so pets sitting side by
            // side do not all hand in at once.
            new InclusiveRange<>(480, 560),
            // Most wild pets hold nothing; a few turn up with grade 5 or 4, hardly any higher.
            List.of(70, 18, 8, 3, 1, 0)));
    }
}
