package de.mrjulsen.wires.decoration;

import org.joml.Vector3d;

public record WireDecorationRenderData(Vector3d worldPos, Vector3d direction, WireDecorationData data) {}
