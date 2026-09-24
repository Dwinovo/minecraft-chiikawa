package com.dwinovo.chiikawa.client.manual;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.manual.ManualPage;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.widget.Bubble;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * One panel of the handbook, put on stage: the pets in it made and dressed once, then
 * moved along a tick at a time and drawn where the panel says. The pets never join the
 * world — nothing thinks for them — so what they do is only what the panel asks: sit,
 * walk, and play their moves over and over.
 */
public final class ManualScene {
    /** Blocks the panel's height stands for: a pet about a block tall takes up about half of it. */
    private static final float BLOCKS_PER_PANEL = 1.9F;
    /** How far above the panel's bottom edge the ground is, in pixels. */
    public static final int GROUND = 7;
    private static final float WALK_SPEED = 0.5F;
    private static final float BOB_PIXELS = 2.0F;

    private final List<Staged> cast;
    private int ticks;

    private ManualScene(List<Staged> cast) {
        this.cast = List.copyOf(cast);
    }

    /** Puts a panel's actors on stage; one that cannot be — an id nothing answers to — is left out. */
    public static ManualScene of(ManualPage.Panel panel) {
        List<Staged> cast = new ArrayList<>();
        for (ManualPage.Actor actor : panel.actors()) {
            stage(actor).ifPresent(cast::add);
        }
        return new ManualScene(cast);
    }

    public void tick() {
        ticks++;
        for (Staged staged : cast) {
            staged.tick(ticks);
        }
    }

    /**
     * Draws the scene inside {@code area}, which the caller has framed. The pets and props go
     * in first and the items over them, as a screen lays one thing over another in the order
     * it is drawn.
     */
    public void draw(GuiGraphicsExtractor graphics, GuiSurface surface, Rect area, float partialTick) {
        float blockPixels = area.height() / BLOCKS_PER_PANEL;
        int groundY = area.bottom() - GROUND;
        graphics.enableScissor(area.x(), area.y(), area.right(), area.bottom());
        for (Staged staged : cast) {
            if (!(staged instanceof StagedItem)) {
                staged.draw(graphics, area, groundY, blockPixels, ticks + partialTick);
            }
        }
        for (Staged staged : cast) {
            if (staged instanceof StagedItem) {
                staged.draw(graphics, area, groundY, blockPixels, ticks + partialTick);
            }
        }
        graphics.disableScissor();
        for (Staged staged : cast) {
            staged.drawSpeech(surface, area);
        }
    }

    private static Optional<Staged> stage(ManualPage.Actor actor) {
        if (actor.pet().isPresent()) {
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(actor.pet().get());
            if (type.isPresent() && type.get().create(Minecraft.getInstance().level, EntitySpawnReason.LOAD) instanceof AbstractPet pet) {
                return Optional.of(new StagedPet(actor, dress(pet, actor)));
            }
            Constants.LOG.warn("[chiikawa-manual] {} is not a pet, so it is left out of its panel", actor.pet().get());
            return Optional.empty();
        }
        if (actor.prop().isPresent()) {
            return Optional.of(new StagedProp(actor));
        }
        ItemStack stack = resolve(actor.item().orElseThrow());
        return stack.isEmpty() ? Optional.empty() : Optional.of(new StagedItem(actor, stack));
    }

    private static AbstractPet dress(AbstractPet pet, ManualPage.Actor actor) {
        actor.hold().map(ManualScene::resolve).ifPresent(stack -> pet.setItemSlot(EquipmentSlot.MAINHAND, stack));
        actor.bag().map(ManualScene::resolve).ifPresent(stack -> pet.setItemSlot(EquipmentSlot.CHEST, stack));
        // The job is the server's to write, and this pet has no server: it takes the one its
        // tool calls for, so it stands the way a pet with that tool does.
        pet.setPetJobId(pet.jobFromMainhand().id());
        pet.setPetDirective(actor.sit() ? PetDirective.STAY : PetDirective.FOLLOW);
        return pet;
    }

    /** An item by id, or the first item of a tag — {@code #chiikawa:currency} being whatever money is. */
    static ItemStack resolve(ExtraCodecs.TagOrElementLocation ref) {
        if (ref.tag()) {
            return BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, ref.id()))
                .flatMap(tag -> tag.stream().findFirst())
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
        }
        Item item = BuiltInRegistries.ITEM.getValue(ref.id());
        return new ItemStack(item);
    }

    /** Something on stage. */
    private abstract static class Staged {
        final ManualPage.Actor actor;

        Staged(ManualPage.Actor actor) {
            this.actor = actor;
        }

        void tick(int ticks) {
        }

        abstract void draw(GuiGraphicsExtractor graphics, Rect area, int groundY, float blockPixels, float time);

        void drawSpeech(GuiSurface surface, Rect area) {
        }

        float screenX(Rect area) {
            return area.x() + actor.x() * area.width();
        }

        float screenY(Rect area, int groundY) {
            return groundY - actor.y() * area.height();
        }

        /**
         * From the middle of a picture the size of the panel to where this stands, in blocks
         * of {@code size} screen pixels, with y running down as on the screen.
         */
        Vector3f fromMiddle(Rect area, int groundY, float size) {
            return new Vector3f((screenX(area) - (area.x() + area.right()) / 2.0F) / size,
                (screenY(area, groundY) - (area.y() + area.bottom()) / 2.0F) / size, 0.0F);
        }
    }

    private static final class StagedPet extends Staged {
        private final AbstractPet pet;

        StagedPet(ManualPage.Actor actor, AbstractPet pet) {
            super(actor);
            this.pet = pet;
        }

        @Override
        void tick(int ticks) {
            pet.tickCount++;
            if (actor.walk()) {
                pet.walkAnimation.update(WALK_SPEED, 1.0F, 1.0F);
            }
            // Halfway into the first round, so a move is under way by the time anyone looks.
            if ((ticks + actor.motion().every() / 2) % actor.motion().every() == 0) {
                actor.action().ifPresent(pet::playActionAnimation);
                actor.reaction().ifPresent(pet::playReactionAnimation);
                if (!actor.motion().play().isEmpty()) {
                    pet.playAnimation(actor.motion().play());
                }
            }
        }

        @Override
        void draw(GuiGraphicsExtractor graphics, Rect area, int groundY, float blockPixels, float time) {
            // Facing the reader is a body turned to 180, as the game's own inventory has it;
            // a smaller yaw turns the pet towards the panel's right.
            float yaw = 180.0F - actor.facing();
            pet.yBodyRot = yaw;
            pet.yBodyRotO = yaw;
            pet.setYRot(yaw);
            pet.yRotO = yaw;
            pet.yHeadRot = yaw;
            pet.yHeadRotO = yaw;
            pet.setXRot(0.0F);
            float size = blockPixels * actor.scale();
            float partialTick = time - (float) Math.floor(time);
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            // Drawn into a picture the size of the panel, as the game draws a pet on the
            // inventory screen, and put where the panel says from the middle of that picture.
            ChiikawaEntityRenderer.drawPortrait(() -> {
                EntityRenderState state = dispatcher.getRenderer(pet).createRenderState(pet, partialTick);
                state.shadowPieces.clear();
                graphics.entity(state, size, fromMiddle(area, groundY, size), Axis.ZP.rotationDegrees(180.0F), null,
                    area.x(), area.y(), area.right(), area.bottom());
            });
        }

        /** What the pet says, in a bubble near the top of the panel above it. */
        @Override
        void drawSpeech(GuiSurface surface, Rect area) {
            if (actor.motion().say().isEmpty()) {
                return;
            }
            Bubble bubble = new Bubble(Component.translatable(actor.motion().say().get()).getString());
            int speaker = Math.round(screenX(area));
            int width = bubble.width(surface);
            int x = Mth.clamp(speaker - width / 2, area.x() + UiStyle.TIGHT, area.right() - UiStyle.TIGHT - width);
            bubble.draw(surface, x, area.y() + UiStyle.GAP, speaker);
        }
    }

    private static final class StagedProp extends Staged {
        StagedProp(ManualPage.Actor actor) {
            super(actor);
        }

        @Override
        void draw(GuiGraphicsExtractor graphics, Rect area, int groundY, float blockPixels, float time) {
            float size = blockPixels * actor.scale();
            // Turned the way a pet is, so a prop and a pet given the same facing face alike.
            Quaternionf turned = Axis.ZP.rotationDegrees(180.0F).mul(Axis.YP.rotationDegrees(actor.facing()));
            Services.CLIENT.submitPicture(graphics, new GuiPropRenderer.State(actor.prop().orElseThrow(),
                fromMiddle(area, groundY, size), turned, area.x(), area.y(), area.right(), area.bottom(), size));
        }
    }

    /** An item, drawn flat as an icon: the way an item looks everywhere else on a screen. */
    private static final class StagedItem extends Staged {
        private final ItemStack stack;

        StagedItem(ManualPage.Actor actor, ItemStack stack) {
            super(actor);
            this.stack = stack;
        }

        @Override
        void draw(GuiGraphicsExtractor graphics, Rect area, int groundY, float blockPixels, float time) {
            float bob = actor.motion().bob() ? Mth.sin(time * 0.2F) * BOB_PIXELS : 0.0F;
            float scale = actor.scale();
            graphics.pose().pushMatrix();
            graphics.pose().translate(screenX(area), screenY(area, groundY) - UiStyle.ICON * scale / 2.0F + bob);
            graphics.pose().scale(scale, scale);
            graphics.item(stack, -UiStyle.ICON / 2, -UiStyle.ICON / 2);
            graphics.pose().popMatrix();
        }
    }
}
