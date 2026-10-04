package com.dwinovo.chiikawa.whistle;

import com.dwinovo.chiikawa.entity.PetDirective;

/**
 * An order blown at one of the owner's pets that it has not taken yet: each waits its own
 * moment, so a yard does not move as one. Held in a brain memory until {@code at}.
 *
 * @param order what the pet is told to do
 * @param at the game time it answers
 */
public record WhistleCall(PetDirective order, long at) {
}
