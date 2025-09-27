package de.mrjulsen.wires.item;

import de.mrjulsen.paw.event.ClientWrapper;
import de.mrjulsen.paw.registry.ModWireRegistry;
import de.mrjulsen.paw.registry.ModNetworkAccessor.WireSettingsData;
import de.mrjulsen.wires.IWireType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MultiWireItem extends AbstractWireItemBase {

    public static final String NBT_TYPE = "SelectedSubtype";

    public MultiWireItem(Properties properties) {
        super(properties);
    }

    @Override
    public IWireType getWireType(ItemStack stack) {
        return getActor(stack).getWireType(stack);
    }

    @Override
    public IWireItemBase getActor(ItemStack stack) {
        IPawWireItemBase type = ModWireRegistry.WIRE_SUBTYPES_REGISTRY.load(stack.getOrCreateTag().getCompound(NBT_TYPE));
        return type == null ? ModWireRegistry.ENERGY_WIRE_ITEM_SUBTYPE.get() : type;
    }

    public static boolean setNbt(ItemStack stack, WireSettingsData data) {
        if (stack.getItem() instanceof MultiWireItem) {
            data.toNbt(stack.getOrCreateTag());
            return true;
        } 
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (level.isClientSide && player.isShiftKeyDown()) {
            ClientWrapper.showWireTypeSelectionScreen(player.getItemInHand(usedHand));
            return InteractionResultHolder.consume(player.getItemInHand(usedHand));
        }
        IPawWireItemBase item = (IPawWireItemBase)getActor(player.getItemInHand(usedHand));
        return item.useWire(level, player, usedHand);
    }
}
