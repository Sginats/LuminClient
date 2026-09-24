package com.lumin.luminclient.gui;

import com.lumin.luminclient.flip.FlipEngine;
import com.lumin.luminclient.flip.FlipOpportunity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
    private SortMode sortMode = SortMode.TOTAL_PROFIT;
    private FilterMode filterMode = FilterMode.ALL;
    private int page = 0;
    private static final int ROWS_PER_PAGE = 18;

    private enum SortMode {
        TOTAL_PROFIT("Sort: Total"),
        PROFIT_PERCENT("Sort: %"),
        NAME("Sort: Name");
        private final String label;
        SortMode(String label) { this.label = label; }
    }

    private enum FilterMode {
        ALL("Filter: All"),
        BAZAAR("Filter: Bazaar"),
        BIN("Filter: BIN");
        private final String label;
        FilterMode(String label) { this.label = label; }
    }

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

        this.addDrawableChild(ButtonWidget.builder(Text.literal(sortMode.label), b -> {
            sortMode = SortMode.values()[(sortMode.ordinal() + 1) % SortMode.values().length];
            b.setMessage(Text.literal(sortMode.label));
            page = 0;
        }).dimensions(cx - 250, btnY, 130, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(filterMode.label), b -> {
            filterMode = FilterMode.values()[(filterMode.ordinal() + 1) % FilterMode.values().length];
            b.setMessage(Text.literal(filterMode.label));
            page = 0;
        }).dimensions(cx + 120, btnY, 130, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            page = Math.max(0, page - 1);
        }).dimensions(cx - 40, btnY - 24, 20, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            int pageCount = pageCount();
            if (page < pageCount - 1) {
                page++;
            }
        }).dimensions(cx + 20, btnY - 24, 20, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Hint"), b -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("[Lumin] Use .lumin top or /lumin top for a chat summary."), false);
            }
        }).dimensions(cx - 30, btnY - 24, 60, 20).build());
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
        List<FlipOpportunity> flips = transformFlips(flipEngine.getLatestFlips());
        int y = headerY + 18;
        int rowH = 16;
        int maxRowsByHeight = Math.min(ROWS_PER_PAGE, (y1 - y - 10) / rowH);
        int totalPages = Math.max(1, (int) Math.ceil((double) flips.size() / (double) Math.max(1, maxRowsByHeight)));
        if (page >= totalPages) page = totalPages - 1;
        int start = page * Math.max(1, maxRowsByHeight);
        int end = Math.min(flips.size(), start + Math.max(1, maxRowsByHeight));
        int maxRows = end - start;

        for (int i = 0; i < maxRows; i++) {
            FlipOpportunity f = flips.get(start + i);

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
        context.drawCenteredTextWithShadow(this.textRenderer,
                "Page " + (page + 1) + "/" + totalPages,
                this.width / 2, y1 - 18, TEXT_DIM);

        super.render(context, mouseX, mouseY, delta);
    }

    private static String formatCoins(long v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000)     return String.format("%.1fk", v / 1_000.0);
        return Long.toString(v);
    }

    private List<FlipOpportunity> transformFlips(List<FlipOpportunity> in) {
        List<FlipOpportunity> out = new ArrayList<FlipOpportunity>();
        for (FlipOpportunity f : in) {
            switch (filterMode) {
                case BAZAAR:
                    if (f.type != FlipOpportunity.Type.BAZAAR_MARGIN) continue;
                    break;
                case BIN:
                    if (f.type != FlipOpportunity.Type.BIN_SNIPE) continue;
                    break;
                case ALL:
                default:
                    break;
            }
            out.add(f);
        }
        switch (sortMode) {
            case PROFIT_PERCENT:
                out.sort(Comparator.comparingDouble((FlipOpportunity f) -> f.profitPercent).reversed());
                break;
            case NAME:
                out.sort(Comparator.comparing(f -> f.display.toLowerCase(Locale.ROOT)));
                break;
            case TOTAL_PROFIT:
            default:
                out.sort(Comparator.comparingDouble(FlipOpportunity::totalProfit).reversed());
                break;
        }
        return out;
    }

    private int pageCount() {
        int panelH = this.height - 80;
        int rowStart = 40 + 38 + 18;
        int y1 = 40 + panelH;
        int maxRowsByHeight = Math.min(ROWS_PER_PAGE, (y1 - rowStart - 10) / 16);
        maxRowsByHeight = Math.max(1, maxRowsByHeight);
        int size = transformFlips(flipEngine.getLatestFlips()).size();
        return Math.max(1, (int) Math.ceil((double) size / (double) maxRowsByHeight));
    }
}
