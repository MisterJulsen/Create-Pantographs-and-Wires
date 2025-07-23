package de.mrjulsen.paw.config;

import de.mrjulsen.paw.PantographsAndWires;
import net.minecraftforge.common.ForgeConfigSpec;

public class ModCommonConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    
    public static final ForgeConfigSpec.ConfigValue<Boolean> ADVANCED_BLOCK_SELECTION;
    public static final ForgeConfigSpec.ConfigValue<Boolean> USE_DATA_FIXERS;

    public static final double MIN_SCALE = 0.25f;
    public static final double MAX_SCALE = 2.0f;

    static {
        BUILDER.push(PantographsAndWires.MOD_ID + "_common_config");
        
        ADVANCED_BLOCK_SELECTION = BUILDER.comment(new String[] {"Improves targeting of blocks when the hitbox is outside the actual block area (e.g. large or diagonal hitboxes). However, this requires a bit more computing power because 9 blocks have to be checked instead of 1.", "Default: ON"})
            .define("advanced.block_selection", true);

        USE_DATA_FIXERS = BUILDER.comment(new String[] {"If enabled, world data from old versions of this mod are converted into the new format, so that blocks or their settings are preserved as far as possible. However, this feature can cause small lags when converting chunks once. If the feature is deactivated, old blocks are lost.", "Default: ON"})
            .define("advanced.use_data_fixers", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
