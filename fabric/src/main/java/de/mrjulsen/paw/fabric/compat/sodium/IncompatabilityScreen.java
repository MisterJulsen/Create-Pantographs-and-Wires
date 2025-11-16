package de.mrjulsen.paw.fabric.compat.sodium;

import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.paw.PantographsAndWires;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.Component;

public class IncompatabilityScreen extends DLScreen {

    private final Component subtitle = TextUtils.translate("gui." + PantographsAndWires.MOD_ID + ".sodium_error.subtitle");
    private final Component description = TextUtils.translate("gui." + PantographsAndWires.MOD_ID + ".sodium_error.description");

    private MultiLineLabel label;

    private final Font font = Minecraft.getInstance().font;

    public IncompatabilityScreen() {
        super(TextUtils.translate("gui." + PantographsAndWires.MOD_ID + ".sodium_error.title").withStyle(ChatFormatting.BOLD));
    }

    @Override
    protected void init() {
        super.init();
        label = MultiLineLabel.create(font, description, width() / 2);

        DLButton btnQuit = addButton(width() / 2 - 100, height() - 50, 200, 20, TextUtils.translate("menu.quit"), (b) -> {
            Minecraft.getInstance().stop();
        }, null);
        btnQuit.setRenderStyle(AreaStyle.DRAGONLIB);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderDirtBackground(graphics.graphics());
        GuiUtils.drawString(graphics, font, width() / 2, 60, title, 0xFFFFFFFF, EAlignment.CENTER, true);
        GuiUtils.drawString(graphics, font, width() / 2, 80, subtitle, 0xFFFF8080, EAlignment.CENTER, true);
        label.renderCentered(graphics.graphics(), width() / 2, 120, font.lineHeight, 0xFFDBDBDB);
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
    }
    
}
