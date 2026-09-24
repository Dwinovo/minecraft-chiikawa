package com.dwinovo.chiikawa.spawn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * Where wild pets turn up, loaded from {@code data/<namespace>/pet_spawn/<id>.json} by
 * {@link PetSpawnLoader}: the biomes, each by id or by {@code #tag}, and who spawns in them,
 * how often and how many at once, written the way vanilla writes a biome's spawners.
 *
 * <p>The same files on either loader, each adding them to its biomes its own way when the
 * server starts. So a change takes effect the next time the world is opened, as any change
 * to a biome does, not on {@code /reload}.
 *
 * @param biomes where these spawn
 * @param spawners who spawns there; each goes in its own mob category
 */
public record PetSpawn(List<ExtraCodecs.TagOrElementLocation> biomes, List<Weighted<MobSpawnSettings.SpawnerData>> spawners) {
    public static final Codec<PetSpawn> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ExtraCodecs.TAG_OR_ELEMENT_ID.listOf().fieldOf("biomes").forGetter(PetSpawn::biomes),
        Weighted.codec(MobSpawnSettings.SpawnerData.CODEC).listOf().fieldOf("spawners").forGetter(PetSpawn::spawners)
    ).apply(instance, PetSpawn::new));

    public PetSpawn {
        biomes = List.copyOf(biomes);
        spawners = List.copyOf(spawners);
    }

    /**
     * @param biome the biome's key
     * @param hasTag whether the biome is in a tag, as the loader asking can tell
     * @return whether these spawn in that biome
     */
    public boolean covers(ResourceKey<Biome> biome, Predicate<TagKey<Biome>> hasTag) {
        for (ExtraCodecs.TagOrElementLocation entry : biomes) {
            boolean named = entry.tag()
                ? hasTag.test(TagKey.create(Registries.BIOME, entry.id()))
                : entry.id().equals(biome.location());
            if (named) {
                return true;
            }
        }
        return false;
    }
}
