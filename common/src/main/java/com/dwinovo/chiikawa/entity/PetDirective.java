package com.dwinovo.chiikawa.entity;

import net.minecraft.network.chat.Component;

/**
 * The owner's standing order for a pet, cycled by sneak-right-click.
 *
 * <p>Persisted as its ordinal in the synced {@code PET_MODE} byte and the
 * {@code PetMode} save key (0 follow, 1 stay, 2 free), so constants must never
 * be reordered.
 *
 * <p>Inside the AI only {@link com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints}
 * reads it; behaviors, sensors and intents see the anchor and permissions derived
 * from it instead.
 */
public enum PetDirective {
    FOLLOW("message.chiikawa.pet_follow"),
    STAY("message.chiikawa.pet_sit"),
    FREE("message.chiikawa.pet_work");

    private final String messageKey;

    PetDirective(String messageKey) {
        this.messageKey = messageKey;
    }

    /**
     * @param pet the pet whose name is shown
     * @return the action-bar message announcing this directive
     */
    public Component message(AbstractPet pet) {
        return Component.translatable(messageKey, pet.getDisplayName().getString());
    }

    public static PetDirective fromId(int id) {
        PetDirective[] values = values();
        if (id < 0 || id >= values.length) {
            return FOLLOW;
        }
        return values[id];
    }

    public PetDirective next() {
        PetDirective[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
