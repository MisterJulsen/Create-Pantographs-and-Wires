package de.mrjulsen.paw.mixin.client;

import java.util.Optional;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.paw.util.collision.RaycastHitResult;
import de.mrjulsen.paw.util.collision.RaycastUtils;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.NewWireCollision.WireBlockCollision;
import de.mrjulsen.wires.network.WireId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    
    @Inject(method = "pick", at = @At("TAIL"))
    public void paw$pick(float partialTicks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Entity viewEntity = mc.getCameraEntity();
        if (viewEntity == null || mc.level == null)
            return;

        Vec3 from = viewEntity.getEyePosition(partialTicks);
        Vec3 look = viewEntity.getViewVector(partialTicks);
        Vec3 to = from.add(look.scale(5)); // TODO Minecraft.getInstance().gameMode.getPlayerMode().pickRange));

        Optional<RaycastHitResult> result = RaycastUtils.rayTrace(
            new Vector3f((float)from.x, (float)from.y, (float)from.z),
            new Vector3f((float)to.x, (float)to.y, (float)to.z),
            mc.level,
            DragonLib.BLOCK_PIXEL * 4,
            DragonLib.BLOCK_PIXEL * 2,
            WireId::checkCollision
        );

        result.ifPresent(hit -> {
            float distanceSqr = (float)hit.getLocation().distanceTo(from);
            double currentHitDistance = mc.hitResult != null ? from.distanceTo(mc.hitResult.getLocation()) : Integer.MAX_VALUE;

            if (distanceSqr < currentHitDistance) {
                WireBlockCollision c = (WireBlockCollision)hit.getHitData();
                WireEdge edge = c.getCollision().getGraph().getEdge(c.getCollision().getId());
                if (edge == null) {
                    return;
                }
                
                float posOnWire = c.getCollision().worldPosToWirePos(c.getWireName(), hit.getLocation().toVector3f());
                mc.hitResult = new WireHitResult(
                    new Vec3(hit.getLocation().x(), hit.getLocation().y(), hit.getLocation().z()),
                    posOnWire,
                    hit.getBlockPos(),
                    edge.getGraph().getId(),
                    new WireId(c.getCollision().getId(), c.getWireName(), edge.getType())
                );
            }
        });
    }
}
