package de.mrjulsen.paw.fabric.client;

import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.fabric.client.model.loaders.MultipartObjLoader;
import de.mrjulsen.paw.fabric.compat.sodium.IncompatabilityScreen;
import de.mrjulsen.paw.fabric.compat.sodium.SodiumCompatEvent;
import de.mrjulsen.wires.debug.WireDebugRenderer;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import io.github.fabricators_of_create.porting_lib.models.geometry.RegisterGeometryLoadersCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class ClientSetup implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(MultipartObjLoader.INSTANCE);
        RegisterGeometryLoadersCallback.EVENT.register(loaders -> {
            loaders.put(MultipartObjLoader.ID, MultipartObjLoader.INSTANCE);
        });
        WorldRenderEvents.LAST.register((WorldRenderContext context) -> {
            WireDebugRenderer.renderWireCollisions(context.matrixStack());
        });
        

        /*
        if (PantographsAndWires.isSodiumLoaded()) {
            SodiumCompatEvent.init();

            if (!PantographsAndWires.isIndiumLoaded()) {
                throw new IllegalStateException("Sodium is installed, but Indium is not! This mod does not work when Sodium is installed without Indium. Blocks have visual glitches and the wires cause crashes which makes the game unplayable. Please install Indium and restart the game.");
            }
        }
            */
    }
}
