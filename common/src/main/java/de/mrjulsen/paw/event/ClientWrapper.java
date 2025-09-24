package de.mrjulsen.paw.event;

import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.internal.DLScreenWrapper;
import de.mrjulsen.paw.client.gui.screens.CantileverSettingsScreen;
import de.mrjulsen.paw.client.gui.screens.CantileverSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.world.item.ItemStack;

public class ClientWrapper {

    public static void showCantileverSettingsScreen(ItemStack stack) {
        //DLScreen.setScreen(new CantileverSettingsScreen(stack));
        Minecraft.getInstance().setScreen(new DLScreenWrapper(root -> new CantileverSettingsScreen(root, stack)));
    }

    public static void setSectionDirty(SectionPos pos) {
        Minecraft.getInstance().execute(() -> {            
            Minecraft.getInstance().levelRenderer.setSectionDirty(pos.getX(), pos.getY(), pos.getZ());
        });
    }
    
}
