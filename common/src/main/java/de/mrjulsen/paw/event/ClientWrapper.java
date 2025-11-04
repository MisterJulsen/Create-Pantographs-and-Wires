package de.mrjulsen.paw.event;

import org.lwjgl.glfw.GLFW;

import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.util.Holder.MutableHolder;
import de.mrjulsen.paw.client.gui.screens.CantileverSettingsScreen;
import de.mrjulsen.paw.client.gui.screens.WireTypeSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.world.item.ItemStack;

public class ClientWrapper {

    public static void showCantileverSettingsScreen(ItemStack stack) {
        MutableHolder<Runnable> closeAction = new MutableHolder<>(() -> {});
        DLWindow.openWindow(root -> {
            root.addEventListener(DLGuiStandardEvents.KeyPressEvent.class, (s, e) -> {
                if (e.keyCode() == GLFW.GLFW_KEY_ESCAPE || Minecraft.getInstance().options.keyInventory.matches(e.keyCode(), e.scanCode())) {
                    closeAction.get().run();
                    return true;
                }
                return false;
            });
            closeAction.set(root::close);
            return new CantileverSettingsScreen(root, stack);
        });
    }

    public static void showWireTypeSelectionScreen(ItemStack stack) {
        MutableHolder<Runnable> closeAction = new MutableHolder<>(() -> {});
        DLWindow.openWindow(root -> {
            root.addEventListener(DLGuiStandardEvents.KeyPressEvent.class, (s, e) -> {
                if (e.keyCode() == GLFW.GLFW_KEY_ESCAPE || Minecraft.getInstance().options.keyInventory.matches(e.keyCode(), e.scanCode())) {
                    closeAction.get().run();
                    return true;
                }
                return false;
            });
            closeAction.set(root::close);
            return new WireTypeSelectionScreen(root, stack);
        });
    }

    public static void setSectionDirty(SectionPos pos) {
        Minecraft.getInstance().execute(() -> {            
            Minecraft.getInstance().levelRenderer.setSectionDirty(pos.getX(), pos.getY(), pos.getZ());
        });
    }
    
}
