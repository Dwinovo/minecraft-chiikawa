package com.dwinovo.chiikawa.anim.compile;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.api.AnimationLibrary;
import com.dwinovo.chiikawa.anim.api.ModelLibrary;
import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.format.BedrockGeoFile;
import com.dwinovo.chiikawa.anim.format.ParallelTracksFile;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reload-aware loader that walks the resource tree, decodes Bedrock geo /
 * animation JSON, and replaces the contents of the model / animation
 * libraries.
 *
 * <p>Animations live at {@code assets/<ns>/animations/<file>.json} and are
 * baked against the model with the matching short key. Each animation in the
 * file is registered under {@code <ns>:<file>/<anim_name>} (e.g.
 * {@code chiikawa:chiikawa/idle}).
 *
 * <p>Optional parallel-track sidecars at {@code assets/<ns>/parallel/<file>.json}
 * declare animations that play continuously alongside the main slots
 * (blink, breath, idle ear flick, ...).
 */
public final class BedrockResourceLoader implements ResourceManagerReloadListener {

    public static final String MODEL_PATH_PREFIX = "models/entity";
    public static final String ANIMATION_PATH_PREFIX = "animations";
    public static final String PARALLEL_PATH_PREFIX = "parallel";
    public static final String JSON_EXTENSION = ".json";

    private static final Gson GSON = new Gson();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<Identifier, List<String>> parallelByModel = loadParallelTracks(manager);
        Map<Identifier, BakedModel> bakedModels = loadModels(manager, parallelByModel);
        ModelLibrary.replaceAll(bakedModels);
        Constants.LOG.info("[chiikawa-anim] loaded {} baked models", bakedModels.size());

        Map<Identifier, BakedAnimation> bakedAnims = loadAnimations(manager, bakedModels);
        AnimationLibrary.replaceAll(bakedAnims);
        Constants.LOG.info("[chiikawa-anim] loaded {} baked animations", bakedAnims.size());
    }

    private static Map<Identifier, BakedModel> loadModels(ResourceManager manager,
                                                          Map<Identifier, List<String>> parallelByModel) {
        Map<Identifier, BakedModel> baked = new HashMap<>();
        Map<Identifier, Resource> resources = manager.listResources(MODEL_PATH_PREFIX,
                id -> id.getPath().endsWith(JSON_EXTENSION));
        for (Map.Entry<Identifier, Resource> e : resources.entrySet()) {
            Identifier rid = e.getKey();
            Identifier modelKey = toModelKey(rid);
            try (BufferedReader reader = e.getValue().openAsReader()) {
                BedrockGeoFile file = GSON.fromJson(reader, BedrockGeoFile.class);
                List<String> parallelTracks = parallelByModel.getOrDefault(modelKey, List.of());
                BakedModel model = ModelBaker.bake(file, parallelTracks);
                baked.put(modelKey, model);
            } catch (Exception ex) {
                Constants.LOG.error("[chiikawa-anim] failed to load geo {}: {}", rid, ex.toString());
            }
        }
        return baked;
    }

    private static Map<Identifier, BakedAnimation> loadAnimations(ResourceManager manager,
                                                                  Map<Identifier, BakedModel> models) {
        Map<Identifier, BakedAnimation> baked = new HashMap<>();
        Map<Identifier, Resource> resources = manager.listResources(ANIMATION_PATH_PREFIX,
                id -> id.getPath().endsWith(JSON_EXTENSION));
        for (Map.Entry<Identifier, Resource> e : resources.entrySet()) {
            Identifier rid = e.getKey();
            Identifier modelKey = toAnimationFileKey(rid);
            BakedModel model = models.get(modelKey);
            if (model == null) {
                Constants.LOG.warn("[chiikawa-anim] animation file {} has no matching model {} — skipping",
                        rid, modelKey);
                continue;
            }
            try (BufferedReader reader = e.getValue().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                Map<String, BakedAnimation> anims = AnimationBaker.bake(root, model);
                for (Map.Entry<String, BakedAnimation> a : anims.entrySet()) {
                    Identifier id = Identifier.fromNamespaceAndPath(modelKey.getNamespace(),
                            modelKey.getPath() + "/" + a.getKey());
                    baked.put(id, a.getValue());
                }
            } catch (Exception ex) {
                Constants.LOG.error("[chiikawa-anim] failed to load animations {}: {}", rid, ex.toString());
            }
        }
        return baked;
    }

    /**
     * Reads every {@code parallel/<file>.json} sidecar into a
     * {@code modelKey → tracks} map.
     */
    private static Map<Identifier, List<String>> loadParallelTracks(ResourceManager manager) {
        Map<Identifier, List<String>> result = new HashMap<>();
        Map<Identifier, Resource> resources = manager.listResources(PARALLEL_PATH_PREFIX,
                id -> id.getPath().endsWith(JSON_EXTENSION));
        for (Map.Entry<Identifier, Resource> e : resources.entrySet()) {
            Identifier rid = e.getKey();
            try (BufferedReader reader = e.getValue().openAsReader()) {
                ParallelTracksFile file = GSON.fromJson(reader, ParallelTracksFile.class);
                if (file == null || file.tracks == null || file.tracks.isEmpty()) {
                    continue;
                }
                Identifier modelKey = toParallelKey(rid);
                result.put(modelKey, List.copyOf(file.tracks));
            } catch (Exception ex) {
                Constants.LOG.error("[chiikawa-anim] failed to load parallel sidecar {}: {}",
                        rid, ex.toString());
            }
        }
        return result;
    }

    /** Strips {@value #MODEL_PATH_PREFIX}/ prefix and .json suffix. */
    public static Identifier toModelKey(Identifier resourceId) {
        return stripPrefixAndExt(resourceId, MODEL_PATH_PREFIX);
    }

    /** Strips {@value #ANIMATION_PATH_PREFIX}/ prefix and .json suffix. */
    public static Identifier toAnimationFileKey(Identifier resourceId) {
        return stripPrefixAndExt(resourceId, ANIMATION_PATH_PREFIX);
    }

    /** Strips {@value #PARALLEL_PATH_PREFIX}/ prefix and .json suffix; aligns with the model key. */
    public static Identifier toParallelKey(Identifier resourceId) {
        return stripPrefixAndExt(resourceId, PARALLEL_PATH_PREFIX);
    }

    private static Identifier stripPrefixAndExt(Identifier resourceId, String prefix) {
        String path = resourceId.getPath();
        if (path.startsWith(prefix + "/")) {
            path = path.substring(prefix.length() + 1);
        }
        if (path.endsWith(JSON_EXTENSION)) {
            path = path.substring(0, path.length() - JSON_EXTENSION.length());
        }
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
    }
}
