package de.mrjulsen.paw.client.gui.widgets;

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
import de.mrjulsen.paw.util.ModMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class CreateSlider extends DLSlider {

    protected static final DLTexture TEXTURE = new DLTexture(DLUtils.resourceLocation("create", "textures/gui/value_settings.png"), 256, 256);
    private static final int SLIDER_BAR_SIZE = 8;
    private static final int SLIDER_HEIGHT = 14;
    private static final int SLIDER_DOCK_WIDTH = 7;

    private Component text;

    public CreateSlider(int x, int y, int w, int h, Component text) {
        super(x, y, w, h);
        this.cursor.set(CursorType.HRESIZE);
        this.text = text;
        addEventListener(DLSlider.ValueRangeChangedEvent.class, (s, e) -> {
            updateSliderWidth();
            return false;
        });
        this.step.withAfterPropertyChangedCallback((o, a) -> {
            this.value.set(ModMath.snapNearest(this.value.get(), step.get()));
            updateSliderWidth();
        });
        updateSliderWidth();
    }

    protected void updateSliderWidth() {
        if (Math.abs(step.get() - Math.round(step.get())) < 1E-6) {
            this.sliderWidth.set(Math.max(Minecraft.getInstance().font.width(String.valueOf(min.get().intValue())), Minecraft.getInstance().font.width(String.valueOf(max.get().intValue()))) + 10);
        } else {
            this.sliderWidth.set(Math.max(Minecraft.getInstance().font.width(String.valueOf(min.get().doubleValue())), Minecraft.getInstance().font.width(String.valueOf(max.get().doubleValue()))) + 10);
        }
    }

    protected String getValueString() {
        return Math.abs(step.get() - Math.round(step.get())) < 1E-6 ? String.valueOf(value.get().intValue()) : String.valueOf(value.get().doubleValue());
    }

    @Override
    protected void updateSliderValue(double mouseX, double mouseY) {
        double value = (max.get() - min.get()) / (width() - sliderWidth.get()) * (mouseX - sliderWidth.get() / 2D);
        value = Math.round(value / step.get().doubleValue()) * step.get().doubleValue();
        this.value.set(value + min.get());
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        int i = (height() - SLIDER_BAR_SIZE) / 2;
        GuiUtils.drawTexture(TEXTURE, graphics, i, i, SLIDER_DOCK_WIDTH, SLIDER_BAR_SIZE);
        GuiUtils.drawTexture(TEXTURE, graphics, width() - SLIDER_DOCK_WIDTH - i, i, SLIDER_DOCK_WIDTH, SLIDER_BAR_SIZE);
        GuiUtils.drawTexture(TEXTURE, graphics, i + SLIDER_DOCK_WIDTH, i, width() - SLIDER_DOCK_WIDTH * 2 - i * 2, SLIDER_BAR_SIZE, 7, 0);

        String txt = getValueString();
        int sliderX = (int)((double)(width() - sliderWidth.get()) / (max.get() - min.get()) * (value.get() - min.get()));

        int sliderY = (height() - SLIDER_HEIGHT) / 2;
        int sliderW = max.get() <= min.get() ? width() : sliderWidth.get();
        GuiUtils.drawTexture(TEXTURE, graphics, sliderX, sliderY, 3, SLIDER_HEIGHT, 0, 9);
        GuiUtils.drawTexture(TEXTURE, graphics, sliderX + 3, sliderY, sliderW - 6, SLIDER_HEIGHT, 4, 9);
        GuiUtils.drawTexture(TEXTURE, graphics, sliderX + sliderW - 3, sliderY, 3, SLIDER_HEIGHT, 61, 9);
        GuiUtils.drawString(graphics, Minecraft.getInstance().font, sliderX + sliderW / 2, height() / 2 - Minecraft.getInstance().font.lineHeight / 2, txt, DLColor.fromInt(0xFF442000), ETextAlignment.CENTER, false);
    }

    @Override
    public void renderFrontLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {        
        if (isSelected() || isDragged()) {
            Font font = Minecraft.getInstance().font;
            Component valueTxt = TextUtils.text(String.valueOf(getValueString()));
            int halfWidth = Math.max(font.width(text) / 2, font.width(valueTxt) / 2);

            ModGuiUtils.renderRoundedBox(graphics, width() / 2 - halfWidth - 3, -font.lineHeight * 2 - 6, halfWidth * 2 + 6, font.lineHeight * 2 + 5, DLColor.fromInt(0xAA000000));
            GuiUtils.drawString(graphics, font, width() / 2, -font.lineHeight - 2, valueTxt, DLColor.fromInt(0xFF94B5DD), ETextAlignment.CENTER, false);
            GuiUtils.drawString(graphics, font, width() / 2, -font.lineHeight * 2 - 4, text, DLColor.WHITE, ETextAlignment.CENTER, false);
        }
    }
    
}
