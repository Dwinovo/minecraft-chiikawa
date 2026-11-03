package com.dwinovo.chiikawa.anim.compile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dwinovo.chiikawa.anim.baked.BakedCube;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.format.BedrockGeoFile;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class ModelBakerTest {

    /** As in Bedrock: a sheet of cloth is its two sides, with nothing drawn across its thickness. */
    @Test
    void aFaceTheModelLeavesOutIsNotDrawn() {
        BedrockGeoFile file = new Gson().fromJson("""
            {"format_version": "1.12.0", "minecraft:geometry": [{
              "description": {"identifier": "geometry.sheet", "texture_width": 16, "texture_height": 16},
              "bones": [{"name": "sheet", "pivot": [0, 0, 0], "cubes": [{
                "origin": [-4, 0, 0], "size": [8, 4, 0.5],
                "uv": {"north": {"uv": [0, 0], "uv_size": [8, 4]}, "south": {"uv": [8, 0], "uv_size": [8, 4]}}
              }]}]
            }]}""", BedrockGeoFile.class);

        BakedModel model = ModelBaker.bake(file);

        BakedCube sheet = model.cubes[0];
        assertNotNull(sheet.faceUV[BakedCube.FACE_NORTH]);
        assertNotNull(sheet.faceUV[BakedCube.FACE_SOUTH]);
        for (int face : new int[] {BakedCube.FACE_WEST, BakedCube.FACE_EAST, BakedCube.FACE_UP, BakedCube.FACE_DOWN}) {
            assertNull(sheet.faceUV[face], "face " + face + " is drawn although the model leaves it out");
        }
    }
}
