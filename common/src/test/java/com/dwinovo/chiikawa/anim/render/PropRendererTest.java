package com.dwinovo.chiikawa.anim.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.compile.ModelBaker;
import com.dwinovo.chiikawa.anim.format.BedrockGeoFile;
import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * Where a prop's model lands in an item's frame. A sword or a tool is modelled standing up
 * and has to come out lying corner to corner, its head at the top right, as vanilla draws
 * a sword's sprite and as {@code item/handheld}'s hands expect; anything else stands as it
 * was modelled. Either way it faces the viewer and fills what a sprite fills.
 */
class PropRendererTest {
    private static final float EPSILON = 1.0E-4F;
    /** Half a sprite's fourteen pixels, in blocks. */
    private static final float HALF_SPAN = 7.0F / 16.0F;
    private static final float LEAN = 45.0F;
    /** A pole two pixels thick and twenty-six long, held at its foot. */
    private static final BakedModel POLE = bake(-1, 0, -1, 2, 26, 2);

    @Test
    void aSwordsHeadEndsUpAtTheTopRight() {
        Vector3f foot = place(POLE, LEAN, new Vector3f(0, 0, 0));
        Vector3f head = place(POLE, LEAN, new Vector3f(0, 26, 0));

        assertTrue(head.x > foot.x && head.y > foot.y, "the head is not above and right of the foot: " + foot + " " + head);
        assertEquals(head.x - foot.x, head.y - foot.y, EPSILON, "the pole does not lie corner to corner");
    }

    @Test
    void aSwordFillsASpriteCornerToCorner() {
        assertFillsASprite(POLE, LEAN);
    }

    @Test
    void anythingElseStandsAsItWasModelled() {
        Vector3f foot = place(POLE, 0.0F, new Vector3f(0, 0, 0));
        Vector3f head = place(POLE, 0.0F, new Vector3f(0, 26, 0));

        assertEquals(foot.x, head.x, EPSILON, "a prop that is not a tool leans");
        assertTrue(head.y > foot.y, "a prop that is not a tool is upside down");
        assertFillsASprite(POLE, 0.0F);
    }

    @Test
    void itsFrontFacesTheViewer() {
        Vector3f front = pose(POLE, LEAN).last().pose().transformDirection(new Vector3f(0, 0, -1)).normalize();

        assertEquals(1.0F, front.z, EPSILON, "the model's front is not turned to the viewer: " + front);
    }

    /** Every corner inside the sprite's fourteen pixels, and the model reaching their edge. */
    private static void assertFillsASprite(BakedModel model, float lean) {
        float reach = 0.0F;
        for (int corner = 0; corner < 8; corner++) {
            Vector3f at = place(model, lean, new Vector3f(
                (corner & 1) != 0 ? model.cubes[0].maxX : model.cubes[0].minX,
                (corner & 2) != 0 ? model.cubes[0].maxY : model.cubes[0].minY,
                (corner & 4) != 0 ? model.cubes[0].maxZ : model.cubes[0].minZ));
            assertTrue(Math.abs(at.x) <= HALF_SPAN + EPSILON && Math.abs(at.y) <= HALF_SPAN + EPSILON,
                "the model reaches out of a sprite's span at " + at);
            reach = Math.max(reach, Math.max(Math.abs(at.x), Math.abs(at.y)));
        }
        assertEquals(HALF_SPAN, reach, EPSILON, "the model does not fill a sprite's span");
    }

    private static Vector3f place(BakedModel model, float lean, Vector3f point) {
        return pose(model, lean).last().pose().transformPosition(point);
    }

    private static PoseStack pose(BakedModel model, float lean) {
        PoseStack pose = new PoseStack();
        PropRenderer.intoSprite(model, lean, pose);
        return pose;
    }

    private static BakedModel bake(float x, float y, float z, float width, float height, float depth) {
        String geo = """
            {
              "format_version": "1.12.0",
              "minecraft:geometry": [{
                "description": { "identifier": "geometry.pole", "texture_width": 16, "texture_height": 16 },
                "bones": [{
                  "name": "pole",
                  "pivot": [0, 0, 0],
                  "cubes": [{ "origin": [%s, %s, %s], "size": [%s, %s, %s], "uv": [0, 0] }]
                }]
              }]
            }
            """.formatted(x, y, z, width, height, depth);
        return ModelBaker.bake(new Gson().fromJson(geo, BedrockGeoFile.class));
    }
}
