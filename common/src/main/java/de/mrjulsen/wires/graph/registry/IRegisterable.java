package de.mrjulsen.wires.graph.registry;

import de.mrjulsen.mcdragonlib.data.INBTSerializable;

public interface IRegisterable<T extends INBTSerializable> extends INBTSerializable {
    DLRegistryObject<T> getRegistryType();
}
