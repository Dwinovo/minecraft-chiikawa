package com.dwinovo.chiikawa.block;

/**
 * A block drawn from a prop model of its own some of whose bones are there only now and then,
 * as an exam desk has a sheet on it only while a pet is signed up at it. Where there is no
 * block to ask — the block as an item, or in the handbook — the prop is drawn at rest, with
 * only the bones this says yes to.
 */
public interface PropAtRest {
    /** @return whether {@code bone} is there when nothing is going on */
    boolean shownAtRest(String bone);
}
