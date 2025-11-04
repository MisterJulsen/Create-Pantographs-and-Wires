package de.mrjulsen.paw.client.gui;

import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLColor;

public class ModGuiUtils {
    public static void renderRoundedBox(DLGuiGraphics graphics, int x, int y, int w, int h, DLColor color) {        
        GuiUtils.fill(graphics, x + 1, y, w - 2, h, color);
        GuiUtils.fill(graphics, x, y + 1, 1, h - 2, color);
        GuiUtils.fill(graphics, x + w - 1, y + 1, 1, h - 2, color);
    }
}
