package de.mrjulsen.paw.client.gui.screens;

import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLSlider;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLTexture;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.client.gui.widgets.CreateButton;
import de.mrjulsen.paw.client.gui.widgets.CreateListSlider;
import de.mrjulsen.paw.components.WireSubtypeComponent;
import de.mrjulsen.paw.data.WireSettingsData;
import de.mrjulsen.paw.network.ModNetworkManager;
import de.mrjulsen.paw.network.packets.UpdateWireSettingsPacketData;
<<<<<<< HEAD
=======
import de.mrjulsen.paw.registry.ModDataComponents;
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
import de.mrjulsen.paw.registry.ModWireRegistry;
import de.mrjulsen.wires.item.IPawWireItemBase;
import de.mrjulsen.wires.item.MultiWireItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class WireTypeSelectionScreen extends DLWindow {
    
    private static final DLTexture TEXTURE = new DLTexture(DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "textures/gui/quick_settings.png"), 256, 256);
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
<<<<<<< HEAD
            MultiWireItem.setNbt(stack, data);   
        
=======
            ModDataComponents.setComponent(stack, ModDataComponents.WIRE_SUBTYPE, new WireSubtypeComponent(Optional.of(data.selectedType().getRegistryType().id())));
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
            ModNetworkManager.UPDATE_WIRE_SETTINGS.send(NetworkDirection.toServer(), new UpdateWireSettingsPacketData(data));
            return false;
        });
        
        int w = ModWireRegistry.WIRE_SUBTYPES_REGISTRY.getAll().size() * 20;
        CreateListSlider<IPawWireItemBase> typeSelection = new CreateListSlider<>(width() / 2 - w / 2, 43, w, 20, ModWireRegistry.WIRE_SUBTYPES_REGISTRY.getAll());
        typeSelection.text.set(txtWireType);
        typeSelection.setValue(selectedType);
        typeSelection.addEventListener(DLSlider.ValueChangedEvent.class, (s, e) -> {
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
    }
            
    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        GuiUtils.drawTexture(TEXTURE, graphics, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        GuiUtils.drawString(graphics, Minecraft.getInstance().font, width() / 2, 4, title, DragonLib.VANILLA_UI_FONT_COLOR, ETextAlignment.CENTER, false);
        GuiUtils.drawString(graphics, Minecraft.getInstance().font, width() / 2, 26, txtInstruction, DLColor.fromInt(0xFF7A7A7A), ETextAlignment.CENTER, false);
    }
    
}
