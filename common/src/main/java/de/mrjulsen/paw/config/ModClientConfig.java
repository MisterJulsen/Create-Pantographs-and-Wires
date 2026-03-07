package de.mrjulsen.paw.config;

import de.mrjulsen.paw.PantographsAndWires;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ModClientConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<Boolean> DEBUG_ORIGINAL_HITBOX;

    static {
        BUILDER.push(PantographsAndWires.MOD_ID + "_client_config");

        DEBUG_ORIGINAL_HITBOX = BUILDER.comment(new String[] {"Shows the real AABB hitbox of rotated blocks and not a rotated version of the outline.", "Default: OFF"})
            .define("debug.show_original_block_hitbox", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
    
}
