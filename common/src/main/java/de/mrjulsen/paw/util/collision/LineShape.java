package de.mrjulsen.paw.util.collision;

import org.joml.Vector3d;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.Optional;

public class LineShape implements IRayTraceShape {
    private final Vector3d pointA;
    private final Vector3d pointB;
    private final float radius;

    public LineShape(Vector3f a, Vector3f b, float radius) {
        this.pointA = new Vector3d(a);
        this.pointB = new Vector3d(b);
        this.radius = radius;
    }

    public LineShape(Vector3d a, Vector3d b, float radius) {
        this.pointA = new Vector3d(a);
        this.pointB = new Vector3d(b);
        this.radius = radius;
    }

    @Override
    public Optional<Vector3d> intersects(Vector3d rayOrigin, Vector3d rayDirection) {
        Vector3d ba = new Vector3d(pointB).sub(pointA);
        Vector3d oa = new Vector3d(rayOrigin).sub(pointA);

        Vector3d d = new Vector3d(rayDirection);
        Vector3d baNorm = new Vector3d(ba).normalize();

        Vector3d dProj = projectOntoPlane(d, baNorm);
        Vector3d oaProj = projectOntoPlane(oa, baNorm);

        double a = dProj.dot(dProj);
        double b = 2f * dProj.dot(oaProj);
        double c = oaProj.dot(oaProj) - radius * radius;

        double discriminant = b * b - 4f * a * c;
        if (discriminant < 0) {
            return Optional.empty();
        }

        double sqrtDisc = Math.sqrt(discriminant);
        double t1 = (-b - sqrtDisc) / (2f * a);
        double t2 = (-b + sqrtDisc) / (2f * a);

        double t = Math.min(t1, t2);
        if (t < 0) t = Math.max(t1, t2);
        if (t < 0) return Optional.empty();

        Vector3d hitPoint = new Vector3d(rayOrigin).fma(t, d);

        double hitProj = new Vector3d(hitPoint).sub(pointA).dot(baNorm);
        if (hitProj < 0 || hitProj > ba.length()) {
            return Optional.empty();
        }

        return Optional.of(hitPoint);
    }

    private Vector3d projectOntoPlane(Vector3d vec, Vector3d normal) {
        Vector3d proj = new Vector3d(normal).mul(vec.dot(normal));
        return new Vector3d(vec).sub(proj);
    }
}
