package de.mrjulsen.wires.graph.registry;

public interface IStaticRegisterable<T> {
    DLStaticRegistryObject<T> getRegistryType();
}
