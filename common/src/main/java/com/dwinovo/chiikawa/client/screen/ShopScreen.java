package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.ItemIcon;
import com.dwinovo.chiikawa.client.ui.mc.UiButton;
import com.dwinovo.chiikawa.network.ShopPayloads.PriceView;
import com.dwinovo.chiikawa.network.ShopPayloads.ShopTradePayload;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.shop.Wallet;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Arrow;
import com.dwinovo.chiikawa.ui.widget.Slot;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The counter, from the owner's side: what the shop sells, what it takes in, and a button
 * for each.
 *
 * <p>A row is a picture, a name and a price. What a thing costs and what it fetches are
 * the whole point of the screen, so they are on the buttons themselves rather than
 * written out beside them — pressing "3" is buying it for three.
 */
public class ShopScreen extends Screen {
    private static final int PANEL_W = 236;
    private static final int ROWS = 7;
    private static final int ROW_H = UiStyle.ROW_H;
    /** Wide enough for a price of two digits and the word in front of it. */
    private static final int TRADE_BUTTON_W = 42;

    private final BlockPos shop;
    private final List<PriceView> prices;
    private int page;
    private int leftPos, topPos, panelHeight;
    private int listY, footerY;

    public ShopScreen(BlockPos shop, List<PriceView> prices) {
        super(Component.translatable("screen.chiikawa.shop"));
        this.shop = shop;
        this.prices = List.copyOf(prices);
    }

    @Override
    protected void init() {
        this.panelHeight = UiStyle.TITLE_H + UiStyle.PAD + ROWS * ROW_H + UiStyle.PAD
            + UiStyle.CONTROL_H + UiStyle.PAD;
        this.leftPos = (this.width - PANEL_W) / 2;
        this.topPos = (this.height - panelHeight) / 2;
        this.listY = topPos + UiStyle.TITLE_H + UiStyle.PAD;
        this.footerY = topPos + panelHeight - UiStyle.PAD - UiStyle.CONTROL_H;
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        clampPage();
        int start = page * ROWS;
        int end = Math.min(prices.size(), start + ROWS);
        for (int i = start; i < end; i++) {
            PriceView price = prices.get(i);
            int rowY = listY + (i - start) * ROW_H;
            int right = leftPos + PANEL_W - UiStyle.PAD;
            if (price.sell() > 0) {
                addRenderableWidget(tradeButton(price, false, right - TRADE_BUTTON_W, rowY));
            }
            if (price.buy() > 0) {
                int x = price.sell() > 0
                    ? right - 2 * TRADE_BUTTON_W - UiStyle.GAP
                    : right - TRADE_BUTTON_W;
                addRenderableWidget(tradeButton(price, true, x, rowY));
            }
        }

        UiButton previous = new UiButton(leftPos + UiStyle.PAD, footerY, UiStyle.CONTROL_H, UiStyle.CONTROL_H,
            Component.translatable("screen.chiikawa.shop.previous_page"),
            (surface, area, argb) -> Arrow.draw(surface, area, true, argb),
            () -> turnTo(page - 1));
        previous.active = page > 0;
        addRenderableWidget(previous);

        UiButton next = new UiButton(leftPos + PANEL_W - UiStyle.PAD - UiStyle.CONTROL_H, footerY,
            UiStyle.CONTROL_H, UiStyle.CONTROL_H,
            Component.translatable("screen.chiikawa.shop.next_page"),
            (surface, area, argb) -> Arrow.draw(surface, area, false, argb),
            () -> turnTo(page + 1));
        next.active = page + 1 < pages();
        addRenderableWidget(next);
    }

    /** One side of one row's trade, with what it costs written on it. */
    private UiButton tradeButton(PriceView price, boolean buying, int x, int rowY) {
        Component label = Component.translatable(
            buying ? "screen.chiikawa.shop.buy" : "screen.chiikawa.shop.sell",
            buying ? price.buy() : price.sell());
        UiButton button = UiButton.text(x, UiStyle.centerIn(rowY, ROW_H, UiStyle.CONTROL_H),
            TRADE_BUTTON_W, UiStyle.CONTROL_H, label,
            () -> Services.NETWORK.sendToServer(new ShopTradePayload(shop, price.item(), buying)));
        button.active = buying ? canAfford(price) : holds(price);
        return button;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        GuiSurface surface = new GuiSurface(graphics, this.font);
        Ui.titledPanel(surface, leftPos, topPos, PANEL_W, panelHeight, this.title.getString());
        // What the customer has to spend, where a shopper looks first.
        Ui.textRight(surface, String.valueOf(purse()), leftPos + PANEL_W - UiStyle.PAD,
            UiStyle.centerIn(topPos, UiStyle.TITLE_H, surface.lineHeight()), UiTheme.TEXT_MUTED);

        int start = page * ROWS;
        int end = Math.min(prices.size(), start + ROWS);
        for (int i = start; i < end; i++) {
            drawRow(surface, prices.get(i), listY + (i - start) * ROW_H, mouseX, mouseY);
        }
        if (prices.isEmpty()) {
            Ui.emptyState(surface, Component.translatable("screen.chiikawa.shop.empty").getString(),
                leftPos + PANEL_W / 2, listY + UiStyle.PAD);
        }
    }

    private void drawRow(DrawSurface surface, PriceView price, int rowY, int mouseX, int mouseY) {
        Rect row = new Rect(leftPos + UiStyle.PAD, rowY, PANEL_W - 2 * UiStyle.PAD, ROW_H);
        if (row.contains(mouseX, mouseY)) {
            Ui.rowHighlight(surface, row);
        }
        Slot.draw(surface, ItemIcon.of(price.item()), row.x(), UiStyle.centerIn(rowY, ROW_H, UiStyle.SLOT));
        int nameX = row.x() + UiStyle.SLOT + UiStyle.GAP;
        int buttons = (price.buy() > 0 ? TRADE_BUTTON_W : 0) + (price.sell() > 0 ? TRADE_BUTTON_W : 0) + UiStyle.GAP;
        Ui.textClipped(surface, name(price).getString(), nameX,
            UiStyle.centerIn(rowY, ROW_H, surface.lineHeight()),
            row.right() - buttons - UiStyle.GAP - nameX, UiTheme.TEXT);
    }

    /** How many emeralds the customer is carrying. */
    private int purse() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? 0 : Wallet.count(minecraft.player.getInventory());
    }

    private boolean canAfford(PriceView price) {
        return purse() >= price.buy();
    }

    private boolean holds(PriceView price) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
            && minecraft.player.getInventory().contains(new ItemStack(item(price)));
    }

    private static Component name(PriceView price) {
        return item(price).getDescription();
    }

    private static Item item(PriceView price) {
        return BuiltInRegistries.ITEM.get(price.item());
    }

    private void turnTo(int wanted) {
        page = Math.max(0, Math.min(wanted, pages() - 1));
        rebuildButtons();
    }

    private int pages() {
        return Math.max(1, (prices.size() + ROWS - 1) / ROWS);
    }

    private void clampPage() {
        page = Math.max(0, Math.min(page, pages() - 1));
    }

    /** Buttons go dead as the purse empties, so the screen keeps up with what it shows. */
    @Override
    public void tick() {
        super.tick();
        rebuildButtons();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
