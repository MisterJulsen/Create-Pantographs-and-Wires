package de.mrjulsen.paw.config;

import de.mrjulsen.paw.PantographsAndWires;
import net.minecraftforge.common.ForgeConfigSpec;

public class ModServerConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    private static final String WARN = "If in doubt, leave unchanged.";
    
    public static final ForgeConfigSpec.ConfigValue<Integer> CATENARY_WIRE_MAX_LENGTH;
    public static final ForgeConfigSpec.ConfigValue<Integer> ENERGY_WIRE_MAX_LENGTH;
    public static final ForgeConfigSpec.ConfigValue<Double> WIRE_COLLISION_TRACER_STEP_SIZE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> BLOCKS_BREAK_WIRES;
    public static final ForgeConfigSpec.ConfigValue<Boolean> WIRE_ENTITY_DAMAGE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> USE_OXIDATION;

    static {
        BUILDER.push(PantographsAndWires.MOD_ID + "_common_config");

        CATENARY_WIRE_MAX_LENGTH = BUILDER.comment(new String[] {"[in Blocks]", "The maximum length of the catenary wire between two masts.", "Default: 48"})
            .defineInRange("wires.catenary_max_length", 50, 16, 64);
        ENERGY_WIRE_MAX_LENGTH = BUILDER.comment(new String[] {"[in Blocks]", "The maximum length of the energy wire between two masts.", "Default: 48"})
            .defineInRange("wires.energy_max_length", 50, 16, 64);
        BLOCKS_BREAK_WIRES = BUILDER.comment(new String[] {"Whether blocks placed in wires can destroy them.", "Default: true"})
            .define("wires.block_destroy_wires", true);
        WIRE_ENTITY_DAMAGE = BUILDER.comment(new String[] {"Whether powered wires should cause damage to entities touching them.", "Default: true"})
            .define("wires.wire_entity_damage", true);

        WIRE_COLLISION_TRACER_STEP_SIZE = BUILDER.comment(new String[] {"[in Block Pixels]", "Which step size is used in the collision calculation of the cables. Lower values increase precision but require more computing power. Higher values are inaccurate but require less more performance.", WARN, "Default: 1"})
            .defineInRange("wires.calculation.collision_tracer_step_size", 1, 0.1, 4);

        USE_OXIDATION = BUILDER.comment(new String[] { "When activated, metal blocks oxidize and concrete blocks erode slowly, similar to copper.", "Default: ON" })
            .define("gameplay.oxidation", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
    
}
