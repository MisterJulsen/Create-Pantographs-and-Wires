package de.mrjulsen.paw.event;

import org.lwjgl.glfw.GLFW;

import de.mrjulsen.mcdragonlib.data.Holder.MutableHolder;
import de.mrjulsen.mcdragonlib.internal.DLScreenWrapper;
import de.mrjulsen.paw.client.gui.screens.CantileverSettingsScreen;
import de.mrjulsen.paw.client.gui.screens.WireTypeSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.world.item.ItemStack;

public class ClientWrapper {

    public static void showCantileverSettingsScreen(ItemStack stack) {
        MutableHolder<Runnable> closeAction = new MutableHolder<>(() -> {});
        Minecraft.getInstance().setScreen(new DLScreenWrapper(root -> {
            closeAction.set(root::close);
            return new CantileverSettingsScreen(root, stack);
        }) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE || Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
                    closeAction.get().run();
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        });
    }

    public static void showWireTypeSelectionScreen(ItemStack stack) {
        MutableHolder<Runnable> closeAction = new MutableHolder<>(() -> {});
        Minecraft.getInstance().setScreen(new DLScreenWrapper(root -> {
            closeAction.set(root::close);
            return new WireTypeSelectionScreen(root, stack);
        }) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE || Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
                    closeAction.get().run();
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        });
    }

    public static void setSectionDirty(SectionPos pos) {
        Minecraft.getInstance().execute(() -> {            
            Minecraft.getInstance().levelRenderer.setSectionDirty(pos.getX(), pos.getY(), pos.getZ());
        });
    }
    
}
