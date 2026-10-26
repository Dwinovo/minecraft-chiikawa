package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.remark.PetRemark;
import com.dwinovo.chiikawa.entity.brain.errand.remark.RemarkCue;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The generated pet remarks (design 0.1.4, section 8): the things a pet says in passing about how it is faring, and when.
 * What each character says is its voice's, see {@link PetVoiceData}; a remark only names the moment.
 */
public final class PetRemarkData {
    /** Short of something its work needs. */
    public static final ResourceLocation MISSING = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "missing");

    /** How long a pet has to have been short of something before it says so: half a minute, so a pet in between jobs is not nagging. */
    private static final int LACKED_TICKS = 600;
    /** How near its owner has to be, which is to say within hearing. */
    private static final double OWNER_BLOCKS = 8.0;
    /** How long it leaves it alone once said: ten minutes. */
    private static final int COOLDOWN = 12000;

    private PetRemarkData() {
    }

    /** @return remarks by id */
    public static Map<ResourceLocation, PetRemark> all() {
        return Map.of(MISSING, new PetRemark(
            List.of(new RemarkCue.LackedFor(LACKED_TICKS), new RemarkCue.OwnerWithin(OWNER_BLOCKS)),
            // Hachiware does not say what it needs: it scratches its head and trusts that something will turn up.
            Map.of(PetRemark.EVERYONE, new PetRemark.Part(VoiceMoment.MISSING, Optional.empty()),
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "hachiware").toString(),
                new PetRemark.Part(VoiceMoment.TROUBLE, Optional.of("scratch_head"))),
            COOLDOWN));
    }
}
