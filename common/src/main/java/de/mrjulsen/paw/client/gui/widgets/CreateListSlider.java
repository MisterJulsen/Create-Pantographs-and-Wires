package de.mrjulsen.paw.client.gui.widgets;

import java.util.List;

import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLSlider;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.CursorType;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLTexture;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.paw.client.gui.ModGuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class CreateListSlider<T extends IIconRepresentable & ITranslatable> extends DLSlider {

    protected static final DLTexture TEXTURE = new DLTexture(DLUtils.resourceLocation("create", "textures/gui/value_settings.png"), 256, 256);
    private static final int SLIDER_BAR_SIZE = 8;
    private static final int SLIDER_DOCK_WIDTH = 7;
    private static final int SLIDER_SIZE = 22;
    
    private final List<T> values;

    public CreateListSlider(int x, int y, int w, int h, List<T> values) {
        super(x, y, w, h);
        this.cursor.set(CursorType.HRESIZE);
        this.values = values;
        max.set((double)values.size() - 1);
        sliderWidth.set(SLIDER_SIZE);
    }
    
    public T getValue() {
        return values.get(value.get().intValue());
    }
    
    public void setValue(T value) {
        this.value.set((double)values.indexOf(value));
    }

    @Override
    protected void updateSliderValue(double mouseX, double mouseY) {
        double value = (max.get() - min.get()) / (width() - sliderWidth.get()) * (mouseX - sliderWidth.get() / 2D);
        value = Math.round(value / step.get().doubleValue()) * step.get().doubleValue();
        this.value.set(value + min.get());
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        int k = sliderWidth.get() / 2 - SLIDER_DOCK_WIDTH / 2;
        int i = (height() - SLIDER_BAR_SIZE) / 2;
        GuiUtils.drawTexture(TEXTURE, graphics, k, i, SLIDER_DOCK_WIDTH, SLIDER_BAR_SIZE);
        GuiUtils.drawTexture(TEXTURE, graphics, width() - SLIDER_DOCK_WIDTH - k, i, SLIDER_DOCK_WIDTH, SLIDER_BAR_SIZE);
        GuiUtils.drawTexture(TEXTURE, graphics, k + SLIDER_DOCK_WIDTH, i, width() - SLIDER_DOCK_WIDTH * 2 - k * 2, SLIDER_BAR_SIZE, 7, 0);

        int sliderX = (int)((double)(width() - sliderWidth.get()) / (max.get() - min.get()) * (value.get() - min.get()));
        GuiUtils.drawTexture(TEXTURE, graphics, sliderX, height() / 2 - SLIDER_SIZE / 2, SLIDER_SIZE, SLIDER_SIZE, 0, 43);
        values.get(value.get().intValue()).getIcon().render(graphics, sliderX + 3, 3);
    }
    
    @Override
    public void renderFrontLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {        
        if (isSelected() || isDragged()) {
            Font font = Minecraft.getInstance().font;
            T e = values.get(value.get().intValue());
            Component valueTxt = TextUtils.translate(e.getTranslationKey());
            int halfWidth = Math.max(font.width(text.get()) / 2, font.width(valueTxt) / 2);

            ModGuiUtils.renderRoundedBox(graphics, width() / 2 - halfWidth - 3, -font.lineHeight * 2 - 6, halfWidth * 2 + 6, font.lineHeight * 2 + 5, DLColor.fromInt(0xAA000000));
            GuiUtils.drawString(graphics, font, width() / 2, -font.lineHeight - 2, valueTxt, DLColor.fromInt(0xFF94B5DD), ETextAlignment.CENTER, false);
            GuiUtils.drawString(graphics, font, width() / 2, -font.lineHeight * 2 - 4, text.get(), DLColor.WHITE, ETextAlignment.CENTER, false);
        }
    }
    
}
