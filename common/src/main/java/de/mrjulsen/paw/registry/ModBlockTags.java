package de.mrjulsen.paw.registry;

import com.simibubi.create.AllBlocks;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.datagen.DataGen;
import de.mrjulsen.paw.datagen.ITagAppender;
import de.mrjulsen.paw.datagen.TagEntry;
import dev.architectury.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public final class ModBlockTags {
    private ModBlockTags() {}

    private static final List<TagEntry<Block>> REGISTRY = new LinkedList<>();

    private static TagKey<Block> tag(String name) {
        return tag(name, $ -> {});
    }

    private static TagKey<Block> tag(String name, Consumer<ITagAppender<Block>> populator) {
        TagKey<Block> key = TagKey.create(Registries.BLOCK, DLUtils.resourceLocation(PantographsAndWires.MOD_ID, name));
        REGISTRY.add(new TagEntry<>(key, populator));
        return key;
    }

    public static final TagKey<Block> FLAT_LATTICE_MASTS = tag("flat_lattice_masts");
    public static final TagKey<Block> FLAT_DIAGONAL_LATTICE_MASTS = tag("flat_diagonal_lattice_masts");
    public static final TagKey<Block> LATTICE_MASTS = tag("lattice_masts");
    public static final TagKey<Block> H_BEAM_MASTS = tag("h_beam_masts");
    public static final TagKey<Block> CONCRETE_POSTS = tag("concrete_posts");
    public static final TagKey<Block> CONCRETE_PILLARS = tag("concrete_pillars");

    public static final TagKey<Block> RAW_MASTS = tag("raw_masts");
    public static final TagKey<Block> EXPOSED_MASTS = tag("exposed_masts");
    public static final TagKey<Block> WEATHERED_MASTS = tag("weathered_masts");
    public static final TagKey<Block> OXIDIZED_MASTS = tag("oxidized_masts");
    public static final TagKey<Block> GALVANIZED_MASTS = tag("galvanized_masts");
    public static final TagKey<Block> WAXED_MASTS = tag("waxed_masts");
    public static final TagKey<Block> UNWAXED_MASTS = tag("unwaxed_masts");

    public static final TagKey<Block> MASTS = tag("masts", p -> p
            .add(FLAT_LATTICE_MASTS)
            .add(FLAT_DIAGONAL_LATTICE_MASTS)
            .add(LATTICE_MASTS)
            .add(H_BEAM_MASTS)
            .add(CONCRETE_POSTS)
            .add(CONCRETE_PILLARS)
    );

    public static final TagKey<Block> CANTILEVER_BRACKETS = tag("cantilever_brackets");
    public static final TagKey<Block> POWER_LINE_BRACKETS = tag("power_line_brackets");

    public static final TagKey<Block> V_SHAPED_INSULATORS = tag("v_shaped_insulators");
    public static final TagKey<Block> U_SHAPED_INSULATORS = tag("u_shaped_insulators");
    public static final TagKey<Block> I_SHAPED_INSULATORS = tag("i_shaped_insulators");
    public static final TagKey<Block> BROWN_INSULATOR = tag("brown_insulators");
    public static final TagKey<Block> GREEN_INSULATOR = tag("green_insulators");
    public static final TagKey<Block> INSULATORS = tag("insulators", p -> p
            .add(V_SHAPED_INSULATORS)
            .add(U_SHAPED_INSULATORS)
            .add(I_SHAPED_INSULATORS)
            .add(BROWN_INSULATOR)
            .add(GREEN_INSULATOR)
    );

    public static final TagKey<Block> TENSIONING_DEVICE_CONNECTABLE = tag("tensioning_device_connectable", p -> p
            .add(BlockTags.WALLS)
            .add(AllBlocks.METAL_GIRDER.get())
    );
    public static final TagKey<Block> CANTILEVER_CONNECTABLE_16PX = tag("cantilever_connectable_16px");
    public static final TagKey<Block> CANTILEVER_CONNECTABLE_12PX = tag("cantilever_connectable_12px");
    public static final TagKey<Block> CANTILEVER_CONNECTABLE_8PX = tag("cantilever_connectable_8px", p -> p
            .add(BlockTags.WALLS)
            .add(AllBlocks.METAL_GIRDER.get())
    );
    public static final TagKey<Block> CANTILEVER_CONNECTABLE_6PX = tag("cantilever_connectable_6px");
    public static final TagKey<Block> CANTILEVER_CONNECTABLE_5PX = tag("cantilever_connectable_5px");
    public static final TagKey<Block> CANTILEVER_CONNECTABLE_4PX = tag("cantilever_connectable_4px", p -> p
            .add(BlockTags.FENCES)
    );


    public static final TagKey<Block> CATENARY_HEADSPAN_CONNECTABLE = tag("catenary_headspan_connectable", p -> p
            .add(BlockTags.WALLS)
            .add(AllBlocks.METAL_GIRDER.get())
    );
    public static final TagKey<Block> SUPPORT_WIRE_CONNECTABLE = tag("support_wire_connectable", p -> p
            .add(BlockTags.WALLS)
            .add(BlockTags.FENCES)
            .add(AllBlocks.METAL_GIRDER.get())
    );
    public static final TagKey<Block> CANTILEVER_CONNECTABLE = tag("cantilever_connectable", p -> p
            .add(CANTILEVER_CONNECTABLE_16PX)
            .add(CANTILEVER_CONNECTABLE_12PX)
            .add(CANTILEVER_CONNECTABLE_8PX)
            .add(CANTILEVER_CONNECTABLE_6PX)
            .add(CANTILEVER_CONNECTABLE_5PX)
            .add(CANTILEVER_CONNECTABLE_4PX)
    );
    public static final TagKey<Block> CANTILEVER_MAST_BRACKET_FITTING = tag("cantilever_mast_bracket_fitting");
    public static final TagKey<Block> CANTILEVER_MAST_HINGE = tag("cantilever_mast_hinge", p -> p
            .add(BlockTags.FENCES)
            .add(BlockTags.WALLS)
            .add(AllBlocks.METAL_GIRDER.get())
    );


    public static void register(RegistrateTagsProvider<Block> provider) {
        if (Platform.isFabric()) {
            DataGen.registerTags(provider, REGISTRY);
        }
    }
}
