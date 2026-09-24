package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.ItemIcon;
import com.dwinovo.chiikawa.client.ui.mc.UiButton;
import com.dwinovo.chiikawa.network.ShopPayloads.PriceView;
import com.dwinovo.chiikawa.network.ShopPayloads.ShopTradePayload;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.shop.Wallet;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Icon;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Arrow;
import com.dwinovo.chiikawa.ui.widget.Price;
import com.dwinovo.chiikawa.ui.widget.Slot;
import com.dwinovo.chiikawa.ui.widget.TitledPanel;
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
 * written out beside them — pressing "Buy 3" with an emerald after it is buying it for
 * three emeralds, or three of whatever a pack has made money of. The customer's own money
 * is in the corner, counted the same way.
 */
public class ShopScreen extends Screen {
    private static final int PANEL_W = 236;
    private static final int ROWS = 7;
    private static final int ROW_H = UiStyle.ROW_H;

    private final BlockPos shop;
    private final List<PriceView> prices;
    private int page;
    private int leftPos, topPos, panelHeight;
    private int listY, footerY;
    /** Each column of buttons as wide as its widest price, so the prices line up; 0 for none. */
    private int buyWidth, sellWidth;
    /** What prices are in, pictured beside every one of them. */
    private Icon coin = Icon.NONE;

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
        this.listY = TitledPanel.contentY(topPos);
        this.footerY = topPos + panelHeight - UiStyle.PAD - UiStyle.CONTROL_H;
        this.coin = new ItemIcon(Wallet.coins(1));
        this.buyWidth = columnWidth(true);
        this.sellWidth = columnWidth(false);
        rebuildButtons();
    }

    private int columnWidth(boolean buying) {
        int widest = 0;
        for (PriceView price : prices) {
            int amount = buying ? price.buy() : price.sell();
            if (amount > 0) {
                widest = Math.max(widest, Price.width(this.font.width(label(buying, amount))) + 2 * UiStyle.GAP);
            }
        }
        return widest;
    }

    private void rebuildButtons() {
        clearWidgets();
        clampPage();
        int start = page * ROWS;
        int end = Math.min(prices.size(), start + ROWS);
        for (int i = start; i < end; i++) {
            PriceView price = prices.get(i);
            int rowY = listY + (i - start) * ROW_H;
            int sellX = leftPos + PANEL_W - UiStyle.PAD - sellWidth;
            int buyX = (sellWidth > 0 ? sellX - UiStyle.GAP : sellX) - buyWidth;
            if (price.sell() > 0) {
                addRenderableWidget(tradeButton(price, false, sellX, sellWidth, rowY));
            }
            if (price.buy() > 0) {
                addRenderableWidget(tradeButton(price, true, buyX, buyWidth, rowY));
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
    private UiButton tradeButton(PriceView price, boolean buying, int x, int width, int rowY) {
        Component label = label(buying, buying ? price.buy() : price.sell());
        UiButton button = new UiButton(x, UiStyle.centerIn(rowY, ROW_H, UiStyle.CONTROL_H),
            width, UiStyle.CONTROL_H, label,
            (surface, area, argb) -> Price.drawCentered(surface, coin, label.getString(), area, argb),
            () -> Services.NETWORK.sendToServer(new ShopTradePayload(shop, price.item(), buying)));
        button.active = buying ? canAfford(price) : holds(price);
        return button;
    }

    private static Component label(boolean buying, int money) {
        return Component.translatable(buying ? "screen.chiikawa.shop.buy" : "screen.chiikawa.shop.sell", money);
    }

    /**
     * The panel and its rows, drawn right after the game dims what is behind the screen and
     * before the buttons, as the music box draws its own: drawn after them, the panel would
     * cover them.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        GuiSurface surface = new GuiSurface(graphics, this.font);
        TitledPanel.draw(surface, leftPos, topPos, PANEL_W, panelHeight, this.title.getString());
        // What the customer has to spend, where a shopper looks first.
        Price.drawRight(surface, coin, String.valueOf(purse()), leftPos + PANEL_W - UiStyle.PAD,
            topPos, UiStyle.TITLE_H, UiTheme.TEXT_MUTED);

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
        int buttons = (buyWidth > 0 ? buyWidth + UiStyle.GAP : 0) + (sellWidth > 0 ? sellWidth + UiStyle.GAP : 0);
        Ui.textClipped(surface, name(price).getString(), nameX,
            UiStyle.centerIn(rowY, ROW_H, surface.lineHeight()),
            row.right() - buttons - nameX, UiTheme.TEXT);
    }

    /** How much money the customer is carrying. */
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
        return item(price).getName();
    }

    private static Item item(PriceView price) {
        return BuiltInRegistries.ITEM.getValue(price.item());
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
