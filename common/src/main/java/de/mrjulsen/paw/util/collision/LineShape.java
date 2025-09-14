package de.mrjulsen.paw.util.collision;

import org.joml.Vector3f;
import java.util.Optional;

public class LineShape implements IRayTraceShape {
    private final Vector3f pointA;
    private final Vector3f pointB;
    private final float radius;

    public LineShape(Vector3f a, Vector3f b, float radius) {
        this.pointA = new Vector3f(a);
        this.pointB = new Vector3f(b);
        this.radius = radius;
    }

    @Override
    public Optional<Vector3f> intersects(Vector3f rayOrigin, Vector3f rayDirection) {
        Vector3f ba = new Vector3f(pointB).sub(pointA);
        Vector3f oa = new Vector3f(rayOrigin).sub(pointA);

        Vector3f d = new Vector3f(rayDirection);
        Vector3f baNorm = new Vector3f(ba).normalize();

        Vector3f dProj = projectOntoPlane(d, baNorm);
        Vector3f oaProj = projectOntoPlane(oa, baNorm);

        float a = dProj.dot(dProj);
        float b = 2f * dProj.dot(oaProj);
        float c = oaProj.dot(oaProj) - radius * radius;

        float discriminant = b * b - 4f * a * c;
        if (discriminant < 0) {
            return Optional.empty();
        }

        float sqrtDisc = (float) Math.sqrt(discriminant);
        float t1 = (-b - sqrtDisc) / (2f * a);
        float t2 = (-b + sqrtDisc) / (2f * a);

        float t = Math.min(t1, t2);
        if (t < 0) t = Math.max(t1, t2);
        if (t < 0) return Optional.empty();

        Vector3f hitPoint = new Vector3f(rayOrigin).fma(t, d);
        
        float hitProj = new Vector3f(hitPoint).sub(pointA).dot(baNorm);
        if (hitProj < 0 || hitProj > ba.length()) {
            return Optional.empty();
        }

        return Optional.of(hitPoint);
    }

    private Vector3f projectOntoPlane(Vector3f vec, Vector3f normal) {
        Vector3f proj = new Vector3f(normal).mul(vec.dot(normal));
        return new Vector3f(vec).sub(proj);
    }
}
