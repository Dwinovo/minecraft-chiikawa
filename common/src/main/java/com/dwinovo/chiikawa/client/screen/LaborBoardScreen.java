package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.network.BoardPayloads.SlipView;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The day's slips on a labor board: what work is up, how much of it, and whose pet took
 * it. Read-only — pets take their own slips, the owner only looks.
 */
public class LaborBoardScreen extends Screen {
    /** Vanilla's parchment panel, 248x166 of a 256x256 sheet. */
    private static final ResourceLocation PANEL = new ResourceLocation("textures/gui/demo_background.png");
    private static final int PANEL_WIDTH = 248;
    private static final int PANEL_HEIGHT = 166;
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int TITLE_Y = 12;
    private static final int FIRST_LINE_Y = 36;
    private static final int LINE_HEIGHT = 22;
    private static final int TEXT_X = 16;

    private final List<SlipView> slips;
    private int leftPos;
    private int topPos;

    public LaborBoardScreen(List<SlipView> slips) {
        super(Component.translatable("screen.chiikawa.labor_board"));
        this.slips = slips;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.blit(PANEL, leftPos, topPos, 0.0F, 0.0F, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);
        graphics.drawCenteredString(this.font, this.title, leftPos + PANEL_WIDTH / 2, topPos + TITLE_Y, TEXT_COLOR);

        if (slips.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("screen.chiikawa.labor_board.empty"),
                leftPos + TEXT_X, topPos + FIRST_LINE_Y, TEXT_COLOR, false);
            return;
        }
        int y = topPos + FIRST_LINE_Y;
        for (SlipView slip : slips) {
            graphics.drawString(this.font, describe(slip), leftPos + TEXT_X, y, TEXT_COLOR, false);
            graphics.drawString(this.font, status(slip), leftPos + TEXT_X + 8, y + 10,
                slip.taker().isEmpty() ? TEXT_COLOR : ChatFormatting.DARK_GREEN.getColor(), false);
            y += LINE_HEIGHT;
        }
    }

    /** "Weeding · 12 · Farmer" */
    private static Component describe(SlipView slip) {
        return Component.translatable("screen.chiikawa.labor_board.slip",
            Component.translatable(translationKey("pet_task", slip.type())),
            slip.target(),
            Component.translatable(jobKey(slip.capability())));
    }

    private static Component status(SlipView slip) {
        return slip.taker().isEmpty()
            ? Component.translatable("screen.chiikawa.labor_board.open")
            : Component.translatable("screen.chiikawa.labor_board.taken", slip.taker());
    }

    private static String translationKey(String prefix, ResourceLocation id) {
        return prefix + "." + id.getNamespace() + "." + id.getPath();
    }

    /** Job names are already translated for the backpack tooltip. */
    private static String jobKey(ResourceLocation capability) {
        return "tooltip." + capability.getNamespace() + ".pet_job." + capability.getPath();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
