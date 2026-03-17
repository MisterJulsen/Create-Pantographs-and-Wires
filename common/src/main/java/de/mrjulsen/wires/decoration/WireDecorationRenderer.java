package de.mrjulsen.wires.decoration;

import org.joml.Vector3d;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public abstract class WireDecorationRenderer<T extends IWireDecoration<T>> {

    protected final T decoration;

    public WireDecorationRenderer(T decoratin) {
        this.decoration = decoratin;
    }

    public abstract void render(PoseStack poseStack, VertexConsumer consumer, Vector3d pos, Vector3d direction, int light);
}
