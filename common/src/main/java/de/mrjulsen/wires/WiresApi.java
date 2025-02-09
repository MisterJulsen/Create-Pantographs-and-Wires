package de.mrjulsen.wires;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.wires.network.NetworkManager;

public class WiresApi {
    public static final String MOD_ID = PantographsAndWires.MOD_ID;//"wiresapi";
    public static final float PIXEL = DragonLib.PIXEL;
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        NetworkManager.init();
    }
}
