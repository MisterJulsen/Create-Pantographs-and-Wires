package de.mrjulsen.paw.client.gui.screens;

import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.newgui.events.DLGuiCommonEvents;
import de.mrjulsen.mcdragonlib.client.newgui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.newgui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.newgui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.client.gui.widgets.CreateButton;
import de.mrjulsen.paw.client.gui.widgets.CreateListSlider;
import de.mrjulsen.paw.registry.ModNetworkAccessor;
import de.mrjulsen.paw.registry.ModNetworkAccessor.WireSettingsData;
import de.mrjulsen.paw.registry.ModWireRegistry;
import de.mrjulsen.wires.item.IPawWireItemBase;
import de.mrjulsen.wires.item.IWireItemBase;
import de.mrjulsen.wires.item.MultiWireItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class WireTypeSelectionScreen extends DLWindow {
    
    private static final ResourceLocation TEXTURE = new ResourceLocation(PantographsAndWires.MOD_ID, "textures/gui/quick_settings.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int GUI_WIDTH = 192;
    private static final int GUI_HEIGHT = 106;

    private final Component title = TextUtils.translate("gui." + PantographsAndWires.MOD_ID + ".wire_selection.title");
    private final Component txtInstruction = TextUtils.translate("gui." + PantographsAndWires.MOD_ID + ".wire_selection.instruction");
    private final Component txtWireType = TextUtils.translate("gui." + PantographsAndWires.MOD_ID + ".wire_selection.wire_type");

    private IPawWireItemBase selectedType;

    public WireTypeSelectionScreen(DLWindowManager manager, ItemStack stack) {
        super(manager);
        if (!(stack.getItem() instanceof MultiWireItem item)) {
            throw new IllegalArgumentException("This item is no MultiWireItem.");
        }
        this.selectedType = (IPawWireItemBase)item.getSubType(stack);

        setSize(GUI_WIDTH, GUI_HEIGHT);
        setPosition(Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - width() / 2, Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2 - height() / 2);

        addEventListener(DLGuiStandardEvents.CloseEvent.class, (s, e) -> {
            WireSettingsData data = new WireSettingsData(selectedType);
            MultiWireItem.setNbt(stack, data);
            DataAccessor.getFromServer(data, ModNetworkAccessor.UPDATE_WIRE_SETTINGS, $ -> {});
            return false;
        });
        
        int w = ModWireRegistry.WIRE_SUBTYPES_REGISTRY.getAll().size() * 20;
        CreateListSlider<IPawWireItemBase> typeSelection = new CreateListSlider<>(width() / 2 - w / 2, 43, w, 20, ModWireRegistry.WIRE_SUBTYPES_REGISTRY.getAll());
        typeSelection.text.set(txtWireType);
        typeSelection.setValue(selectedType);
        typeSelection.addEventListener(DLGuiCommonEvents.ValueChangedEvent.class, (s, e) -> {
            this.selectedType = typeSelection.getValue();
            return false;
        });
        addComponent(typeSelection);
        
        CreateButton doneBtn = new CreateButton(width() - 7 - CreateButton.WIDTH, height() - 6 - CreateButton.HEIGHT, AllIcons.I_CONFIRM);
        doneBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().closeWindow(this);
            return false;
        });
        addComponent(doneBtn);
        
        CreateButton resetBtn = new CreateButton(7, height() - 6 - CreateButton.HEIGHT, AllIcons.I_TRASH);
        resetBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {            
            IWireItemBase.clear(stack);
            DataAccessor.getFromServer(null, ModNetworkAccessor.CLEAR_WIRE_CONNECTION_DATA, $ -> {});
            getWindowManager().closeWindow(this);
            return false;
        });
        addComponent(resetBtn);
    }
            
    @Override
    public void renderMainLayer(Graphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        GuiUtils.drawTexture(TEXTURE, graphics, 0, 0, GUI_WIDTH, GUI_HEIGHT, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        GuiUtils.drawString(graphics, Minecraft.getInstance().font, width() / 2, 4, title, DragonLib.NATIVE_UI_FONT_COLOR, EAlignment.CENTER, false);
        GuiUtils.drawString(graphics, Minecraft.getInstance().font, width() / 2, 26, txtInstruction, 0xFF7A7A7A, EAlignment.CENTER, false);
    }
    
}
