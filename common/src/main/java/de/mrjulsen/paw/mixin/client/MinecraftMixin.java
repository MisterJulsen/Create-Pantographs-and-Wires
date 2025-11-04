package de.mrjulsen.paw.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.paw.data.CustomHitResultTypes;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.wires.item.IWireInteractableItem;
import de.mrjulsen.wires.network.ModNetworkManager;
import de.mrjulsen.wires.network.WireInteractionData;
import de.mrjulsen.wires.network.packets.cts.WireInteractionPacketData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.HitResult;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/HitResult;getType()Lnet/minecraft/world/phys/HitResult$Type;"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void onStartUsingItem(CallbackInfo ci, InteractionHand[] a, int b, int c, InteractionHand hand) {
        HitResult res = Minecraft.getInstance().hitResult;
        if (res == null) {
            return;
        }
        
        LocalPlayer player = Minecraft.getInstance().player;
        if (res.getType() == CustomHitResultTypes.WIRE.getType()) {
            WireHitResult hit = (WireHitResult)res;
            InteractionResult interactionresult = null;
            if (player.getItemInHand(hand).getItem() instanceof IWireInteractableItem i) {
                interactionresult = i.interactWithWire(player.level(), player, hand, hit);
            }
            if (interactionresult == null || !interactionresult.consumesAction()) {
                interactionresult = hit.getWireId().type().use(player.level(), player, hand, hit);
            }
            
            ModNetworkManager.WIRE_INTERACTION.send(NetworkDirection.toServer(), new WireInteractionPacketData(new WireInteractionData(hand, hit)));
            
            if (interactionresult.consumesAction()) {
                if (interactionresult.shouldSwing()) {
                    player.swing(hand);
                }
            }
            ci.cancel();
        }
    }
}
