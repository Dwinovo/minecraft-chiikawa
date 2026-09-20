package com.dwinovo.chiikawa.client.ui.mc;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Hands out vertex consumers that quietly take a bite out of every colour's alpha, so
 * anything drawn through it fades as a whole.
 *
 * <p>For the item on a fading label: a fill or a glyph can simply be given a fainter
 * colour, but an item is drawn by the game from its own model, with its own colours, and
 * the only word we get in it is the buffer it draws into. Without this the card would fade
 * out from under an item that stayed perfectly solid and then blinked.
 */
public record FadingBufferSource(MultiBufferSource delegate, float alpha) implements MultiBufferSource {
    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return new FadingConsumer(delegate.getBuffer(renderType), alpha);
    }

    /**
     * Every way of colouring a vertex ends up in {@code setColor(r, g, b, a)} — the int and
     * float forms and the bulk quad path are all written in terms of it — so dimming there
     * dims everything.
     */
    private record FadingConsumer(VertexConsumer delegate, float alpha) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int vertexAlpha) {
            delegate.setColor(red, green, blue, Math.round(vertexAlpha * alpha));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            delegate.setNormal(normalX, normalY, normalZ);
            return this;
        }
    }
}
