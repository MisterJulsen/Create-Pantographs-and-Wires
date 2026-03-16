package de.mrjulsen.paw.fabric.compat.sodium;

import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.paw.PantographsAndWires;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class IncompatabilityScreen extends Screen {

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
        label = MultiLineLabel.create(font, description, width / 2);

        Button btnQuit = Button.builder(TextUtils.translate("menu.quit"), (b) -> {
            Minecraft.getInstance().stop();
        }).bounds(width / 2 - 100, height - 50, 200, 20).build();
        addRenderableWidget(btnQuit);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderDirtBackground(guiGraphics);
        guiGraphics.drawString(font, title, width / 2, 60, 0xFFFFFFFF);
        guiGraphics.drawString(font, subtitle, width / 2, 80, 0xFFFF8080);
        label.renderCentered(guiGraphics, width / 2, 120, font.lineHeight, 0xFFDBDBDB);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}