package de.mrjulsen.paw;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;

import de.mrjulsen.paw.event.ModClientEvents;
import de.mrjulsen.paw.event.ModCommonEvents;
import de.mrjulsen.paw.network.stc.ClearWireConnectionPacket;
import de.mrjulsen.paw.network.stc.UpdateCantileverSettingsPacket;
import de.mrjulsen.paw.network.stc.UpdateWireSettingsPacket;
import de.mrjulsen.paw.registry.ModBlockEntities;
import de.mrjulsen.paw.registry.ModBlocks;
import de.mrjulsen.paw.registry.ModCreativeModeTab;
import de.mrjulsen.paw.registry.ModItems;
import de.mrjulsen.paw.registry.ModWireRegistry;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.mcdragonlib.net.NetworkManagerBase;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.createmod.catnip.lang.FontHelper.Palette;
import software.bernie.geckolib.GeckoLib;
import java.util.List;
import org.slf4j.Logger;

public final class PantographsAndWires {

    public static final String MOD_ID = "pantographsandwires";
    public static final String SHORT_MOD_ID = "paw";
    public static final Logger LOGGER = LogUtils.getLogger();

    //public static final ResourceLocation WIRE_NET = new ResourceLocation(PantographsAndWires.MOD_ID, "trains");

    public static final String NBT_DATA_FIXER = MOD_ID + "_datafixer_version";
    public static final int DATA_FIXER_VERSION = 2;
    
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    static {
		REGISTRATE.setTooltipModifierFactory(item -> {
			return new ItemDescription.Modifier(item, Palette.STANDARD_CREATE)
				.andThen(TooltipModifier.mapNull(KineticStats.create(item)));
		});
	}
    
    private static NetworkManagerBase net;

    public static void load() {}

    public static void init() {

        GeckoLib.initialize();
        WiresApi.init();

        ModWireRegistry.init();
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModCreativeModeTab.setup();
        //ModDataFixers.init();

        CrossPlatform.registerConfig();
        ModCommonEvents.init();
        if (Platform.getEnvironment() == Env.CLIENT) {
            ModClientEvents.init();
        }
        
        net = new NetworkManagerBase(MOD_ID, "paw_network", List.of(
            // cts
            // stc
            ClearWireConnectionPacket.class,
            UpdateCantileverSettingsPacket.class,
            UpdateWireSettingsPacket.class
        ));
    }

    public static NetworkManagerBase net() {
        return net;
    }

    public static boolean useAdvancedLogging() {
        return Platform.isDevelopmentEnvironment();
    }

    public static boolean isEmbeddiumLoaded() {
        return Platform.isModLoaded("embeddium");
    }

    public static boolean isSodiumLoaded() {
        return Platform.isFabric() && Platform.isModLoaded("sodium");
    }

    public static boolean isIndiumLoaded() {
        return Platform.isFabric() && Platform.isModLoaded("indium");
    }
}
