package de.mrjulsen.wires;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.mrjulsen.wires.render.WireRenderData;
import de.mrjulsen.wires.render.WireRenderPoint;
import de.mrjulsen.wires.render.WireRenderPoint.VertexCorner;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;


public final class WireBuilder {

    public enum CableType { HANGING, TENSION, TIGHT; }

    public static Wire createWire(String name, WireCreationContext context, Vector3d start, Vector3d end, CableType type, double thickness, double hangFac, SegmentControl segControl) {
        WireRenderData renderData = null;
        if (context.renderingRequired()) {
            renderData = createWireRenderData(start, end, type, thickness, hangFac, segControl);
        }
        WirePoints collisionData = null;
        if (context.collisionRequired()) {
            collisionData = createWirePoints(start, end, type, hangFac, segControl);
        }
        return new Wire(name, new Vector3d(end).add(start).div(2), collisionData, renderData);
    }

    public static WireRenderData createWireRenderData(Vector3d start, Vector3d end, CableType type, double thickness, double hangFac, SegmentControl segControl) {
        Vector3d[] path = generateWirePath(start, end, type, hangFac, segControl, true);
        // If sub-segments are desired for rendering, subdivide each segment using the sub configuration.
        int totalPoints = path.length;
        List<Vector3d> fullPath = new ArrayList<>();
        for (int i = 0; i < totalPoints - 1; i++) {
            Vector3d segStart = path[i];
            Vector3d segEnd = path[i + 1];
            fullPath.add(new Vector3d(segStart));
            double segLength = new Vector3d(segEnd).sub(segStart).length();
            int subCount = segControl.computeSubSegmentCount(segLength);
            // Generate intermediate points – subdivision does not change the cable geometry.
            for (int j = 1; j < subCount; j++) {
                float t = j / (float) subCount;
                Vector3d subPoint = new Vector3d(segStart).lerp(segEnd, t);
                fullPath.add(subPoint);
            }
        }
        fullPath.add(new Vector3d(path[totalPoints - 1])); // add last point

        Vector3d[] renderPath = fullPath.toArray(new Vector3d[0]);
        int numPoints = renderPath.length;
        WireRenderData wire = new WireRenderData(numPoints);
        for (int i = 0; i < numPoints; i++) {
            Vector3d point = renderPath[i];
            Vector3d tangent = calculateTangent(renderPath, i);
            WireRenderPoint vertices = calcVertices(point, tangent, thickness, point);
            wire.setPoint(vertices, i);
        }
        return wire;
    }

    public static WirePoints createWirePoints(Vector3d start, Vector3d end, CableType type, double hangFac, SegmentControl segControl) {
        Vector3d[] path = generateWirePath(start, end, type, hangFac, segControl, false);
        return new WirePoints(path);
    }

    private static Vector3d[] generateWirePath(Vector3d start, Vector3d end, CableType type, double hangFac, SegmentControl segControl, boolean rendering) {
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
        Vector3d direction = new Vector3d(end).sub(start);
        double length = direction.length();
        Vector3d normalized = new Vector3d(direction).normalize();
        int segCount = segControl.computeSegmentCount(start, end, type, hangFac, rendering);
        int numPoints = segCount + 1;
        Vector3d[] path = new Vector3d[numPoints];
        if (type == CableType.TIGHT) {
            for (int i = 0; i < numPoints; i++) {
                float t = i / (float) (numPoints - 1);
                path[i] = new Vector3d(start).add(new Vector3d(normalized).mul(t * length));
            }
        } else {
            Vector2d p1 = new Vector2d(0, 0);
            Vector2d p2 = new Vector2d(length / 2f, direction.y / 2f - Math.min(hangFac, length / 2f));
            Vector2d p3 = new Vector2d(length, direction.y);
            Vector2d center = SegmentControl.circumcenter(p1, p2, p3);
            double rad = SegmentControl.radius(center, p1);
            int totalPointsForArc = numPoints;
            Vector2d[] arcPoints = (type == CableType.HANGING)
                    ? SegmentControl.equallyDistributedPointsOnArc(center, p1, p3, rad, totalPointsForArc)
                    : SegmentControl.equallyDistributedPointsOnX(center, p1, p3, rad, totalPointsForArc);
            for (int i = 0; i < totalPointsForArc; i++) {
                Vector2d arcPoint = arcPoints[i];
                Vector3d projected = projectPointOnVectorPlane(normalized, arcPoint);
                path[i] = new Vector3d(start).add(projected);
            }
        }
        return path;
    }

    private static Vector3d[] generateCustomPathLinear(Vector3d start, Vector3d end, SegmentControl segControl) {
        double totalLength = new Vector3d(end).sub(start).length();
        Vector3d direction = new Vector3d(end).sub(start).normalize();
        double[] custom = segControl.getMainCustomLengths();
        List<Double> segmentLengths = new ArrayList<>();
        double cum = 0f;
        for (double len : custom) {
            if (cum >= totalLength) break;
            if (segControl.getMainMode() == SegmentControl.SegmentationMode.CUSTOM_MAX && len > segControl.getMainMaxLength()) {
                int n = (int) Math.ceil(len / segControl.getMainMaxLength());
                double subLen = len / n;
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
                    double truncated = totalLength - cum;
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
            double remaining = totalLength - cum;
            if (segControl.getMainMode() == SegmentControl.SegmentationMode.CUSTOM_MAX && remaining > segControl.getMainMaxLength()) {
                int n = (int) Math.ceil(remaining / segControl.getMainMaxLength());
                double subLen = remaining / n;
                for (int i = 0; i < n; i++) {
                    segmentLengths.add(subLen);
                }
            } else {
                segmentLengths.add(remaining);
            }
        }
        List<Vector3d> points = new ArrayList<>();
        points.add(new Vector3d(start));
        Vector3d current = new Vector3d(start);
        for (double segLen : segmentLengths) {
            current = new Vector3d(current).add(new Vector3d(direction).mul(segLen));
            points.add(new Vector3d(current));
        }
        return points.toArray(new Vector3d[0]);
    }

// In WireBuilder.java
private static Vector3d[] generateCustomPathArc(Vector3d start, Vector3d end, double hangFac, SegmentControl segControl) {
    boolean useArc = segControl.getMainUseArc();
    Vector3d diff = new Vector3d(end).sub(start);
    double linearLength = diff.length();
    Vector2d p1 = new Vector2d(0, 0);
    Vector2d p3 = new Vector2d(linearLength, diff.y);
    Vector2d p2 = new Vector2d(linearLength / 2f, diff.y / 2f - Math.min(hangFac, linearLength / 2f));
    Vector2d center = SegmentControl.circumcenter(p1, p2, p3);
    double rad = SegmentControl.radius(center, p1);
    double startAngle = Math.atan2(p1.y - center.y, p1.x - center.x);
    double endAngle = Math.atan2(p3.y - center.y, p3.x - center.x);
    if (endAngle < startAngle) {
        endAngle += 2 * Math.PI;
    }

    double[] custom = segControl.getMainCustomLengths();
    List<Double> segmentLengths = new ArrayList<>();
    
    if (useArc) {
        double totalArcLength = SegmentControl.arcLength(center, p1, p3, rad);
        double cum = 0f;
        for (double len : custom) {
            if (cum >= totalArcLength) break;
            if (segControl.getMainMode() == SegmentControl.SegmentationMode.CUSTOM_MAX && len > segControl.getMainMaxLength()) {
                int n = (int) Math.ceil(len / segControl.getMainMaxLength());
                double subLen = len / n;
                for (int i = 0; i < n; i++) {
                    if (cum + subLen > totalArcLength) {
                        segmentLengths.add(totalArcLength - cum);
                        cum = totalArcLength;
                        break;
                    } else {
                        segmentLengths.add(subLen);
                        cum += subLen;
                    }
                }
            } else {
                if (cum + len > totalArcLength) {
                    segmentLengths.add(totalArcLength - cum);
                    cum = totalArcLength;
                    break;
                } else {
                    segmentLengths.add(len);
                    cum += len;
                }
            }
        }
        if (cum < totalArcLength) {
            double remaining = totalArcLength - cum;
            if (segControl.getMainMode() == SegmentControl.SegmentationMode.CUSTOM_MAX && remaining > segControl.getMainMaxLength()) {
                int n = (int) Math.ceil(remaining / segControl.getMainMaxLength());
                double subLen = remaining / n;
                for (int i = 0; i < n; i++) {
                    segmentLengths.add(subLen);
                }
            } else {
                segmentLengths.add(remaining);
            }
        }
        
        List<Vector3d> points = new ArrayList<>();
        points.add(new Vector3d(start));
        float cumulativeArcLength = 0f;
        Vector3d direction = new Vector3d(end).sub(start).normalize();
        
        for (double segLen : segmentLengths) {
            cumulativeArcLength += segLen;
            double fraction = cumulativeArcLength / totalArcLength;
            double angle = startAngle + fraction * (endAngle - startAngle);
            double x2d = center.x + rad * (float) Math.cos(angle);
            double y2d = center.y + rad * (float) Math.sin(angle);
            Vector2d arcPoint = new Vector2d(x2d, y2d);
            Vector3d projected = projectPointOnVectorPlane(direction, arcPoint);
            points.add(new Vector3d(start).add(projected));
        }
        return points.toArray(new Vector3d[0]);

    } else {
        double totalHorizontalLength = p3.x - p1.x;
        double cum = 0f;
        for (double len : custom) {
            if (cum >= totalHorizontalLength) break;
            if (segControl.getMainMode() == SegmentControl.SegmentationMode.CUSTOM_MAX && len > segControl.getMainMaxLength()) {
                int n = (int) Math.ceil(len / segControl.getMainMaxLength());
                double subLen = len / n;
                for (int i = 0; i < n; i++) {
                    if (cum + subLen > totalHorizontalLength) {
                        segmentLengths.add(totalHorizontalLength - cum);
                        cum = totalHorizontalLength;
                        break;
                    } else {
                        segmentLengths.add(subLen);
                        cum += subLen;
                    }
                }
            } else {
                if (cum + len > totalHorizontalLength) {
                    segmentLengths.add(totalHorizontalLength - cum);
                    cum = totalHorizontalLength;
                    break;
                } else {
                    segmentLengths.add(len);
                    cum += len;
                }
            }
        }
        if (cum < totalHorizontalLength) {
            double remaining = totalHorizontalLength - cum;
            if (segControl.getMainMode() == SegmentControl.SegmentationMode.CUSTOM_MAX && remaining > segControl.getMainMaxLength()) {
                int n = (int) Math.ceil(remaining / segControl.getMainMaxLength());
                double subLen = remaining / n;
                for (int i = 0; i < n; i++) {
                    segmentLengths.add(subLen);
                }
            } else {
                segmentLengths.add(remaining);
            }
        }

        List<Vector3d> points = new ArrayList<>();
        points.add(new Vector3d(start));
        float cumulativeHorizontalLength = 0f;
        Vector3d direction = new Vector3d(end).sub(start).normalize();

        for (double segLen : segmentLengths) {
            cumulativeHorizontalLength += segLen;
            double x2d = p1.x + cumulativeHorizontalLength;

            double dx = x2d - center.x;
            double dySquared = rad * rad - dx * dx;
            double y2d;
            if (dySquared >= 0) {
                double dy = Math.sqrt(dySquared);
                double candidateY1 = center.y + dy;
                double candidateY2 = center.y - dy;
                double angle1 = Math.atan2(candidateY1 - center.y, x2d - center.x);
                if (endAngle < startAngle) endAngle += 2 * Math.PI;
                
                if (angle1 >= startAngle && angle1 <= endAngle) {
                    y2d = candidateY1;
                } else {
                    y2d = candidateY2;
                }
            } else {
                y2d = center.y;
            }

            Vector2d arcPoint = new Vector2d(x2d, y2d);
            Vector3d projected = projectPointOnVectorPlane(direction, arcPoint);
            points.add(new Vector3d(start).add(projected));
        }
        return points.toArray(new Vector3d[0]);
    }
}

    private static Vector3d calculateTangent(Vector3d[] path, int i) {
        if (i == 0 && path.length > 1) {
            return new Vector3d(path[1]).sub(path[0]).normalize();
        } else if (i == path.length - 1 && path.length > 1) {
            return new Vector3d(path[i]).sub(path[i - 1]).normalize();
        } else if (path.length > 2) {
            return new Vector3d(path[i + 1]).sub(path[i - 1]).normalize();
        }
        return new Vector3d(0, 1, 0);
    }

    private static WireRenderPoint calcVertices(Vector3d start, Vector3d direction, double thickness, Vector3d customCenter) {
        Vector3d norm = new Vector3d(direction).normalize();
        Vector3d rightVec;
        if (Math.abs(norm.x) < 0.1f && Math.abs(norm.z) < 0.1f) {
            rightVec = new Vector3d(1, 0, 0);
        } else {
            rightVec = new Vector3d(norm.z, 0, -norm.x);
        }
        rightVec.normalize().mul(thickness / 2f);
        Vector3d crossVec = new Vector3d(norm).cross(rightVec, new Vector3d()).normalize().mul(thickness / 2f);
        return new WireRenderPoint(Map.of(
            VertexCorner.CENTER, customCenter != null ? customCenter : new Vector3d(start).add(norm),
            VertexCorner.TOP_LEFT, new Vector3d(start).add(new Vector3d(rightVec).add(crossVec)),
            VertexCorner.BOTTOM_RIGHT, new Vector3d(start).sub(new Vector3d(rightVec).add(crossVec)),
            VertexCorner.TOP_RIGHT, new Vector3d(start).add(new Vector3d(crossVec)).sub(rightVec),
            VertexCorner.BOTTOM_LEFT, new Vector3d(start).add(new Vector3d(rightVec)).sub(crossVec)
        ));
    }

    private static Vector3d projectPointOnVectorPlane(Vector3d direction, Vector2d point) {
        Vector3d scaled = new Vector3d(direction).mul(point.x);
        return new Vector3d(scaled.x, point.y, scaled.z);
    }
}
