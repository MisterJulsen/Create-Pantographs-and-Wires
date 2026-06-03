package de.mrjulsen.paw.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.mrjulsen.mcdragonlib.client.util.DLGraphics;
import de.mrjulsen.mcdragonlib.client.util.RenderUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import de.mrjulsen.paw.PantographsAndWires;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;
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

    public static void renderWaxedItem(DLGraphics graphics, ItemStack itemStack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model) {
        if (context != ItemDisplayContext.GUI) {
            return;
        }

        DLGraphics g = new DLGraphics(new PoseStack(), buffer, combinedLight, combinedOverlay, graphics.partialTick());
        RenderUtils.renderTexture(DLUtils.resourceLocation("minecraft", "textures/item/honeycomb.png"), g, new Vector3f(0, 8, 0), 8, 8, 0, 0, 1, 1, Direction.NORTH, DLColor.WHITE, LightTexture.FULL_BRIGHT, false);

    }

    public static void renderWaxedItem(DLGraphics graphics, ItemStack itemStack, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        RenderUtils.renderTexture(DLUtils.resourceLocation("minecraft", "textures/item/honeycomb.png"), graphics, new Vector3f(0, 8, 0), 8, 8, 0, 0, 1, 1, Direction.NORTH, DLColor.WHITE, LightTexture.FULL_BRIGHT, false);

    }
    
}
