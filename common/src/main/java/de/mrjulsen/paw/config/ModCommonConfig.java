package de.mrjulsen.paw.config;

import de.mrjulsen.paw.PantographsAndWires;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ModCommonConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    
    public static final ModConfigSpec.ConfigValue<Boolean> ADVANCED_BLOCK_SELECTION;
    public static final ModConfigSpec.ConfigValue<Boolean> USE_DATA_FIXERS;
    public static final ModConfigSpec.ConfigValue<Boolean> CONVERT_WIRE_NETWORK;
    public static final ModConfigSpec.ConfigValue<Boolean> COMPRESS_HASH_VALUES;
    public static final ModConfigSpec.ConfigValue<Boolean> ADVANCED_LOGGING;
    public static final ModConfigSpec.ConfigValue<Boolean> WIRE_CONVERTER_LOGGING;

    public static final double MIN_SCALE = 0.25f;
    public static final double MAX_SCALE = 2.0f;

    static {
        BUILDER.push(PantographsAndWires.MOD_ID + "_common_config");
        
        ADVANCED_BLOCK_SELECTION = BUILDER.comment(new String[] {"Improves targeting of blocks when the hitbox is outside the actual block area (e.g. large or diagonal hitboxes). However, this requires a bit more computing power because 9 blocks have to be checked instead of 1.", "Default: ON"})
            .define("advanced.block_selection", true);

        USE_DATA_FIXERS = BUILDER.comment(new String[] {"If enabled, world data from old versions of this mod are converted into the new format, so that blocks or their settings are preserved as far as possible. However, this feature can cause small lag spikes when converting chunks once. If the feature is deactivated, old blocks are lost.", "Default: ON"})
            .define("advanced.use_data_fixers", true);
            
        CONVERT_WIRE_NETWORK = BUILDER.comment(new String[] {"If enabled, wire network data from old versions of this mod is converted into the new format, so that existing wires are preserved as far as possible. If the feature is deactivated, old wire networks are lost.", "Default: ON"})
            .define("advanced.convert_wire_network", true);

        COMPRESS_HASH_VALUES = BUILDER.comment(new String[] { "Compresses all calculated unique hash values to save memory. Compression slows down the calculation of hash values, but uses less memory. These differences are very minimal and only useful for extremely large wire networks or very limited hardware.", "Default: OFF"})
            .define("advanced.compress_hash_values", false);

        ADVANCED_LOGGING = BUILDER.comment(new String[] { "Prints advanced debug information in the console. This option can be disabled for normal gameplay, but it might be useful for troubleshooting.", "Default: OFF"})
            .define("debug.advanced_logging", false);
        WIRE_CONVERTER_LOGGING = BUILDER.comment(new String[] { "Prints detailed information abount converting and updating the wire graph in the console. This option can be disabled for normal gameplay, but it might be useful for troubleshooting problems with converting wire data from older versions.", "Default: OFF"})
            .define("debug.wire_converter_logging", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
