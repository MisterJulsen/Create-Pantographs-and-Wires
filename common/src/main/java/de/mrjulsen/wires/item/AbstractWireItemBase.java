package de.mrjulsen.wires.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public abstract class AbstractWireItemBase extends Item implements IWireItemBase {

    public AbstractWireItemBase(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        return useWire(level, player, usedHand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return useWireOn(context);
    }
    
}
