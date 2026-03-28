package de.mrjulsen.paw.registry;

import de.mrjulsen.paw.PantographsAndWires;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    private ModTags() {}

    private static TagKey<Block> createBlockTag(String name) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation(PantographsAndWires.MOD_ID, name));
    }

    public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE = createBlockTag("cantilever_connectable");
    public static final TagKey<Block> TAG_CANTILEVER_MAST_BRACKET_FITTING = createBlockTag("cantilever_mast_bracket_fitting");
    public static final TagKey<Block> TAG_CANTILEVER_MAST_HINGE = createBlockTag("cantilever_mast_hinge");
    public static final TagKey<Block> TAG_TENSIONING_DEVICE_CONNECTABLE = createBlockTag("tensioning_device_connectable");

    public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE_16PX = createBlockTag("cantilever_connectable_16px");
    public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE_12PX = createBlockTag("cantilever_connectable_12px");
    public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE_8PX = createBlockTag("cantilever_connectable_8px");
    public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE_5PX = createBlockTag("cantilever_connectable_5px");
    public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE_4PX = createBlockTag("cantilever_connectable_4px");

    public static final TagKey<Block> TAG_RAW_MASTS = createBlockTag("raw_masts");
    public static final TagKey<Block> TAG_EXPOSED_MASTS = createBlockTag("exposed_masts");
    public static final TagKey<Block> TAG_WEATHERED_MASTS = createBlockTag("weathered_masts");
    public static final TagKey<Block> TAG_OXIDIZED_MASTS = createBlockTag("oxidized_masts");
    public static final TagKey<Block> TAG_GALVANIZED_MASTS = createBlockTag("galvanized_masts");

    public static final TagKey<Block> TAG_WAXED_MASTS = createBlockTag("waxed_masts");
    public static final TagKey<Block> TAG_UNWAXED_MASTS = createBlockTag("unwaxed_masts");

    public static final TagKey<Block> TAG_FLAT_LATTICE_MASTS = createBlockTag("flat_lattice_masts");
    public static final TagKey<Block> TAG_FLAT_DIAGONAL_LATTICE_MASTS = createBlockTag("flat_diagonal_lattice_masts");
    public static final TagKey<Block> TAG_LATTICE_MAST = createBlockTag("lattice_masts");
    public static final TagKey<Block> TAG_H_BEAM_MASTS = createBlockTag("h_beam_masts");
    public static final TagKey<Block> TAG_CONCRETE_MASTS = createBlockTag("concrete_masts");
    public static final TagKey<Block> TAG_CONCRETE_PILLARS = createBlockTag("concrete_pillars");

    public static final TagKey<Block> TAG_CATENARY_HEADSPAN_CONNECTABLE = createBlockTag("catenary_headspan_connectable");
    public static final TagKey<Block> TAG_SUPPORT_WIRE_CONNECTABLE = createBlockTag("support_wire_connectable");

}
