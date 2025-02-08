package de.mrjulsen.paw.fabric.client;

import de.mrjulsen.paw.fabric.client.model.loaders.MultipartObjLoader;
import de.mrjulsen.wires.debug.WireDebugRenderer;
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
    }
}
