package com.dwinovo.chiikawa.entity.brain.errand.rest;

import net.minecraft.resources.ResourceLocation;

/**
 * What a rest is about, for a source that depends on it: the camera item a photo is taken with,
 * or the scene two pets play.
 *
 * @param key the id of that item or scene
 */
public record RestContext(ResourceLocation key) {
}
