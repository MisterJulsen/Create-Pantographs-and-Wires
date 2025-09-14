package de.mrjulsen.wires.item;

import de.mrjulsen.paw.data.WireHitResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface IWireInteractableItem {
    InteractionResult interactWithWire(Level level, Player player, InteractionHand hand, WireHitResult hit);
}
