package com.dwinovo.chiikawa.entity.job;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.resources.ResourceLocation;

/**
 * The id of every job a pet can assume: its key in the job registry, the file name of its work in
 * {@code pet_job}, and what the labor board's slips name as the job they are for.
 */
public final class JobIds {
    public static final ResourceLocation NONE = id("none");
    public static final ResourceLocation FARMER = id("farmer");
    public static final ResourceLocation FENCER = id("fencer");
    public static final ResourceLocation ARCHER = id("archer");
    public static final ResourceLocation MUSICIAN = id("musician");

    private JobIds() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
