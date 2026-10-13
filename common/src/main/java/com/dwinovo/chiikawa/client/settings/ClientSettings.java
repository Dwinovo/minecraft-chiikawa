package com.dwinovo.chiikawa.client.settings;

import com.dwinovo.chiikawa.Constants;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;

/**
 * What a player chooses for their own game, whatever world they are in: kept in
 * {@code config/chiikawa/client.json}, beside the music folder, and written out with its
 * defaults the first time so there is a file to open. Nothing in here reaches the server.
 *
 * @param speechBubbles whether a pet's words are drawn over its head. The pet still opens
 *                      its mouth either way
 */
public record ClientSettings(boolean speechBubbles) {
    public static final ClientSettings DEFAULT = new ClientSettings(true);

    /**
     * A field left out reads as its default, but is always written: the file is where a player
     * finds what there is to change, and an optional field with a default is left out of the
     * file whenever it has the default value.
     */
    public static final Codec<ClientSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ExtraCodecs.strictOptionalField(Codec.BOOL, "speech_bubbles").forGetter(settings -> Optional.of(settings.speechBubbles()))
    ).apply(instance, speechBubbles -> new ClientSettings(speechBubbles.orElse(DEFAULT.speechBubbles))));

    /**
     * Reads the file, writing the defaults to it when it is not there. A file that cannot be
     * read leaves the defaults in force and is left alone, so a player's typo is theirs to fix
     * rather than something overwritten.
     */
    public static ClientSettings read(Path file) {
        if (!Files.exists(file)) {
            DEFAULT.write(file);
            return DEFAULT;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement json = JsonParser.parseReader(reader);
            return CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(Constants.LOG::warn).orElse(DEFAULT);
        } catch (IOException | RuntimeException ex) {
            Constants.LOG.warn("[chiikawa-settings] Could not read {}; using the defaults", file, ex);
            return DEFAULT;
        }
    }

    public void write(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                new GsonBuilder().setPrettyPrinting().create()
                    .toJson(CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow(false, Constants.LOG::error), writer);
            }
        } catch (IOException ex) {
            Constants.LOG.warn("[chiikawa-settings] Could not write {}", file, ex);
        }
    }
}
