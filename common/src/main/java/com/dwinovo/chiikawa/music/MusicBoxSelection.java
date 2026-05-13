package com.dwinovo.chiikawa.music;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record MusicBoxSelection(String trackId, String title, int revision) {
    private static final String TAG_KEY = "ChiikawaMusicBox";
    private static final String TRACK_ID_KEY = "TrackId";
    private static final String TITLE_KEY = "Title";
    private static final String REVISION_KEY = "Revision";

    public static final Codec<MusicBoxSelection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("track_id").forGetter(MusicBoxSelection::trackId),
        Codec.STRING.fieldOf("title").forGetter(MusicBoxSelection::title),
        Codec.INT.fieldOf("revision").forGetter(MusicBoxSelection::revision)
    ).apply(instance, MusicBoxSelection::new));

    public static MusicBoxSelection read(FriendlyByteBuf buffer) {
        return new MusicBoxSelection(buffer.readUtf(), buffer.readUtf(), buffer.readVarInt());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(trackId);
        buffer.writeUtf(title);
        buffer.writeVarInt(revision);
    }

    public static MusicBoxSelection get(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_KEY);
        if (tag == null || !tag.contains(TRACK_ID_KEY) || !tag.contains(TITLE_KEY)) {
            return null;
        }
        return new MusicBoxSelection(
            tag.getString(TRACK_ID_KEY),
            tag.getString(TITLE_KEY),
            tag.getInt(REVISION_KEY)
        );
    }

    public static void set(ItemStack stack, MusicBoxSelection selection) {
        CompoundTag tag = stack.getOrCreateTagElement(TAG_KEY);
        tag.putString(TRACK_ID_KEY, selection.trackId());
        tag.putString(TITLE_KEY, selection.title());
        tag.putInt(REVISION_KEY, selection.revision());
    }

    public String signature() {
        return trackId + ":" + revision;
    }
}
