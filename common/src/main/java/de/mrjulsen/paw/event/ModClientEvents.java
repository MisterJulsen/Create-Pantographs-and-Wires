package de.mrjulsen.paw.event;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import de.mrjulsen.mcdragonlib.client.model.DLBlockModelRegistry;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.DataCache;
import de.mrjulsen.mcdragonlib.util.Holder;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock;
import de.mrjulsen.paw.block.model.CantileverModel;
import de.mrjulsen.paw.client.VerticalPlaneOutline;
import de.mrjulsen.paw.registry.ModBlocks;
import de.mrjulsen.wires.graph.DLStatistics;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.item.IPawWireItemBase;
import de.mrjulsen.wires.item.IWireItemBase;
import de.mrjulsen.wires.render.WireRenderer;
import de.mrjulsen.wires.util.ClientUtils;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.TickEvent;
import net.createmod.catnip.outliner.Outliner;
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

        for (Supplier<? extends AbstractCantileverBlock> cantilever : ModBlocks.getCantilevers()) {
            DLBlockModelRegistry.registerForBlock(() -> cantilever.get(), CantileverModel::new, null);
        }

        ClientGuiEvent.DEBUG_TEXT_LEFT.register((lines) -> {
            if (!PantographsAndWires.useAdvancedLogging()) {
                return;
            }

            List<DLStatistics> serverStats = new LinkedList<>();
            for (WireGraph graph : WireGraphManager.getAll(ClientUtils.level())) {
                serverStats.add(graph.getStatistics());
            }
            List<DLStatistics> clientStats = new LinkedList<>();
            for (WireGraphClient graph : WireGraphManager.getAllClient(ClientUtils.level())) {
                clientStats.add(graph.getStatistics());
            }

            if (!serverStats.isEmpty()) {                
                DLStatistics result = DLStatistics.merge(serverStats.get(0).getName(), (a, b) -> {
                    if (a instanceof Integer i && b instanceof Integer k) {
                        return i + k;
                    }
                    return a;
                }, serverStats.toArray(DLStatistics[]::new));
                lines.add(result.print(false));
            }            
            if (!clientStats.isEmpty()) {                
                DLStatistics result = DLStatistics.merge(clientStats.get(0).getName(), (a, b) -> {
                    if (a instanceof Integer i && b instanceof Integer k) {
                        return i + k;
                    }
                    return a;
                }, clientStats.toArray(DLStatistics[]::new));
                lines.add(result.print(false));
            }
        });

        ClientLifecycleEvent.CLIENT_STARTED.register((mc) -> {
            if (mc != null) {
                ReloadableResourceManager reloadableManager = (ReloadableResourceManager)mc.getResourceManager();
                reloadableManager.registerReloadListener(new WireRenderer());
            } else {
                PantographsAndWires.LOGGER.error("Could not register ReloadableResourceManager.");
            } 
        });

        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register((server) -> {
            WireGraphManager.clearClient();
        });

        Holder.MutableHolder<Component> wireHudText = new Holder.MutableHolder<>(null);
        ClientTickEvent.CLIENT_POST.register((mc) -> {
            Player player = mc.player;
            if (player == null) {
                return;
            }

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);

                if (stack.getItem() instanceof IWireItemBase item) {
                    HitResult lookingAt = mc.hitResult;
                    Component text = item.createHudInfoText(stack, mc.player, lookingAt);
                    wireHudText.set(text);
                    if (text != null) {
                        break;
                    }
                } else {
                    wireHudText.set(null);
                }
            }
        });

        ClientGuiEvent.RENDER_HUD.register((graphics, ticks) -> {
            if (wireHudText.get() != null) {
                Minecraft mc = Minecraft.getInstance();
                int scaledWidth = mc.getWindow().getGuiScaledWidth();
                int scaledHeight = mc.getWindow().getGuiScaledHeight();
                graphics.drawCenteredString(mc.font, wireHudText.get(), scaledWidth / 2, scaledHeight - 100, 0xFFFFFFFF);
            }

            /*
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null) {
                return;
            }

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);

                if (stack.getItem() instanceof IWireItemBase item) {
                    HitResult lookingAt = mc.hitResult;
                    item.renderHelperOutline(stack, mc.player, lookingAt);
                }
            }
             */
        });

        /*
        if (PantographsAndWires.isSodiumLoaded()) {
            SodiumCompatEvent.init();

            if (!PantographsAndWires.isIndiumLoaded()) {
                ClientGuiEvent.SET_SCREEN.register((screen) -> {
                    DLScreenWrapper<?> wrapper = new DLScreenWrapper<>(null, IncompatabilityScreen::new);
                    return CompoundEventResult.interruptTrue(wrapper);
                });
            }
        }
            */
    }
    
}
