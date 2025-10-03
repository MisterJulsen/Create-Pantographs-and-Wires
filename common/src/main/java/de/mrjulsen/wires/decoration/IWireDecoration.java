package de.mrjulsen.wires.decoration;

import java.util.Optional;

import org.joml.Vector3f;

import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.wires.graph.registry.IRegisterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface IWireDecoration<T extends IWireDecoration<T>> extends IRegisterable<IWireDecoration<?>> {
    
    default void onPlace(Level level, Player player) {}

    default void onBreak(Level level, Vector3f position, Optional<Player> player) {}

    default InteractionResult use(Level level, Player player, InteractionHand hand, WireHitResult hit) {
        return InteractionResult.PASS;
    }

    
    @Deprecated
    float getRadius(IWireDecoration<?> element); // TODO temp workaround, real isOccupied method needed!
    WireDecorationRenderer<T> getRenderer();
}
