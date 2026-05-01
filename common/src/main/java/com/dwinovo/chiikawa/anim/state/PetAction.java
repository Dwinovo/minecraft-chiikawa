package com.dwinovo.chiikawa.anim.state;

import java.util.List;

/** Semantic one-shot action events that may trigger animation layers. */
public enum PetAction {
    NONE(0),

    /** Backward-compatible generic actions used by old animation files. */
    GENERIC_USE_MAINHAND(1, "use_mainhand"),
    GENERIC_SWORD_ATTACK(2, "sword_attack"),

    PICKUP(3, "pickup", "use_mainhand"),
    HARVEST(4, "harvest", "use_mainhand"),
    PLANT(5, "plant", "use_mainhand"),
    DEPOSIT(6, "deposit", "use_mainhand"),
    SLASH(7, "slash", "use_mainhand"),
    BOW_DRAW(8, "bow_draw", "sword_attack"),
    BOW_RELEASE(9, "bow_release", "sword_attack");

    private static final PetAction[] BY_NETWORK_ID = buildNetworkLookup();

    private final int networkId;
    private final List<String> animationCandidates;

    PetAction(int networkId, String... animationCandidates) {
        this.networkId = networkId;
        this.animationCandidates = List.of(animationCandidates);
    }

    public int networkId() {
        return networkId;
    }

    public List<String> animationCandidates() {
        return animationCandidates;
    }

    public static PetAction fromNetworkId(int id) {
        if (id < 0 || id >= BY_NETWORK_ID.length) {
            return NONE;
        }
        PetAction action = BY_NETWORK_ID[id];
        return action == null ? NONE : action;
    }

    public static PetAction fromLegacyAnimationName(String name) {
        if (name == null) {
            return NONE;
        }
        return switch (name) {
            case "use_mainhand" -> GENERIC_USE_MAINHAND;
            case "sword_attack" -> GENERIC_SWORD_ATTACK;
            case "pickup" -> PICKUP;
            case "harvest" -> HARVEST;
            case "plant" -> PLANT;
            case "deposit" -> DEPOSIT;
            case "slash" -> SLASH;
            case "bow_draw" -> BOW_DRAW;
            case "bow_release" -> BOW_RELEASE;
            default -> NONE;
        };
    }

    private static PetAction[] buildNetworkLookup() {
        int max = 0;
        for (PetAction action : values()) {
            max = Math.max(max, action.networkId);
        }
        PetAction[] lookup = new PetAction[max + 1];
        for (PetAction action : values()) {
            lookup[action.networkId] = action;
        }
        return lookup;
    }
}
