package de.mrjulsen.paw;

import com.mojang.logging.LogUtils;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import de.mrjulsen.paw.event.ModClientEvents;
import de.mrjulsen.paw.event.ModCommonEvents;
import de.mrjulsen.paw.network.ModNetworkManager;
import de.mrjulsen.paw.registry.*;
import de.mrjulsen.wires.WiresApi;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import org.slf4j.Logger;

public final class PantographsAndWires {

    public static final String MOD_ID = "pantographsandwires";
    public static final String SHORT_MOD_ID = "paw";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String NBT_DATA_FIXER = MOD_ID + "_datafixer_version";
    public static final int DATA_FIXER_VERSION = 2;
    
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    static {
		REGISTRATE.setTooltipModifierFactory(item -> {
            return new ItemDescription.Modifier(item, Palette.STANDARD_CREATE)
                    .andThen(TooltipModifier.mapNull(KineticStats.create(item)));
        }).defaultCreativeTab((ResourceKey<CreativeModeTab>) null);
	}

    public static void load() {}

    public static void init() {
        WiresApi.init();

        ModWireRegistry.init();
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModCreativeModeTab.setup();
        ModNetworkManager.init();
        ModRecipes.init();
        ModDataComponents.init();

        CrossPlatform.registerConfig();
        ModCommonEvents.init();
        if (Platform.getEnvironment() == Env.CLIENT) {
            ModClientEvents.init();
        }
    }

    public static boolean useAdvancedLogging() {
        return Platform.isDevelopmentEnvironment();
    }
}
