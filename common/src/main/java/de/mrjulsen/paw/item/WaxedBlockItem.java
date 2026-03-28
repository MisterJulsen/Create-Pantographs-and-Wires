package de.mrjulsen.paw.item;

import com.mojang.blaze3d.vertex.PoseStack;
import de.mrjulsen.mcdragonlib.client.render.ICustomItemRenderer;
import de.mrjulsen.mcdragonlib.client.util.DLGraphics;
import de.mrjulsen.paw.event.ClientWrapper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class WaxedBlockItem extends BlockItem implements ICustomItemRenderer {

    public WaxedBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void renderAdditional(DLGraphics graphics, ItemStack itemStack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model) {
        //ClientWrapper.renderWaxedItem(graphics, itemStack, context, leftHand, poseStack, buffer, combinedLight, combinedOverlay, model);
    }
}
