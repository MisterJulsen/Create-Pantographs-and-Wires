package de.mrjulsen.paw.compat.sodium;

import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLRichTextLabel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.RichTextComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.paw.PantographsAndWires;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class IncompatabilityScreen extends DLWindow {

    private final Component caption = TextUtils.translate("gui." + PantographsAndWires.MOD_ID + ".sodium_error.title").withStyle(ChatFormatting.BOLD);
    private final Component subtitle = TextUtils.translate("gui." + PantographsAndWires.MOD_ID + ".sodium_error.subtitle");
    private final Component description = TextUtils.translate("gui." + PantographsAndWires.MOD_ID + ".sodium_error.description");

    private final Font font = Minecraft.getInstance().font;

    public IncompatabilityScreen(DLWindowManager manager) {
        super(manager);
        fullscreen.set(true);

        DLRichTextLabel label = new DLRichTextLabel(width() / 4, 120, width() / 2, 200);
        RichTextComponent txt = new RichTextComponent();
        txt.set(description.getString());
        label.text.set(txt);
        label.anchor.set(EAlign.values());
        addComponent(label);

        DLButton quitBtn = new DLButton(width() / 2 - 100, height() - 50, 200, 20);
        quitBtn.anchor.set2(EAlign.BOTTOM);
        quitBtn.text.set(TextUtils.translate("menu.quit"));
        quitBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            Minecraft.getInstance().stop();
            return false;
        });
        addComponent(quitBtn);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        GuiUtils.drawString(graphics, font, width() / 2, 60, caption, DLColor.WHITE, ETextAlignment.CENTER, true);
        GuiUtils.drawString(graphics, font, width() / 2, 80, subtitle, DLColor.fromInt(0xFFFF8080), ETextAlignment.CENTER, true);
    }
    
}
