package de.mrjulsen.paw.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.mrjulsen.mcdragonlib.client.util.DLGraphics;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.event.ClientWrapper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

@Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "HEAD"))
    private void renderItemOil(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof IWeatheringBlock<?> weatheringBlock && weatheringBlock.getWeatheringData().isWaxed()) {
            GuiGraphics self = (GuiGraphics)(Object)this;
            PoseStack poseStack = self.pose();
            poseStack.pushPose();
            poseStack.translate(x, y, 200);
            DLGraphics graphics = new DLGraphics(self.pose(), self.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            ClientWrapper.renderWaxedItem(graphics, stack, self.pose(), self.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }
}