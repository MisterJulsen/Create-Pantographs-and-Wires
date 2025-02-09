package de.mrjulsen.wires.block;

public interface IBlockEntityExtension {
    default void onChunkUnloaded() {}
}
