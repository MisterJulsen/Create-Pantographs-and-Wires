package de.mrjulsen.paw.event;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.compat.sodium.IncompatabilityScreen;
import de.mrjulsen.paw.compat.sodium.SodiumCompatEvent;
import de.mrjulsen.wires.item.WireBaseItem;
import de.mrjulsen.wires.render.WireRenderer;
import de.mrjulsen.wires.util.ClientUtils;
import de.mrjulsen.wires.WireClientNetwork;
import de.mrjulsen.wires.WireNetwork;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

public final class ModClientEvents {

    public static final ResourceLocation WIRE_TEXTURE = new ResourceLocation(PantographsAndWires.MOD_ID, "textures/block/wire.png");

    private ModClientEvents() {}

    public static void init() {

        ClientGuiEvent.DEBUG_TEXT_LEFT.register((lines) -> {
            if (!PantographsAndWires.useAdvancedLogging()) {
                return;
            }
            lines.add(WireNetwork.get(ClientUtils.level()).debug_text());
            lines.add(WireClientNetwork.get(ClientUtils.level()).debug_text());
        });

        ClientLifecycleEvent.CLIENT_STARTED.register((mc) -> {        
            if (Minecraft.getInstance() != null) {            
                ReloadableResourceManager reloadableManager = (ReloadableResourceManager)Minecraft.getInstance().getResourceManager();
                reloadableManager.registerReloadListener(new WireRenderer());
            } else {
                PantographsAndWires.LOGGER.error("Could not register ReloadableResourceManager.");
            } 
        });

        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register((server) -> {
            WireClientNetwork.clear();
        });

        ClientGuiEvent.RENDER_HUD.register((graphics, ticks) -> {
            Player player = Minecraft.getInstance().player;
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);

                if (stack.getItem() instanceof WireBaseItem item) {
                    HitResult lookingAt = Minecraft.getInstance().hitResult;
                    Component text = item.createHudInfoText(stack, Minecraft.getInstance().player, lookingAt);
                    if (text == null) {
                        continue;
                    }                    
                    int scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                    int scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                    graphics.drawCenteredString(Minecraft.getInstance().font, text, scaledWidth / 2, scaledHeight - 100, 0xFFFFFFFF);
                    break;
                }
            }
        });

        if (PantographsAndWires.isSodiumLoaded()) {
            SodiumCompatEvent.init();

            if (!PantographsAndWires.isIndiumLoaded()) {
                ClientGuiEvent.SET_SCREEN.register((screen) -> {
                    return CompoundEventResult.interruptTrue(new IncompatabilityScreen());
                });
            }
        }
    }
    
}
