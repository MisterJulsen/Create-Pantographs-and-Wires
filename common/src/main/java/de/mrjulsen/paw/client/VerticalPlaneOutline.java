package de.mrjulsen.paw.client;

import com.simibubi.create.AllSpecialTextures;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.createmod.catnip.outliner.Outline;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.render.BindableTexture;
import net.createmod.catnip.render.PonderRenderTypes;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.world.phys.Vec3;

public class VerticalPlaneOutline extends Outline {

    private static final float WORLD_BOTTOM = Short.MIN_VALUE;
    private static final float SIDE_LINE_WIDTH = DragonLib.BLOCK_PIXEL;
    private static final float TOP_LINE_WIDTH = DragonLib.BLOCK_PIXEL * 2;

    private Vector3d pointA;
    private Vector3d pointB;

    private final Vector4f colorTemp = new Vector4f();
    private final Vector3f pos0Temp = new Vector3f();
    private final Vector3f pos1Temp = new Vector3f();
    private final Vector3f pos2Temp = new Vector3f();
    private final Vector3f pos3Temp = new Vector3f();
    private final Vector3f normalTemp = new Vector3f();

    public VerticalPlaneOutline(Vector3d pointA, Vector3d pointB) {
        this.pointA = pointA;
        this.pointB = pointB;
    }

    public void set(Vector3d pointA, Vector3d pointB) {
        this.pointA = pointA;
        this.pointB = pointB;
    }

    @Override
    public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
        params.loadColor(colorTemp);
        renderPlane(ms, buffer, camera, pointA, pointB, colorTemp);
    }

    private void renderPlane(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, Vector3d a, Vector3d b, Vector4f color) {
        BindableTexture faceTexture = AllSpecialTextures.CHECKERED;

        double ax = a.x - camera.x;
        double ay = a.y - camera.y;
        double az = a.z - camera.z;
        double bx = b.x - camera.x;
        double by = b.y - camera.y;
        double bz = b.z - camera.z;

        double heightA = ay - WORLD_BOTTOM + camera.y;
        double heightB = by - WORLD_BOTTOM + camera.y;
        double bottomAy = ay - heightA;
        double bottomBy = by - heightB;

        Vector3f pos0 = pos0Temp;
        Vector3f pos1 = pos1Temp;
        Vector3f pos2 = pos2Temp;
        Vector3f pos3 = pos3Temp;
        Vector3f normal = normalTemp;

        pos0.set((float) ax, (float) ay, (float) az);
        pos1.set((float) ax, (float) bottomAy, (float) az);
        pos2.set((float) bx, (float) bottomBy, (float) bz);
        pos3.set((float) bx, (float) by, (float) bz);

        double dx = bx - ax;
        double dz = bz - az;
        double len = Math.sqrt(dx * dx + dz * dz);
        double avgHeight = (heightA + heightB) / 2.0;

        normal.set((float) (-dz / len), 0f, (float) (dx / len));

        float maxU = (float) len;
        float maxV = (float) avgHeight;

        PoseStack.Pose pose = ms.last();
        VertexConsumer consumer = buffer.getLateBuffer(PonderRenderTypes.crumbling(faceTexture.getLocation()));

        bufferQuad(pose, consumer, pos0, pos1, pos2, pos3, color, 0, 0, maxU, maxV, LightTexture.FULL_BRIGHT, normal);

        normal.set((float) (dz / len), 0f, (float) (-dx / len));
        bufferQuad(pose, consumer, pos3, pos2, pos1, pos0, color, 0, 0, maxU, maxV, LightTexture.FULL_BRIGHT, normal);

        VertexConsumer lineConsumer = buffer.getBuffer(PonderRenderTypes.outlineSolid());

        Vector3d topA = new Vector3d(ax, ay, az);
        Vector3d topB = new Vector3d(bx, by, bz);
        Vector3d botA = new Vector3d(ax, bottomAy, az);
        Vector3d botB = new Vector3d(bx, bottomBy, bz);

        final Vec3 originTemp = Vec3.ZERO;
        bufferCuboidLine(ms, lineConsumer, originTemp, topA, topB, TOP_LINE_WIDTH, color, LightTexture.FULL_BRIGHT, false);
        bufferCuboidLine(ms, lineConsumer, originTemp, botA, botB, SIDE_LINE_WIDTH, color, LightTexture.FULL_BRIGHT, false);
        bufferCuboidLine(ms, lineConsumer, originTemp, topA, botA, SIDE_LINE_WIDTH, color, LightTexture.FULL_BRIGHT, false);
        bufferCuboidLine(ms, lineConsumer, originTemp, topB, botB, SIDE_LINE_WIDTH, color, LightTexture.FULL_BRIGHT, false);
    }
}