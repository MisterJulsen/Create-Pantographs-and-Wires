package de.mrjulsen.wires.decoration;

import java.util.Optional;

import org.joml.Vector3f;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class WireDecorationElement<T extends WireDecorationElement<T>> {

    private final ResourceLocation id;

    public WireDecorationElement(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation getId() {
        return id;
    }

    public void writeNbt(CompoundTag nbt) {}
    public void readNbt(CompoundTag nbt) {}
    
    public void onPlace(Level level, Player player) {}
    public void onBreak(Level level, Vector3f position, Optional<Player> player) {}

    public abstract float getRadius();
    public abstract WireDecorationRenderer<T> getRenderer();
}
