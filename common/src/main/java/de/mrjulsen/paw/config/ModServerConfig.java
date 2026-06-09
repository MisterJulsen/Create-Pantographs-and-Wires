package de.mrjulsen.paw.config;

import de.mrjulsen.paw.PantographsAndWires;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ModServerConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    private static final String WARN = "If in doubt, leave unchanged.";
    
    public static final ModConfigSpec.ConfigValue<Integer> CATENARY_WIRE_MAX_LENGTH;
    public static final ModConfigSpec.ConfigValue<Integer> ENERGY_WIRE_MAX_LENGTH;
    public static final ModConfigSpec.ConfigValue<Integer> CATENARY_HEADSPAN_MAX_LENGTH;
    public static final ModConfigSpec.ConfigValue<Integer> SUPPORT_WIRE_MAX_LENGTH;
    public static final ModConfigSpec.ConfigValue<Double> WIRE_COLLISION_TRACER_STEP_SIZE;
    public static final ModConfigSpec.ConfigValue<Boolean> BLOCKS_BREAK_WIRES;
    public static final ModConfigSpec.ConfigValue<Boolean> WIRE_ENTITY_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Boolean> USE_OXIDATION;
    public static final ModConfigSpec.ConfigValue<Boolean> DROP_WIRE_ITEMS_IN_CREATIVE;
    public static final ModConfigSpec.ConfigValue<Integer> CATENARY_HEADSPAN_MIN_UPPER_TENSION_WIRE;
    public static final ModConfigSpec.ConfigValue<Integer> CATENARY_HEADSPAN_MAX_UPPER_TENSION_WIRE;
    public static final ModConfigSpec.ConfigValue<Integer> CATENARY_HEADSPAN_MAX_TOP_SUPPORT_WIRE;

    static {
        BUILDER.push(PantographsAndWires.MOD_ID + "_common_config");

        CATENARY_WIRE_MAX_LENGTH = BUILDER.comment("[in Blocks]", "The maximum length of the catenary wire between two masts.", "Default: 50")
            .defineInRange("wires.catenary_max_length", 50, 16, 128);
        ENERGY_WIRE_MAX_LENGTH = BUILDER.comment("[in Blocks]", "The maximum length of the energy wire between two masts.", "Default: 64")
            .defineInRange("wires.energy_max_length", 64, 16, 128);
        CATENARY_HEADSPAN_MAX_LENGTH = BUILDER.comment("[in Blocks]", "The maximum length of the catenary headspan wire between two masts.", "Default: 50")
            .defineInRange("wires.catenary_headspan_length", 50, 16, 128);
        SUPPORT_WIRE_MAX_LENGTH = BUILDER.comment("[in Blocks]", "The maximum length of the support wire between two masts.", "Default: 64")
            .defineInRange("wires.support_length", 64, 16, 128);

        CATENARY_HEADSPAN_MIN_UPPER_TENSION_WIRE = BUILDER.comment("[in Blocks]", "The minimum distance between the two tension wires. The minimum distance cannot be larger than the maximum distance. If it is entered incorrectly, the minimum distance is 'max - 1'.", "Default: 1")
            .defineInRange("wires.catenary_headspan.upper_tension_wire_min_height", 1, 1, 7);
        CATENARY_HEADSPAN_MAX_UPPER_TENSION_WIRE = BUILDER.comment("[in Blocks]", "The maximum distance between the two tension wires. The maximum distance cannot be smaller than the minimum distance. If it is entered incorrectly, the maximum distance is 'min + 1'.", "Default: 4")
            .defineInRange("wires.catenary_headspan.upper_tension_wire_max_height", 4, 2, 8);   

        CATENARY_HEADSPAN_MAX_TOP_SUPPORT_WIRE = BUILDER.comment("[in Blocks]", "The additional maximum distance between the top support wire and the upper tension wire. This value is added to the automatically calculated value (= minimum) as the max difference. The minimum is calculated from the distance between the two masts.", "Default: 4")
            .defineInRange("wires.catenary_headspan.top_support_wire_max_height_difference", 4, 0, 8);

        BLOCKS_BREAK_WIRES = BUILDER.comment("Whether blocks placed in wires can destroy them.", "Default: true")
            .define("wires.block_destroy_wires", true);
        WIRE_ENTITY_DAMAGE = BUILDER.comment("Whether powered wires should cause damage to entities touching them.", "Default: true")
            .define("wires.wire_entity_damage", true);

        WIRE_COLLISION_TRACER_STEP_SIZE = BUILDER.comment("[in Block Pixels]", "The step size used in the collision calculation of the wires. Lower values increase precision but require more computing power. Higher values are more inaccurate but require less performance.", WARN, "Default: 1")
            .defineInRange("wires.calculation.collision_tracer_step_size", 1, 0.1, 4);

        USE_OXIDATION = BUILDER.comment("When activated, metal blocks oxidize and concrete blocks erode slowly, similar to copper.", "Default: ON")
            .define("gameplay.oxidation", true);
        DROP_WIRE_ITEMS_IN_CREATIVE = BUILDER.comment("When enabled, wires drop items if the player modifying the wire is in creative mode.", "Default: OFF")
            .define("gameplay.drop_wire_items_in_creative", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
    
}
