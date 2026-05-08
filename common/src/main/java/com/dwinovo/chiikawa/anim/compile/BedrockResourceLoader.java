package com.dwinovo.chiikawa.anim.compile;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.api.AnimationLibrary;
import com.dwinovo.chiikawa.anim.api.ModelLibrary;
import com.dwinovo.chiikawa.anim.baked.BakeStamp;
import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.format.BedrockGeoFile;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
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
 * <p>Animations live at {@code assets/<ns>/animations/<file>.json} and are
 * baked against the model with the matching short key. Each animation in the
 * file is registered under {@code <ns>:<file>/<anim_name>} (e.g.
 * {@code chiikawa:chiikawa/idle}). Decorative animations like {@code blink}
 * and {@code breath} are authored in the same animation file, then wired into
 * controllers in code via {@code ChiikawaEntityRenderer.addLoopingController}.
 */
public final class BedrockResourceLoader implements ResourceManagerReloadListener {

    public static final String MODEL_PATH_PREFIX = "models/entity";
    public static final String ANIMATION_PATH_PREFIX = "animations";
    public static final String JSON_EXTENSION = ".json";

    private static final Gson GSON = new Gson();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        // Bump the bake stamp so any baked object that survives across the reload
        // is detectably stale via stamp comparison.
        long stamp = BakeStamp.next();

        Map<ResourceLocation, BakedModel> bakedModels = loadModels(manager, stamp);
        ModelLibrary.replaceAll(bakedModels);
        Constants.LOG.info("[chiikawa-anim] loaded {} baked models (stamp {})", bakedModels.size(), stamp);

        Map<ResourceLocation, BakedAnimation> bakedAnims = loadAnimations(manager, bakedModels);
        AnimationLibrary.replaceAll(bakedAnims);
        Constants.LOG.info("[chiikawa-anim] loaded {} baked animations (stamp {})", bakedAnims.size(), stamp);
    }

    private static Map<ResourceLocation, BakedModel> loadModels(ResourceManager manager, long stamp) {
        Map<ResourceLocation, BakedModel> baked = new HashMap<>();
        Map<ResourceLocation, Resource> resources = manager.listResources(MODEL_PATH_PREFIX,
                id -> id.getPath().endsWith(JSON_EXTENSION));
        for (Map.Entry<ResourceLocation, Resource> e : resources.entrySet()) {
            ResourceLocation rid = e.getKey();
            ResourceLocation modelKey = toModelKey(rid);
            try (BufferedReader reader = e.getValue().openAsReader()) {
                BedrockGeoFile file = GSON.fromJson(reader, BedrockGeoFile.class);
                BakedModel model = ModelBaker.bake(file, stamp);
                baked.put(modelKey, model);
            } catch (Exception ex) {
                Constants.LOG.error("[chiikawa-anim] failed to load geo {}: {}", rid, ex.toString());
            }
        }
        return baked;
    }

    private static Map<ResourceLocation, BakedAnimation> loadAnimations(ResourceManager manager,
                                                                        Map<ResourceLocation, BakedModel> models) {
        Map<ResourceLocation, BakedAnimation> baked = new HashMap<>();
        Map<ResourceLocation, Resource> resources = manager.listResources(ANIMATION_PATH_PREFIX,
                id -> id.getPath().endsWith(JSON_EXTENSION));
        for (Map.Entry<ResourceLocation, Resource> e : resources.entrySet()) {
            ResourceLocation rid = e.getKey();
            ResourceLocation modelKey = toAnimationFileKey(rid);
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
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(modelKey.getNamespace(),
                            modelKey.getPath() + "/" + a.getKey());
                    baked.put(id, a.getValue());
                }
            } catch (Exception ex) {
                Constants.LOG.error("[chiikawa-anim] failed to load animations {}: {}", rid, ex.toString());
            }
        }
        return baked;
    }

    /** Strips {@value #MODEL_PATH_PREFIX}/ prefix and .json suffix. */
    public static ResourceLocation toModelKey(ResourceLocation resourceId) {
        return stripPrefixAndExt(resourceId, MODEL_PATH_PREFIX);
    }

    /** Strips {@value #ANIMATION_PATH_PREFIX}/ prefix and .json suffix. */
    public static ResourceLocation toAnimationFileKey(ResourceLocation resourceId) {
        return stripPrefixAndExt(resourceId, ANIMATION_PATH_PREFIX);
    }

    private static ResourceLocation stripPrefixAndExt(ResourceLocation resourceId, String prefix) {
        String path = resourceId.getPath();
        if (path.startsWith(prefix + "/")) {
            path = path.substring(prefix.length() + 1);
        }
        if (path.endsWith(JSON_EXTENSION)) {
            path = path.substring(0, path.length() - JSON_EXTENSION.length());
        }
        return ResourceLocation.fromNamespaceAndPath(resourceId.getNamespace(), path);
    }
}
