package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandView;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.social.PetInteraction;
import com.dwinovo.chiikawa.social.SocialRules;
import net.minecraft.world.item.ItemStack;

/**
 * The two meet: whatever the scene hands over is taken out of the pet's own backpack, there and then, and given to the
 * pet that was asked, which eats it, and the scene begins for both. Waits until the pet it is for has stopped what it
 * was doing and is waiting for it, which is the tick after it says yes. Gives up if that pet has gone, or no longer
 * waits for this one, or if there is nothing left to hand over, which is a pet that has eaten what it meant to share.
 * Done in the tick it takes, so it is never cut half way: the treat is in the bag or it is on its way.
 */
public final class StartScene implements Step {
    private final Meeting meeting;
    private final PetInteraction interaction;

    /**
     * @param meeting the scene, which says whom
     * @param interaction the scene, which says what is handed over and how keen it makes the other pet
     */
    public StartScene(Meeting meeting, PetInteraction interaction) {
        this.meeting = meeting;
        this.interaction = interaction;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        AbstractPet pet = live.pet();
        AbstractPet partner = meeting.partner(live).orElse(null);
        if (partner == null) {
            return StepStatus.gaveUp("gone");
        }
        handle.lookAt(partner);
        ErrandView theirs = partner.getMind().errands();
        if (theirs.engagedWith(handle.now()).filter(pet.getUUID()::equals).isEmpty()) {
            return StepStatus.gaveUp("partner_left");
        }
        if (!theirs.isOn(ErrandIds.COOPERATE)) {
            return StepStatus.RUNNING;
        }
        ItemStack treat = ItemStack.EMPTY;
        if (interaction.handsOver().isPresent()) {
            treat = SocialRules.takeHandOver(pet, interaction);
            if (treat.isEmpty()) {
                return StepStatus.gaveUp("no_treat");
            }
        }
        handle.post(partner, new Message.SceneStart(treat, interaction.partnerEagerTicks()));
        return StepStatus.DONE;
    }

    @Override
    public boolean holdsGaze() {
        return true;
    }
}
