package de.mrjulsen.wires.decoration;

import org.joml.Vector3f;

public record WireDecorationRenderData(Vector3f worldPos, Vector3f direction, WireDecorationData data) {}
