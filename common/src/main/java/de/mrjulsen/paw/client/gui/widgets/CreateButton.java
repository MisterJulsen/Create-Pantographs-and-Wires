package de.mrjulsen.paw.client.gui.widgets;

import com.simibubi.create.foundation.gui.element.ScreenElement;

import de.mrjulsen.mcdragonlib.client.newgui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.resources.ResourceLocation;

public class CreateButton extends DLButton {

    private static final ResourceLocation TEXTURE = new ResourceLocation("create", "textures/gui/widgets.png");
    public static final int WIDTH = 18;
    public static final int HEIGHT = 18;
    private final ScreenElement icon;

    public CreateButton(int x, int y, ScreenElement icon) {
        super(x, y, WIDTH, HEIGHT);
        this.icon = icon;
    }

    @Override
    public void renderMainLayer(Graphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        if (!enabled.get()) {
            GuiUtils.drawTexture(TEXTURE, graphics, 0, 0, WIDTH * 2, 0, WIDTH, HEIGHT);
        } else if (isSelected()) {
            GuiUtils.drawTexture(TEXTURE, graphics, 0, 0, WIDTH, 0, WIDTH, HEIGHT);
        } else {
            GuiUtils.drawTexture(TEXTURE, graphics, 0, 0, 0, 0, WIDTH, HEIGHT);
        }

        icon.render(graphics.graphics(), 1, 1);
    }
    
}
