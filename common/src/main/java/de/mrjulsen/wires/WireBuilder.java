package de.mrjulsen.wires;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.mrjulsen.wires.render.WireRenderData;
import de.mrjulsen.wires.render.WireRenderPoint;
import de.mrjulsen.wires.render.WireRenderPoint.VertexCorner;
import org.joml.Vector2f;
import org.joml.Vector3f;


public final class WireBuilder {

    public enum CableType { HANGING, TENSION, TIGHT; }

    public static Wire createWire(WireCreationContext context, Vector3f start, Vector3f end, CableType type, float thickness, float hangFac, SegmentControl segControl) {
        WireRenderData renderData = null;
        if (context.renderingRequired()) {
            renderData = createWireRenderData(start, end, type, thickness, hangFac, segControl);
        }
        WirePoints collisionData = null;
        if (context.collisionRequired()) {
            collisionData = createWirePoints(start, end, type, hangFac, segControl);
        }
        return new Wire(collisionData, renderData);
    }

    public static WireRenderData createWireRenderData(Vector3f start, Vector3f end, CableType type, float thickness, float hangFac, SegmentControl segControl) {
        Vector3f[] path = generateWirePath(start, end, type, hangFac, segControl, true);
        // If sub-segments are desired for rendering, subdivide each segment using the sub configuration.
        int totalPoints = path.length;
        List<Vector3f> fullPath = new ArrayList<>();
        for (int i = 0; i < totalPoints - 1; i++) {
            Vector3f segStart = path[i];
            Vector3f segEnd = path[i + 1];
            fullPath.add(new Vector3f(segStart));
            float segLength = new Vector3f(segEnd).sub(segStart).length();
            int subCount = segControl.computeSubSegmentCount(segLength);
            // Generate intermediate points – subdivision does not change the cable geometry.
            for (int j = 1; j < subCount; j++) {
                float t = j / (float) subCount;
                Vector3f subPoint = new Vector3f(segStart).lerp(segEnd, t);
                fullPath.add(subPoint);
            }
        }
        fullPath.add(new Vector3f(path[totalPoints - 1])); // add last point

        Vector3f[] renderPath = fullPath.toArray(new Vector3f[0]);
        int numPoints = renderPath.length;
        WireRenderData wire = new WireRenderData(numPoints);
        for (int i = 0; i < numPoints; i++) {
            Vector3f point = renderPath[i];
            Vector3f tangent = calculateTangent(renderPath, i);
            WireRenderPoint vertices = calcVertices(point, tangent, thickness, point);
            wire.setPoint(vertices, i);
        }
        return wire;
    }

    public static WirePoints createWirePoints(Vector3f start, Vector3f end, CableType type, float hangFac, SegmentControl segControl) {
        Vector3f[] path = generateWirePath(start, end, type, hangFac, segControl, false);
        return new WirePoints(path);
    }

    private static Vector3f[] generateWirePath(Vector3f start, Vector3f end, CableType type, float hangFac, SegmentControl segControl, boolean rendering) {
        // For custom segmentation modes, use a separate generation method.
        if (segControl.getMainMode() == SegmentControl.SegmentationMode.CUSTOM ||
            segControl.getMainMode() == SegmentControl.SegmentationMode.CUSTOM_MAX) {
            if (type == CableType.TIGHT) {
                return generateCustomPathLinear(start, end, segControl);
            } else {
                return generateCustomPathArc(start, end, hangFac, segControl);
            }
        }
        // Otherwise, use the standard evenly distributed segmentation.
        Vector3f direction = new Vector3f(end).sub(start);
        float length = direction.length();
        Vector3f normalized = new Vector3f(direction).normalize();
        int segCount = segControl.computeSegmentCount(start, end, type, hangFac, rendering);
        int numPoints = segCount + 1;
        Vector3f[] path = new Vector3f[numPoints];
        if (type == CableType.TIGHT) {
            for (int i = 0; i < numPoints; i++) {
                float t = i / (float) (numPoints - 1);
                path[i] = new Vector3f(start).add(new Vector3f(normalized).mul(t * length));
            }
        } else {
            Vector2f p1 = new Vector2f(0, 0);
            Vector2f p2 = new Vector2f(length / 2f, direction.y / 2f - Math.min(hangFac, length / 2f));
            Vector2f p3 = new Vector2f(length, direction.y);
            Vector2f center = SegmentControl.circumcenter(p1, p2, p3);
            float rad = SegmentControl.radius(center, p1);
            int totalPointsForArc = numPoints;
            Vector2f[] arcPoints = (type == CableType.HANGING)
                    ? SegmentControl.equallyDistributedPointsOnArc(center, p1, p3, rad, totalPointsForArc)
                    : SegmentControl.equallyDistributedPointsOnX(center, p1, p3, rad, totalPointsForArc);
            for (int i = 0; i < totalPointsForArc; i++) {
                Vector2f arcPoint = arcPoints[i];
                Vector3f projected = projectPointOnVectorPlane(normalized, arcPoint);
                path[i] = new Vector3f(start).add(projected);
            }
        }
        return path;
    }

    private static Vector3f[] generateCustomPathLinear(Vector3f start, Vector3f end, SegmentControl segControl) {
        float totalLength = new Vector3f(end).sub(start).length();
        Vector3f direction = new Vector3f(end).sub(start).normalize();
        float[] custom = segControl.getMainCustomLengths();
        List<Float> segmentLengths = new ArrayList<>();
        float cum = 0f;
        for (float len : custom) {
            if (cum >= totalLength) break;
            if (segControl.getMainMode() == SegmentControl.SegmentationMode.CUSTOM_MAX && len > segControl.getMainMaxLength()) {
                int n = (int) Math.ceil(len / segControl.getMainMaxLength());
                float subLen = len / n;
                for (int i = 0; i < n; i++) {
                    if (cum + subLen > totalLength) {
                        subLen = totalLength - cum;
                        segmentLengths.add(subLen);
                        cum = totalLength;
                        break;
                    } else {
                        segmentLengths.add(subLen);
                        cum += subLen;
                    }
                }
            } else {
                if (cum + len > totalLength) {
                    float truncated = totalLength - cum;
                    segmentLengths.add(truncated);
                    cum = totalLength;
                    break;
                } else {
                    segmentLengths.add(len);
                    cum += len;
                }
            }
        }
        // If the custom array does not fill the cable, add a fill segment.
        if (cum < totalLength) {
            float remaining = totalLength - cum;
            if (segControl.getMainMode() == SegmentControl.SegmentationMode.CUSTOM_MAX && remaining > segControl.getMainMaxLength()) {
                int n = (int) Math.ceil(remaining / segControl.getMainMaxLength());
                float subLen = remaining / n;
                for (int i = 0; i < n; i++) {
                    segmentLengths.add(subLen);
                }
            } else {
                segmentLengths.add(remaining);
            }
        }
        List<Vector3f> points = new ArrayList<>();
        points.add(new Vector3f(start));
        Vector3f current = new Vector3f(start);
        for (float segLen : segmentLengths) {
            current = new Vector3f(current).add(new Vector3f(direction).mul(segLen));
            points.add(new Vector3f(current));
        }
        return points.toArray(new Vector3f[0]);
    }

    private static Vector3f[] generateCustomPathArc(Vector3f start, Vector3f end, float hangFac, SegmentControl segControl) {
        Vector3f diff = new Vector3f(end).sub(start);
        float linearLength = diff.length();
        Vector2f p1 = new Vector2f(0, 0);
        Vector2f p2 = new Vector2f(linearLength / 2f, diff.y / 2f - Math.min(hangFac, linearLength / 2f));
        Vector2f p3 = new Vector2f(linearLength, diff.y);
        Vector2f center = SegmentControl.circumcenter(p1, p2, p3);
        float rad = SegmentControl.radius(center, p1);
        float totalArcLength = SegmentControl.arcLength(center, p1, p3, rad);
        float startAngle = (float) Math.atan2(p1.y - center.y, p1.x - center.x);
        float endAngle = (float) Math.atan2(p3.y - center.y, p3.x - center.x);
        if (endAngle < startAngle) {
            endAngle += 2 * (float) Math.PI;
        }
        float theta = endAngle - startAngle;
        float[] custom = segControl.getMainCustomLengths();
        List<Float> segmentLengths = new ArrayList<>();
        float cum = 0f;
        for (float len : custom) {
            if (cum >= totalArcLength) break;
            if (segControl.getMainMode() == SegmentControl.SegmentationMode.CUSTOM_MAX && len > segControl.getMainMaxLength()) {
                int n = (int) Math.ceil(len / segControl.getMainMaxLength());
                float subLen = len / n;
                for (int i = 0; i < n; i++) {
                    if (cum + subLen > totalArcLength) {
                        subLen = totalArcLength - cum;
                        segmentLengths.add(subLen);
                        cum = totalArcLength;
                        break;
                    } else {
                        segmentLengths.add(subLen);
                        cum += subLen;
                    }
                }
            } else {
                if (cum + len > totalArcLength) {
                    float truncated = totalArcLength - cum;
                    segmentLengths.add(truncated);
                    cum = totalArcLength;
                    break;
                } else {
                    segmentLengths.add(len);
                    cum += len;
                }
            }
        }
        // If the custom array does not cover the entire arc, add a fill segment.
        if (cum < totalArcLength) {
            float remaining = totalArcLength - cum;
            if (segControl.getMainMode() == SegmentControl.SegmentationMode.CUSTOM_MAX && remaining > segControl.getMainMaxLength()) {
                int n = (int) Math.ceil(remaining / segControl.getMainMaxLength());
                float subLen = remaining / n;
                for (int i = 0; i < n; i++) {
                    segmentLengths.add(subLen);
                }
            } else {
                segmentLengths.add(remaining);
            }
        }
        List<Vector3f> points = new ArrayList<>();
        points.add(new Vector3f(start));
        float cumulative = 0f;
        Vector3f direction = new Vector3f(end).sub(start).normalize();
        for (float segLen : segmentLengths) {
            cumulative += segLen;
            float fraction = cumulative / totalArcLength;
            float angle = startAngle + fraction * theta;
            float x2d = center.x + rad * (float) Math.cos(angle);
            float y2d = center.y + rad * (float) Math.sin(angle);
            Vector2f arcPoint = new Vector2f(x2d, y2d);
            Vector3f projected = projectPointOnVectorPlane(direction, arcPoint);
            Vector3f point = new Vector3f(start).add(projected);
            points.add(point);
        }
        return points.toArray(new Vector3f[0]);
    }

    private static Vector3f calculateTangent(Vector3f[] path, int i) {
        if (i == 0 && path.length > 1) {
            return new Vector3f(path[1]).sub(path[0]).normalize();
        } else if (i == path.length - 1 && path.length > 1) {
            return new Vector3f(path[i]).sub(path[i - 1]).normalize();
        } else if (path.length > 2) {
            return new Vector3f(path[i + 1]).sub(path[i - 1]).normalize();
        }
        return new Vector3f(0, 1, 0); // Fallback
    }

    private static WireRenderPoint calcVertices(Vector3f start, Vector3f direction, float thickness, Vector3f customCenter) {
        Vector3f norm = new Vector3f(direction).normalize();
        Vector3f rightVec;
        if (Math.abs(norm.x) < 0.1f && Math.abs(norm.z) < 0.1f) {
            rightVec = new Vector3f(1, 0, 0);
        } else {
            rightVec = new Vector3f(norm.z, 0, -norm.x);
        }
        rightVec.normalize().mul(thickness / 2f);
        Vector3f crossVec = new Vector3f(norm).cross(rightVec, new Vector3f()).normalize().mul(thickness / 2f);
        return new WireRenderPoint(Map.of(
            VertexCorner.CENTER, customCenter != null ? customCenter : new Vector3f(start).add(norm),
            VertexCorner.TOP_LEFT, new Vector3f(start).add(new Vector3f(rightVec).add(crossVec)),
            VertexCorner.BOTTOM_RIGHT, new Vector3f(start).sub(new Vector3f(rightVec).add(crossVec)),
            VertexCorner.TOP_RIGHT, new Vector3f(start).add(new Vector3f(crossVec)).sub(rightVec),
            VertexCorner.BOTTOM_LEFT, new Vector3f(start).add(new Vector3f(rightVec)).sub(crossVec)
        ));
    }

    private static Vector3f projectPointOnVectorPlane(Vector3f direction, Vector2f point) {
        Vector3f scaled = new Vector3f(direction).mul(point.x);
        return new Vector3f(scaled.x, point.y, scaled.z);
    }
}
