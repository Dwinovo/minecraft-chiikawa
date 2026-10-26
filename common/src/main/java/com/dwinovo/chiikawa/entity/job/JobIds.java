package com.dwinovo.chiikawa.entity.job;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.resources.Identifier;

/**
 * The id of every job a pet can assume: its key in the job registry, the file name of its work in
 * {@code pet_job}, and what the labor board's slips name as the job they are for.
 */
public final class JobIds {
    public static final Identifier NONE = id("none");
    public static final Identifier FARMER = id("farmer");
    public static final Identifier FENCER = id("fencer");
    public static final Identifier ARCHER = id("archer");
    public static final Identifier MUSICIAN = id("musician");

    private JobIds() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
