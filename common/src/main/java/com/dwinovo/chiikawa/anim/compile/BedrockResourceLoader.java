package com.dwinovo.chiikawa.anim.compile;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.api.ModelLibrary;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.format.BedrockGeoFile;
import com.google.gson.Gson;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Reload-aware loader that walks the resource tree, decodes Bedrock geo /
 * animation JSON, and replaces the contents of the model / animation
 * libraries.
 *
 * <p>Phase 1 only loads models. Animations come online in Phase 2.
 */
public final class BedrockResourceLoader implements ResourceManagerReloadListener {

    /** Resource path prefix (relative to namespace) where geo files live. */
    public static final String MODEL_PATH_PREFIX = "models/entity";
    /** File extension that marks a Bedrock geometry file. */
    public static final String MODEL_EXTENSION = ".json";

    private static final Gson GSON = new Gson();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<Identifier, BakedModel> baked = new HashMap<>();

        Map<Identifier, Resource> resources = manager.listResources(MODEL_PATH_PREFIX,
                id -> id.getPath().endsWith(MODEL_EXTENSION));

        for (Map.Entry<Identifier, Resource> e : resources.entrySet()) {
            Identifier rid = e.getKey();
            try (BufferedReader reader = e.getValue().openAsReader()) {
                BedrockGeoFile file = GSON.fromJson(reader, BedrockGeoFile.class);
                BakedModel model = ModelBaker.bake(file);
                baked.put(toModelKey(rid), model);
            } catch (Exception ex) {
                Constants.LOG.error("[chiikawa-anim] failed to load geo {}: {}", rid, ex.toString());
            }
        }

        ModelLibrary.replaceAll(baked);
        Constants.LOG.info("[chiikawa-anim] loaded {} baked models", baked.size());
    }

    /**
     * Strips the prefix and {@code .geo.json} suffix from the resource path so
     * callers can address models by short keys (e.g. {@code chiikawa:usagi}).
     */
    public static Identifier toModelKey(Identifier resourceId) {
        String path = resourceId.getPath();
        if (path.startsWith(MODEL_PATH_PREFIX + "/")) {
            path = path.substring(MODEL_PATH_PREFIX.length() + 1);
        }
        if (path.endsWith(MODEL_EXTENSION)) {
            path = path.substring(0, path.length() - MODEL_EXTENSION.length());
        }
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
    }
}
