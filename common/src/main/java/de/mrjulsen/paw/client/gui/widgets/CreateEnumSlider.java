package de.mrjulsen.paw.client.gui.widgets;

import de.mrjulsen.mcdragonlib.client.newgui.widgets.components.DLSlider;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.core.ITranslatableEnum;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.client.gui.ModGuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CreateEnumSlider<T extends Enum<T> & IIconEnum & ITranslatableEnum> extends DLSlider {

    protected static final ResourceLocation TEXTURE = new ResourceLocation("create:textures/gui/value_settings.png");
    private static final int SLIDER_BAR_SIZE = 8;
    private static final int SLIDER_DOCK_WIDTH = 7;
    private static final int SLIDER_SIZE = 22;
    
    private final Class<T> clazz;

    public CreateEnumSlider(int x, int y, int w, int h, Class<T> clazz) {
        super(x, y, w, h);
        this.clazz = clazz;
        max.set((double)clazz.getEnumConstants().length - 1);
        sliderWidth.set(SLIDER_SIZE);
    }    

    @Override
    protected void updateSliderValue(double mouseX, double mouseY) {
        double value = (max.get() - min.get()) / (width() - sliderWidth.get()) * (mouseX - sliderWidth.get() / 2D);
        value = Math.round(value / step.get().doubleValue()) * step.get().doubleValue();
        this.value.set(value + min.get());
    }

    @Override
    public void renderMainLayer(Graphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        int k = sliderWidth.get() / 2 - SLIDER_DOCK_WIDTH / 2;
        int i = (height() - SLIDER_BAR_SIZE) / 2;
        GuiUtils.drawTexture(TEXTURE, graphics, k, i, 0, 0, SLIDER_DOCK_WIDTH, SLIDER_BAR_SIZE);
        GuiUtils.drawTexture(TEXTURE, graphics, width() - SLIDER_DOCK_WIDTH - k, i, 0, 0, SLIDER_DOCK_WIDTH, SLIDER_BAR_SIZE);
        GuiUtils.drawTexture(TEXTURE, graphics, k + SLIDER_DOCK_WIDTH, i, 7, 0, width() - SLIDER_DOCK_WIDTH * 2 - k * 2, SLIDER_BAR_SIZE);

        int sliderX = (int)((double)(width() - sliderWidth.get()) / (max.get() - min.get()) * (value.get() - min.get()));
        GuiUtils.drawTexture(TEXTURE, graphics, sliderX, height() / 2 - SLIDER_SIZE / 2, 0, 43, SLIDER_SIZE, SLIDER_SIZE);
        clazz.getEnumConstants()[value.get().intValue()].getIcon().render(graphics, sliderX + 3, 3);
    }
    
    @Override
    public void renderFrontLayer(Graphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {        
        if (isSelected() || isDragged()) {
            Font font = Minecraft.getInstance().font;
            T e = clazz.getEnumConstants()[value.get().intValue()];
            Component titleTxt = TextUtils.translate(e.getEnumTranslationKey(PantographsAndWires.MOD_ID));
            Component valueTxt = TextUtils.translate(e.getValueTranslationKey(PantographsAndWires.MOD_ID));
            int halfWidth = Math.max(font.width(titleTxt) / 2, font.width(valueTxt) / 2);

            ModGuiUtils.renderRoundedBox(graphics, width() / 2 - halfWidth - 3, -font.lineHeight * 2 - 6, halfWidth * 2 + 6, font.lineHeight * 2 + 5, 0xAA000000);
            GuiUtils.drawString(graphics, font, width() / 2, -font.lineHeight - 2, valueTxt, 0xFF94B5DD, EAlignment.CENTER, false);
            GuiUtils.drawString(graphics, font, width() / 2, -font.lineHeight * 2 - 4, titleTxt, 0xFFFFFFFF, EAlignment.CENTER, false);
        }
    }
    
}
