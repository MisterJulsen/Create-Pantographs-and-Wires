package de.mrjulsen.paw.client.gui.widgets;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.newgui.widgets.components.DLToggleButton;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class CreateCheckBox extends DLToggleButton {
    
    protected static final ResourceLocation TEXTURE = new ResourceLocation("create:textures/gui/value_settings.png");

    public CreateCheckBox(int x, int y, int w, int h) {
        super(x, y, w, h);
    }
    
    @Override
    public void renderMainLayer(Graphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        GuiUtils.setTint(backgroundTint.get().getAsARGB());
        if (isSelected()) {
            componentRenderer.get().renderSprite(graphics, 0, height() / 2 - 6, 12, 12, this, ButtonState.DISABLED_SELECTED);
        } else {
            componentRenderer.get().renderSprite(graphics, 0, height() / 2 - 6, 12, 12, this, ButtonState.DISABLED);
        }
        if (checked.get()) {
            GuiUtils.fill(graphics, 3, height() / 2 - 3, 6, 6, 0xFFFFFFFF);
        }
        GuiUtils.resetTint();
        
        GuiUtils.setTint(textColor.get().getAsARGB());
        GuiUtils.drawString(graphics, Minecraft.getInstance().font, 16, height() / 2 - Minecraft.getInstance().font.lineHeight / 2, text.get(), enabled.get() ? DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE : DragonLib.NATIVE_BUTTON_FONT_COLOR_DISABLED, EAlignment.LEFT, true);
        GuiUtils.resetTint();
    }
    
}
