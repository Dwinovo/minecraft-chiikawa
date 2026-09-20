package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** What a player sees on a labor board, sent when they open one. */
public final class BoardPayloads {
    public static final ResourceLocation BOARD_SLIPS = new ResourceLocation(Constants.MOD_ID, "board_slips");

    private BoardPayloads() {
    }

    /**
     * One slip on the board.
     *
     * @param type the slip type, named in the screen
     * @param icon the item the slip is pictured as
     * @param capability the job it is for
     * @param target how much work it asks for
     * @param taker who took it; empty while it is still up
     */
    public record SlipView(ResourceLocation type, ResourceLocation icon, ResourceLocation capability,
                           int target, String taker) {
        public static SlipView read(FriendlyByteBuf buffer) {
            return new SlipView(
                buffer.readResourceLocation(),
                buffer.readResourceLocation(),
                buffer.readResourceLocation(),
                buffer.readVarInt(),
                buffer.readUtf()
            );
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(type);
            buffer.writeResourceLocation(icon);
            buffer.writeResourceLocation(capability);
            buffer.writeVarInt(target);
            buffer.writeUtf(taker);
        }
    }

    /** Opens the labor board screen with the day's slips. */
    public record BoardSlipsPayload(List<SlipView> slips) implements MusicPayloads.Payload {
        public static BoardSlipsPayload read(FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<SlipView> slips = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                slips.add(SlipView.read(buffer));
            }
            return new BoardSlipsPayload(slips);
        }

        @Override
        public ResourceLocation id() {
            return BOARD_SLIPS;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(slips.size());
            for (SlipView slip : slips) {
                slip.write(buffer);
            }
        }
    }
}
