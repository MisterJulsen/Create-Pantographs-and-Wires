package de.mrjulsen.paw.util;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class ClientUtils {
    public static void renderDebugLine(PoseStack poseStack, VertexConsumer consumer, Vector3f from, Vector3f to, float r, float g, float b, float a) {
        Matrix4f matrix4f = poseStack.last().pose();
        Matrix3f matrix3f = poseStack.last().normal();

        float dx = to.x() - from.x();
        float dy = to.y() - from.y();
        float dz = to.z() - from.z();
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (length > 0) {
            dx /= length;
            dy /= length;
            dz /= length;
        }

        consumer.vertex(matrix4f, (float) from.x(), (float) from.y(), (float) from.z()).color(r, g, b, a).normal(matrix3f, dx, dy, dz).endVertex();
        consumer.vertex(matrix4f, (float) to.x(), (float) to.y(), (float) to.z()).color(r, g, b, a).normal(matrix3f, dx, dy, dz).endVertex();
    }

    public static void resetTranslation(PoseStack poseStack) {
        PoseStack.Pose currentPose = poseStack.last();
        Matrix4f matrix = currentPose.pose();
        matrix.m30(0);
        matrix.m31(0);
        matrix.m32(0);
    }

    public static void resetRotation(PoseStack poseStack) {
        PoseStack.Pose currentPose = poseStack.last();
        Matrix4f matrix = currentPose.pose();
    
        Vector3f translation = new Vector3f();
        matrix.getTranslation(translation);
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        
        matrix.identity();
        matrix.translate(translation);
        matrix.scale(scale);
    }
    
    public static void resetScale(PoseStack poseStack) {
        PoseStack.Pose currentPose = poseStack.last();
        Matrix4f matrix = currentPose.pose();

        Vector3f translation = new Vector3f();
        matrix.getTranslation(translation);
        Quaternionf rotation = new Quaternionf();
        matrix.getUnnormalizedRotation(rotation);

        matrix.identity();
        matrix.translate(translation);
        matrix.rotate(rotation);
    }
}
