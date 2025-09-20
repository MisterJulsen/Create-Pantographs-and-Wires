package de.mrjulsen.paw.registry;

import java.util.function.Supplier;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.item.CatenaryWireType;
import de.mrjulsen.paw.item.CatenaryHeadspanWireType;
import de.mrjulsen.paw.item.PowerWireType;
import de.mrjulsen.wires.WireTypeRegistry;
import de.mrjulsen.wires.decoration.IWireDecoration;
import de.mrjulsen.wires.decoration.WireDecorationRegistry;
import de.mrjulsen.wires.graph.registry.DLRegistry;
import de.mrjulsen.wires.graph.registry.DLRegistryObject;
import net.minecraft.resources.ResourceLocation;

public class ModWireRegistry {
    
    public static final CatenaryWireType CATENARY_WIRE = WireTypeRegistry.register(PantographsAndWires.MOD_ID, "catenary_wire", CatenaryWireType::new);
    public static final PowerWireType ENERGY_WIRE = WireTypeRegistry.register(PantographsAndWires.MOD_ID, "energy_wire", PowerWireType::new);
    public static final CatenaryHeadspanWireType CATENARY_HEADSPAN = WireTypeRegistry.register(PantographsAndWires.MOD_ID, "catenary_headspan", CatenaryHeadspanWireType::new);

    public static final DLRegistry<IWireDecoration<?>> DECORATION_REGISTRY = new DLRegistry<>();
    public static final DLRegistryObject<InsulatorWireDecoration> BROWN_INSULATOR_DECORATION = DECORATION_REGISTRY.register(new ResourceLocation(PantographsAndWires.MOD_ID, "insulator_decoration"), InsulatorWireDecoration::new);
    public static final DLRegistryObject<RegistrationArmWireDecoration> CATENARY_HEADSPAN_REGISTRATION_ARM = DECORATION_REGISTRY.register(new ResourceLocation(PantographsAndWires.MOD_ID, "catenary_headspan_registration_arm"), RegistrationArmWireDecoration::new);

    public static void init() {}
}
