package de.mrjulsen.wires;

import java.util.Map;

import de.mrjulsen.wires.client.ClientUtils;
import de.mrjulsen.wires.render.WireRenderData;
import de.mrjulsen.wires.render.WireRenderPoint;
import de.mrjulsen.wires.render.WireRenderPoint.VertexCorner;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import de.mrjulsen.mcdragonlib.util.MathUtils;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class WireBuilder {

	public static final int SEGMENTS_AUTO = -1;

	public static enum CableType { HANGING, TENSION, TIGHT; }
	
	public static int calcSegmentsCount(float arcLength, float distance, float radius, boolean rendering) {
		float multiplier = 1;		
		if (rendering && Platform.getEnvironment() == Env.CLIENT) {
			multiplier = ClientUtils.getMultiplierByGraphicsMode();
		}
		return Math.max(1, (int)(Math.max(Math.ceil(arcLength * 16 / radius), distance) * multiplier));
	}

	public static Wire createWire(WireCreationContext context, Vec3 start, Vec3 end, CableType type, float thickness, float hangFac, int segmentsCount) {
		WireRenderData renderData = null;
		if (context.renderingRequired()) {
			renderData = createWireRenderData(start, end, type, thickness, hangFac, segmentsCount);
		}
		WirePoints collisionData = null;
		if (context.collisionRequired()) {
			collisionData = createWirePoints(start, end, type, hangFac, segmentsCount);
		}
		return new Wire(collisionData, renderData);
	}
	

	public static WireRenderData createWireRenderData(Vec3 start, Vec3 end, CableType type, float thickness, float hangFac, int segmentsCount) {
		Vec3 direction = new Vec3(end.x, end.y, end.z).subtract(start);
		Vec3 normalized = direction.normalize();
		float length = (float)direction.length();
		int maxSegments;
		
		Vec2[] points = null;
		if (type == CableType.TIGHT) {	
			maxSegments = (segmentsCount > 0 ? segmentsCount : (int)length) + 1;
		} else {
			Vec2 p1 = Vec2.ZERO;
			Vec2 p2 = new Vec2(length / 2f, (float)direction.y / 2f - Math.min(hangFac, length / 2f));
			Vec2 p3 = new Vec2(length, (float)direction.y);
			Vec2 center = circumcenter(p1, p2, p3);
			float radius = radius(center, p1);
			float arcLength = arcLength(center, p1, p3, radius);
			maxSegments = (segmentsCount > 0 ? segmentsCount : calcSegmentsCount(arcLength, length, radius, true)) + 1;
			points = type == CableType.HANGING ? equallyDistributedPointsOnArc(center, p1, p3, radius, maxSegments) : equallyDistributedPointsOnX(center, p1, p3, radius, maxSegments);
		}

		WireRenderData wire = new WireRenderData(maxSegments);
		WireRenderPoint lastVertices;
		Vec3 lastPoint = Vec3.ZERO;
		Vec3 lastPoint2 = Vec3.ZERO;

		if (type == CableType.TIGHT) {
			lastVertices = calcVertices(start, normalized, thickness, start);
		} else {
			Vec2 arcPoint;
			Vec3 currentPoint;
			
			arcPoint = points[1];
			currentPoint = projectPointOnVectorPlane(normalized, arcPoint);
			lastVertices = calcVertices(start, currentPoint, thickness, start);
			lastPoint2 = lastPoint;
			lastPoint = currentPoint;
		}

		wire.setPoint(lastVertices, 0);

		for (int i = 1; i < maxSegments; i++) {
			WireRenderPoint vertices;

			if (type == CableType.TIGHT) {
				Vec3 s = start.add(normalized.scale(length / (maxSegments - 1) * i));
				vertices = calcVertices(s, normalized, thickness, s);
			} else {
				Vec2 arcPoint;
				Vec3 currentPoint;

				int idx = MathUtils.clamp(i + 1, 0, maxSegments - 1);
				arcPoint = points[idx];
				currentPoint = projectPointOnVectorPlane(normalized, arcPoint);
				Vec3 s = start.add(lastPoint);
				vertices = calcVertices(s, currentPoint.subtract(lastPoint).add(lastPoint.subtract(lastPoint2)), thickness, start.add(lastPoint));
				lastPoint2 = lastPoint;
				lastPoint = currentPoint;
			}

			wire.setPoint(vertices, i);

			lastVertices = vertices;
		}

		return wire;
	}

	public static WirePoints createWirePoints(Vec3 start, Vec3 end, CableType type, float hangFac, int segmentsCount) {
		Vec3 direction = new Vec3(end.x, end.y, end.z).subtract(start);
		Vec3 normalized = direction.normalize();
		float length = (float)direction.length();
		int maxSegments;
		
		Vec2[] points = null;
		if (type == CableType.TIGHT) {	
			maxSegments = (segmentsCount > 0 ? segmentsCount : (int)length) + 1;
		} else {
			Vec2 p1 = Vec2.ZERO;
			Vec2 p2 = new Vec2(length / 2f, (float)direction.y / 2f - Math.min(hangFac, length / 2f));
			Vec2 p3 = new Vec2(length, (float)direction.y);
			Vec2 center = circumcenter(p1, p2, p3);
			float radius = radius(center, p1);
			float arcLength = arcLength(center, p1, p3, radius);
			maxSegments = (segmentsCount > 0 ? segmentsCount : calcSegmentsCount(arcLength, length, radius, false)) + 1;
			points = type == CableType.HANGING ? equallyDistributedPointsOnArc(center, p1, p3, radius, maxSegments) : equallyDistributedPointsOnX(center, p1, p3, radius, maxSegments);
		}

		Vec3[] wire = new Vec3[maxSegments];
		Vec3 lastPoint = Vec3.ZERO;
		final float segmentLength = length / (maxSegments - 1);

		wire[0] = start;
		wire[wire.length - 1] = end;

		for (int i = 1; i < maxSegments; i++) {
			if (type == CableType.TIGHT) {
				Vec3 s = start.add(normalized.scale(segmentLength * i));
				wire[i] = s;
			} else {
				Vec2 arcPoint;
				Vec3 currentPoint;

				int idx = MathUtils.clamp(i + 1, 0, maxSegments - 1);
				arcPoint = points[idx];
				currentPoint = projectPointOnVectorPlane(normalized, arcPoint);
				wire[i] = start.add(lastPoint);
				lastPoint = currentPoint;
			}
		}

		return new WirePoints(wire);
	}
	
	public static WireRenderPoint calcVertices(Vec3 start, Vec3 direction, float thickness, Vec3 customCenter) {
		Vec3 norm = direction.normalize();
		Vec3 rightVec = (Math.abs(direction.x) < 0.1f && Math.abs(direction.z) < 0.1f ? new Vec3(1, 0, 0) : new Vec3(norm.z, 0, -norm.x)).normalize().scale(thickness / 2f);
		Vec3 crossVec = norm.cross(rightVec).normalize().scale(thickness / 2f);
		
		return new WireRenderPoint(Map.of(
			VertexCorner.CENTER, customCenter == null ? start.add(direction) : customCenter,
			VertexCorner.TOP_LEFT, crossVec.add(rightVec).add(start),
			VertexCorner.BOTTOM_RIGHT, Vec3.ZERO.subtract(crossVec).subtract(rightVec).add(start),
			VertexCorner.TOP_RIGHT, crossVec.subtract(rightVec).add(start),
			VertexCorner.BOTTOM_LEFT, rightVec.subtract(crossVec).add(start)
		));
	}

    public static Vec2 circumcenter(Vec2 a, Vec2 b, Vec2 c) {
        float d = 2 * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y));
        float ux = ((a.x * a.x + a.y * a.y) * (b.y - c.y) + (b.x * b.x + b.y * b.y) * (c.y - a.y) + (c.x * c.x + c.y * c.y) * (a.y - b.y)) / d;
        float uy = ((a.x * a.x + a.y * a.y) * (c.x - b.x) + (b.x * b.x + b.y * b.y) * (a.x - c.x) + (c.x * c.x + c.y * c.y) * (b.x - a.x)) / d;
        return new Vec2(ux, uy);
    }

    public static float radius(Vec2 center, Vec2 p) {
        return (float)Math.sqrt(Math.pow(center.x - p.x, 2) + Math.pow(center.y - p.y, 2));
    }

    public static float arcLength(Vec2 center, Vec2 a, Vec2 b, float radius) {
        float angle = (float)Math.acos(((a.x - center.x) * (b.x - center.x) + (a.y - center.y) * (b.y - center.y)) / (radius * radius));
        return radius * angle;
    }
    
    public static Vec2[] equallyDistributedPointsOnArc(Vec2 center, Vec2 start, Vec2 end, float radius, int segments) {
        Vec2[] points = new Vec2[segments];
        float startAngle = (float)Math.atan2(start.y - center.y, start.x - center.x);
        float endAngle = (float)Math.atan2(end.y - center.y, end.x - center.x);
        
        if (endAngle < startAngle) {
            endAngle += 2 * Math.PI;
        }

        float angleStep = (endAngle - startAngle) / (segments - 1);

		points[0] = start;
		points[points.length - 1] = end;

        float angle = startAngle;
        for (int i = 1; i < segments - 1; i++) {
            angle += angleStep;
            float x = (float)(center.x + radius * Math.cos(angle));
            float y = (float)(center.y + radius * Math.sin(angle));
            points[i] = new Vec2(x, y);
        } 
        
        return points;
    }
	
	public static Vec2[] equallyDistributedPointsOnX(Vec2 center, Vec2 start, Vec2 end, float radius, int segments) {
		Vec2[] points = new Vec2[segments];
	
		float startX = start.x;
		float endX = end.x;
	
		float stepX = (endX - startX) / (segments - 1);
	
		points[0] = start;
		points[points.length - 1] = end;

		float startAngle = (float) Math.atan2(start.y - center.y, start.x - center.x);
		float endAngle = (float) Math.atan2(end.y - center.y, end.x - center.x);
		float x = startX;

		for (int i = 1; i < segments - 1; i++) {
			x += stepX;
			float dx = x - center.x;
			float dySquared = radius * radius - dx * dx;

			if (dySquared < 0) {
				dySquared = 0;
			}
	
			float dy = dySquared == 0 ? 0 : (float)Math.sqrt(dySquared);
			float y1 = center.y + dy;
			float y2 = center.y - dy;

			float angle1 = (float) Math.atan2(y1 - center.y, x - center.x);	
			
			if (endAngle < startAngle) {
				endAngle += 2 * Math.PI;
			}
	
			float y;
			if (angle1 >= startAngle && angle1 <= endAngle) {
				y = y1;
			} else {
				y = y2;
			}
	
			points[i] = new Vec2(x, y);
		}
	
		return points;
	}
	

	private static Vec3 projectPointOnVectorPlane(Vec3 direction, Vec2 point) {
		Vec3 v = direction.scale(point.x);
		return new Vec3(v.x, point.y, v.z);
	}
}
