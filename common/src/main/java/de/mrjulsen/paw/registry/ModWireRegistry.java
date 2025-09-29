package de.mrjulsen.paw.registry;


import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.item.CatenaryWireType;
import de.mrjulsen.paw.item.DecorationWireItem;
import de.mrjulsen.paw.item.DecorationWireType;
import de.mrjulsen.paw.item.FeederWireItem;
import de.mrjulsen.paw.item.CatenaryHeadspanWireItem;
import de.mrjulsen.paw.item.CatenaryHeadspanWireType;
import de.mrjulsen.paw.item.CatenaryWireItem;
import de.mrjulsen.paw.item.PowerWireType;
import de.mrjulsen.wires.WireTypeRegistry;
import de.mrjulsen.wires.decoration.IWireDecoration;
import de.mrjulsen.wires.graph.registry.DLRegistry;
import de.mrjulsen.wires.graph.registry.DLRegistryObject;
import de.mrjulsen.wires.graph.registry.DLStaticRegistry;
import de.mrjulsen.wires.graph.registry.DLStaticRegistryObject;
import de.mrjulsen.wires.item.IPawWireItemBase;
import net.minecraft.resources.ResourceLocation;

public class ModWireRegistry {
    
    public static final CatenaryWireType CATENARY_WIRE = WireTypeRegistry.register(PantographsAndWires.MOD_ID, "catenary_wire", CatenaryWireType::new);
    public static final PowerWireType ENERGY_WIRE = WireTypeRegistry.register(PantographsAndWires.MOD_ID, "energy_wire", PowerWireType::new);
    public static final CatenaryHeadspanWireType CATENARY_HEADSPAN = WireTypeRegistry.register(PantographsAndWires.MOD_ID, "catenary_headspan", CatenaryHeadspanWireType::new);
    public static final DecorationWireType DECORATION_WIRE = WireTypeRegistry.register(PantographsAndWires.MOD_ID, "decoration_wire", DecorationWireType::new);

    public static final DLRegistry<IWireDecoration<?>> DECORATION_REGISTRY = new DLRegistry<>();
    public static final DLRegistryObject<InsulatorWireDecoration> INSULATOR_DECORATION = DECORATION_REGISTRY.register(new ResourceLocation(PantographsAndWires.MOD_ID, "insulator_decoration"), InsulatorWireDecoration::new);
    public static final DLRegistryObject<RegistrationArmWireDecoration> CATENARY_HEADSPAN_REGISTRATION_ARM = DECORATION_REGISTRY.register(new ResourceLocation(PantographsAndWires.MOD_ID, "catenary_headspan_registration_arm"), RegistrationArmWireDecoration::new);
    
    public static final DLStaticRegistry<IPawWireItemBase> WIRE_SUBTYPES_REGISTRY = new DLStaticRegistry<>();
    public static final DLStaticRegistryObject<FeederWireItem> ENERGY_WIRE_ITEM_SUBTYPE = WIRE_SUBTYPES_REGISTRY.register(new ResourceLocation(PantographsAndWires.MOD_ID, "energy_wire"), FeederWireItem::new);
    public static final DLStaticRegistryObject<CatenaryWireItem> CATENARY_WIRE_ITEM_SUBTYPE = WIRE_SUBTYPES_REGISTRY.register(new ResourceLocation(PantographsAndWires.MOD_ID, "catenary_wire"), CatenaryWireItem::new);
    public static final DLStaticRegistryObject<CatenaryHeadspanWireItem> CATENARY_HEADSPAN_ITEM_SUBTYPE = WIRE_SUBTYPES_REGISTRY.register(new ResourceLocation(PantographsAndWires.MOD_ID, "catenary_headspan"), CatenaryHeadspanWireItem::new);
    public static final DLStaticRegistryObject<DecorationWireItem> DECORATION_WIRE_ITEM_SUBTYPE = WIRE_SUBTYPES_REGISTRY.register(new ResourceLocation(PantographsAndWires.MOD_ID, "decoration_wire"), DecorationWireItem::new);

    public static void init() {}
}
