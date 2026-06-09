package de.mrjulsen.wires.item;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.foundation.recipe.ItemCopyingRecipe;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.paw.components.WireAmountComponent;
import de.mrjulsen.paw.components.WireSubtypeComponent;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.paw.data.WireSettingsData;
import de.mrjulsen.paw.event.ClientWrapper;
import de.mrjulsen.paw.registry.ModDataComponents;
import de.mrjulsen.paw.registry.ModWireRegistry;
import de.mrjulsen.wires.IWireType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class MultiWireItem extends AbstractWireItemBase implements ItemCopyingRecipe.SupportsItemCopying {

    public static final String NBT_TYPE = "SelectedSubtype";

    public MultiWireItem(Properties properties) {
        super(properties
            .stacksTo(1)
        );
    }

    @Override
    public IWireType getWireType(ItemStack stack) {
        return getSubType(stack).getWireType(stack);
    }

    public IWireItemBase getSubType(ItemStack stack) {
        IPawWireItemBase fallback = ModWireRegistry.ENERGY_WIRE_ITEM_SUBTYPE.get();
        WireSubtypeComponent subtype = ModDataComponents.getComponent(stack, ModDataComponents.WIRE_SUBTYPE, WireSubtypeComponent::empty);
        return subtype.id().map(id -> ModWireRegistry.WIRE_SUBTYPES_REGISTRY.getById(id).orElse(fallback)).orElse(fallback);
    }
    
    @Override
    public InteractionResult interactWithWire(Level level, Player player, InteractionHand hand, WireHitResult hit) {
        return getSubType(player.getItemInHand(hand)).interactWithWire(level, player, hand, hit);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        getSubType(context.getItemInHand()).useWireOn(context);
        return InteractionResult.SUCCESS;
<<<<<<< HEAD
    }

    @Override
    public void renderHelperOutline(ItemStack stack, Player player, HitResult hit) {
        getSubType(player.getItemInHand(InteractionHand.MAIN_HAND)).renderHelperOutline(stack, player, hit);
=======
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
    }

    public static boolean setSettings(ItemStack stack, WireSettingsData data) {
        ModDataComponents.setComponent(stack, ModDataComponents.WIRE_SUBTYPE, new WireSubtypeComponent(Optional.ofNullable(data.selectedType().getRegistryType().id())));
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        IPawWireItemBase item = (IPawWireItemBase)getSubType(player.getItemInHand(usedHand));
        InteractionResultHolder<ItemStack> result = item.useWire(level, player, usedHand);
        if (result.getResult().consumesAction()) {
            return result;
        }
        if (!result.getResult().consumesAction() && player.isShiftKeyDown()) {
<<<<<<< HEAD
            IWireItemBase.clear(player, player.getItemInHand(usedHand));
=======
            clear(player.getItemInHand(usedHand));
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
            return InteractionResultHolder.consume(player.getItemInHand(usedHand));
        }
        if (level.isClientSide()) {
            ClientWrapper.showWireTypeSelectionScreen(player.getItemInHand(usedHand));
            return InteractionResultHolder.consume(player.getItemInHand(usedHand));
        }
        return result;
    }

    @Override
    public Component createHudInfoText(ItemStack stack, Player player, HitResult hit) {
        return getSubType(stack).createHudInfoText(stack, player, hit);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(TextUtils.text("Type: ").append(TextUtils.translate(((IPawWireItemBase)getSubType(stack)).getTranslationKey())));
        tooltipComponents.add(TextUtils.text("Amount: " + IPawWireItemBase.getRemainingWire(stack) + "m"));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (IPawWireItemBase.getRemainingWire(stack) * 13) / WireAmountComponent.MAX_WIRE;
    }

    @Override
    public int getBarColor(ItemStack pStack) {
        return 0xFFFF0000;
    }
    
}
