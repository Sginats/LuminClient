package com.lumin.luminclient.gui;

import com.lumin.luminclient.flip.FlipEngine;
import com.lumin.luminclient.flip.FlipOpportunity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * In-game screen showing the current top flips.
 */
public class FlipsScreen extends Screen {

    private final FlipEngine flipEngine;

    public FlipsScreen(FlipEngine flipEngine) {
        super(Text.literal("LuminClient - Top Flips"));
        this.flipEngine = flipEngine;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), b -> {
            flipEngine.refreshNow();
        }).dimensions(cx - 100, this.height - 30, 95, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> {
            this.close();
        }).dimensions(cx + 5, this.height - 30, 95, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);

        List<FlipOpportunity> flips = flipEngine.getLatestFlips();
        int y = 40;
        int rowH = 14;
        int maxRows = Math.min(flips.size(), 20);

        context.drawTextWithShadow(this.textRenderer, "Item", 30, y - 12, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, "Buy", 240, y - 12, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, "Sell", 320, y - 12, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, "Profit", 400, y - 12, 0xAAAAAA);

        for (int i = 0; i < maxRows; i++) {
            FlipOpportunity f = flips.get(i);
            String name = f.display.length() > 22 ? f.display.substring(0, 22) : f.display;
            context.drawTextWithShadow(this.textRenderer, name, 30, y, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, String.format("%.0f", f.buyAt), 240, y, 0x55FF55);
            context.drawTextWithShadow(this.textRenderer, String.format("%.0f", f.sellAt), 320, y, 0xFF5555);
            context.drawTextWithShadow(this.textRenderer,
                    String.format("+%.0f (%.1f%%)", f.profitPerUnit, f.profitPercent), 400, y, 0xFFFF55);
            y += rowH;
        }

        if (flips.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    "No flips yet. Press Refresh or wait for the next scan.",
                    this.width / 2, this.height / 2, 0xAAAAAA);
        }
    }
}
