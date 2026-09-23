package com.lumin.luminclient.gui;

import com.lumin.luminclient.flip.FlipEngine;
import com.lumin.luminclient.flip.FlipOpportunity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Modern, clean flips screen.
 *
 * Design goals:
 *  - simple: just a list of the best flips, biggest profit first
 *  - aesthetic: soft dark panel, subtle header, color-coded profit
 *  - readable: generous spacing, clear columns, no clutter
 */
public class FlipsScreen extends Screen {

    // Palette
    private static final int BG_PANEL   = 0xCC14161A;  // semi-transparent dark
    private static final int BG_HEADER  = 0xFF1E2229;  // header bar
    private static final int ACCENT     = 0xFF5BC0DE;  // cyan accent
    private static final int TEXT_MAIN  = 0xFFEAEAEA;
    private static final int TEXT_DIM   = 0xFF9AA0A6;
    private static final int PROFIT     = 0xFF6FCF97;  // green
    private static final int BUY        = 0xFF6FCF97;
    private static final int SELL       = 0xFFEB5757;  // red

    private final FlipEngine flipEngine;

    public FlipsScreen(FlipEngine flipEngine) {
        super(Text.literal("LuminClient"));
        this.flipEngine = flipEngine;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int btnY = this.height - 32;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), b -> {
            flipEngine.refreshNow();
        }).dimensions(cx - 110, btnY, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> {
            this.close();
        }).dimensions(cx + 10, btnY, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int panelW = Math.min(520, this.width - 40);
        int panelH = this.height - 80;
        int x0 = (this.width - panelW) / 2;
        int y0 = 40;
        int x1 = x0 + panelW;
        int y1 = y0 + panelH;

        // Panel
        context.fill(x0, y0, x1, y1, BG_PANEL);
        // Header bar
        context.fill(x0, y0, x1, y0 + 26, BG_HEADER);
        // Accent underline
        context.fill(x0, y0 + 26, x1, y0 + 28, ACCENT);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, "LuminClient  •  Top Flips",
                this.width / 2, y0 + 9, TEXT_MAIN);

        // Column headers
        int colName = x0 + 16;
        int colBuy  = x0 + panelW - 260;
        int colSell = x0 + panelW - 170;
        int colProf = x0 + panelW - 90;
        int headerY = y0 + 38;

        context.drawTextWithShadow(this.textRenderer, "Item", colName, headerY, TEXT_DIM);
        context.drawTextWithShadow(this.textRenderer, "Buy",  colBuy,  headerY, TEXT_DIM);
        context.drawTextWithShadow(this.textRenderer, "Sell", colSell, headerY, TEXT_DIM);
        context.drawTextWithShadow(this.textRenderer, "Profit", colProf, headerY, TEXT_DIM);

        // Rows
        List<FlipOpportunity> flips = flipEngine.getLatestFlips();
        int y = headerY + 18;
        int rowH = 16;
        int maxRows = Math.min(flips.size(), (y1 - y - 10) / rowH);

        for (int i = 0; i < maxRows; i++) {
            FlipOpportunity f = flips.get(i);

            // zebra striping for readability
            if (i % 2 == 0) {
                context.fill(x0 + 8, y - 2, x1 - 8, y + rowH - 3, 0x22FFFFFF);
            }

            String name = f.display.length() > 24 ? f.display.substring(0, 24) + "…" : f.display;
            context.drawTextWithShadow(this.textRenderer, name, colName, y, TEXT_MAIN);
            context.drawTextWithShadow(this.textRenderer, formatCoins(f.buyAt),  colBuy,  y, BUY);
            context.drawTextWithShadow(this.textRenderer, formatCoins(f.sellAt), colSell, y, SELL);
            context.drawTextWithShadow(this.textRenderer,
                    "+" + formatCoins(f.profitPerUnit) + " (" + String.format("%.1f", f.profitPercent) + "%)",
                    colProf, y, PROFIT);
            y += rowH;
        }

        if (flips.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    "No flips yet. Press Refresh or wait for the next scan.",
                    this.width / 2, (y0 + y1) / 2, TEXT_DIM);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private static String formatCoins(double v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000)     return String.format("%.1fk", v / 1_000.0);
        return String.format("%.0f", v);
    }
}
