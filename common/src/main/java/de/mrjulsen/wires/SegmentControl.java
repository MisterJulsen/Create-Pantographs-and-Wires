package de.mrjulsen.wires;

import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import de.mrjulsen.wires.util.ClientUtils;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

public final class SegmentControl {

    public enum SegmentationMode {
        AUTO,
        FIXED,
        MAX_LENGTH,
        CUSTOM,
        CUSTOM_MAX
    }

    /**
     * Internal configuration class for segmentation.
     */
    public static final class Config {
        private final SegmentationMode mode;
        private final int fixedCount;
        private final double maxLength;
        private final double[] customLengths;
        private final boolean useArc;

        private Config(SegmentationMode mode, int fixedCount, double maxLength, double[] customLengths, boolean useArc) {
            this.mode = mode;
            this.fixedCount = fixedCount;
            this.maxLength = maxLength;
            this.customLengths = customLengths;
            this.useArc = useArc;
        }

        /**
         * Creates a configuration for one single segment.
         * @return a Config instance
         */
        public static Config single() {
            return fixed(1);
        }

        /**
         * Creates an automatic configuration.
         * @return a Config instance with automatic segmentation (AUTO)
         */
        public static Config auto() {
            return new Config(SegmentationMode.AUTO, 0, 0f, null, false);
        }

        /**
         * Creates a fixed segmentation configuration.
         * @param fixedCount the fixed number of segments to use. If fixedCount is {@code <= 0}, automatic segmentation is used
         * @return a Config instance with fixed segmentation (FIXED)
         */
        public static Config fixed(int fixedCount) {
            return new Config(SegmentationMode.FIXED, fixedCount, 0f, null, false);
        }

        /**
         * Creates a maximum length segmentation configuration.
         * @param maxLength the maximum allowed length for each segment. Must be a positive float. The wire is segmented into as many parts as necessary so that no segment exceeds maxLength.
         * @return a Config instance with maximum length segmentation (MAX_LENGTH)
         */
        public static Config maxLength(double maxLength) {
            return new Config(SegmentationMode.MAX_LENGTH, 0, maxLength, null, false);
        }

        /**
         * Creates a custom segmentation configuration using a provided array of segment lengths.
         * @param customLengths an array of floats for desired segment lengths. customLengths is an array of floats representing the desired segment lengths.
         * The number of segments equals the length of the array. The cumulative sum is truncated if it would exceed the cable length.
         * If the sum is less than the cable length, an extra segment is added.
         * @return a Config instance with custom segmentation (CUSTOM)
         */
        public static Config custom(double[] customLengths, boolean useArc) {
            return new Config(SegmentationMode.CUSTOM, 0, 0f, customLengths, useArc);
        }

        /**
         * Creates a custom segmentation configuration with maximum length constraints.
         * @param customLengths an array of floats for desired segment lengths. customLengths is an array of floats and maxLength is a positive float.
         * Each custom value is capped at maxLength; if a custom segment is longer than maxLength,
         * it is subdivided evenly. The cumulative sum is truncated if it would exceed the cable length.
         * If the sum is less than the cable length, an extra segment is added.
         * @param maxLength the maximum allowed length for each segment. 
         * @return a Config instance with custom segmentation and maximum length constraints (CUSTOM_MAX)
         */
        public static Config customMax(double[] customLengths, double maxLength, boolean useArc) {
            return new Config(SegmentationMode.CUSTOM_MAX, 0, maxLength, customLengths, useArc);
        }

        public SegmentationMode getMode() {
            return mode;
        }

        public int getFixedCount() {
            return fixedCount;
        }

        public double getMaxLength() {
            return maxLength;
        }

        public double[] getCustomLengths() {
            return customLengths;
        }

        public boolean useArc() {
            return useArc;
        }
    }

    private final Config mainConfig;
    private final Config subConfig;

    private SegmentControl(Config mainConfig, Config subConfig) {
        this.mainConfig = mainConfig;
        this.subConfig = subConfig;
    }

    /**
     * Creates a SegmentControl with automatic segmentation for the wire.
     * @return the SegmentControl instance
     */
    public static SegmentControl createAuto() {
        return new SegmentControl(Config.auto(), Config.single());
    }

    /**
     * Creates a SegmentControl with one single segment for the wire.
     * <p>
     * Equivalent to {@code SegmentControl.fixed(1)}.
     * </p>
     * @return the SegmentControl instance
     */
    public static SegmentControl single() {
        return new SegmentControl(Config.single(), Config.single());
    }

    /**
     * Creates a SegmentControl with a fixed amount of segments for the wire.
     * @param fixedCount the fixed number of wire segments. If {@code <= 0}, AUTO mode is used.
     * @return the SegmentControl instance
     */
    public static SegmentControl fixed(int fixedCount) {
        return new SegmentControl(Config.fixed(fixedCount), Config.single());
    }

    // TODO Gleichmäßige Verteilung? ja oder nein
    /**
     * Creates a SegmentControl with any number of segments required for the wire with an individual length that does not exceed the maximum value.
     * @param maxLength the maximum allowed length for each segment. Must be {@code > 0}.
     * @return the SegmentControl instance
     */
    public static SegmentControl maxLength(double maxLength) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("Max Segment Length cannot be less or equal 0.");
        }
        return new SegmentControl(Config.maxLength(maxLength), Config.single());
    }

    /**
     * Creates a SegmentControl with custom lengths for each segment of the wire.
     * <p>The amount of values in the array is the amount of segments used in the wire. If the sum of all lengths is less than the max wire length,
     * the remaining space will be filled with one new segment. If the sum of all lengths is greater than the max wire length, then the last segment
     * within the length of the cable is cut off at the end of the wire and all following values ​​are ignored.</p>
     * @param customLengths an array of floats for the length of each wire segment. The amount of values in the array is the amount of segments for the wire.
     * @return the SegmentControl instance
     */
    public static SegmentControl custom(double[] customLengths, boolean useArc) {
        return new SegmentControl(Config.custom(customLengths, useArc), Config.single());
    }

    /**
     * Creates a SegmentControl with custom lengths for each segment of the wire.
     * <p>The amount of values in the array is the amount of segments used in the wire. If the sum of all lengths is less than the max wire length,
     * the remaining space will be filled with one new segment. If the sum of all lengths is greater than the max wire length, then the last segment
     * within the length of the cable is cut off at the end of the wire and all following values ​​are ignored.</p>
     * <p>If a segment exceeds the maximum value, the segment is divided into several smaller segments until each of these parts no longer exceeds the maximum
     * value. This behavior also applies to the segment that fills the remaining space if the sum of all lengths is smaller than the cable length.</p>
     * @param customLengths an array of floats for the length of each wire segment. The amount of values in the array is the amount of segments for the wire.
     * @param maxLength the maximum allowed length for each segment
     * @return the SegmentControl instance
     */
    public static SegmentControl customMax(double[] customLengths, double maxLength, boolean useArc) {
        return new SegmentControl(Config.customMax(customLengths, maxLength, useArc), Config.single());
    }

    /**
     * Creates a SegmentControl with a custom configuration for the wire segments.
     * <p>Refer to {@link Config} for allowed configurations.</p>
     * @param config a Config instance for segmentation
     * @return the SegmentControl instance
     */
    public static SegmentControl configured(Config config) {
        return new SegmentControl(config, Config.single());
    }

    /**
     * Creates a SegmentControl with a custom configuration for the wire segments and sub-segments.
     * <p>Sub-segments are only relevant for rendering and divide the segments into smaller segments without changing the geometry, collision or shape of the wires. The reason for this is that segments (especially large segments) may become invisible because of culling.</p>
     * <p>Refer to {@link Config} for allowed configurations. In all other factory methods {@code Config.single()} is used by default, which doesn't devie the segments any further.</p>
     * @param mainConfig a Config instance for default segmentation
     * @param subConfig a Config instance for sub-segmentation
     * @return the SegmentControl instance
     */
    public static SegmentControl create(Config mainConfig, Config subConfig) {
        return new SegmentControl(mainConfig, subConfig);
    }

    int computeSegmentCount(Vector3d start, Vector3d end, WireBuilder.CableType type, double hangFac, boolean rendering) {
        Vector3d diff = new Vector3d(end).sub(start);
        double linearLength = diff.length();
        if (type == WireBuilder.CableType.TIGHT) {
            if (mainConfig.mode == SegmentationMode.CUSTOM || mainConfig.mode == SegmentationMode.CUSTOM_MAX) {
                return getCustomSegmentCount(linearLength, mainConfig.getCustomLengths(), mainConfig.getMaxLength(), mainConfig.mode == SegmentationMode.CUSTOM_MAX);
            }
            switch (mainConfig.mode) {
                case FIXED:
                    return mainConfig.fixedCount > 0 ? mainConfig.fixedCount : Math.max(1, (int) linearLength);
                case MAX_LENGTH:
                    return Math.max(1, (int) Math.ceil(linearLength / mainConfig.maxLength));
                case AUTO:
                default:
                    return Math.max(1, (int) linearLength);
            }
        } else {
            // For HANGING and TENSION, use arc length.
            Vector2d p1 = new Vector2d(0, 0);
            Vector2d p2 = new Vector2d(linearLength / 2f, diff.y / 2f - Math.min(hangFac, linearLength / 2f));
            Vector2d p3 = new Vector2d(linearLength, diff.y);
            Vector2d center = circumcenter(p1, p2, p3);
            double rad = radius(center, p1);
            double arcLen = arcLength(center, p1, p3, rad);
            if (mainConfig.mode == SegmentationMode.CUSTOM || mainConfig.mode == SegmentationMode.CUSTOM_MAX) {
                return getCustomSegmentCount(arcLen, mainConfig.getCustomLengths(), mainConfig.getMaxLength(), mainConfig.mode == SegmentationMode.CUSTOM_MAX);
            }
            switch (mainConfig.mode) {
                case FIXED:
                    return mainConfig.fixedCount > 0 ? mainConfig.fixedCount : getCustomSegmentCount(arcLen, mainConfig.getCustomLengths(), mainConfig.getMaxLength(), false);
                case MAX_LENGTH:
                    return Math.max(1, (int) Math.ceil(arcLen / mainConfig.maxLength));
                case AUTO:
                default:
                    return (int) Math.max(Math.ceil(arcLen * 16 / rad), linearLength);
            }
        }
    }

    int computeSubSegmentCount(double segmentLength) {
        if (subConfig.mode == SegmentationMode.CUSTOM || subConfig.mode == SegmentationMode.CUSTOM_MAX) {
            return getCustomSegmentCount(segmentLength, subConfig.getCustomLengths(), subConfig.getMaxLength(), subConfig.mode == SegmentationMode.CUSTOM_MAX);
        }
        switch (subConfig.mode) {
            case FIXED:
                return subConfig.fixedCount > 0 ? subConfig.fixedCount : Math.max(1, (int) segmentLength);
            case MAX_LENGTH:
                return Math.max(1, (int) Math.ceil(segmentLength / subConfig.maxLength));
            case AUTO:
            default:
                double multiplier = 1f;
                if (Platform.getEnvironment() == Env.CLIENT) {
                    multiplier = ClientUtils.getMultiplierByGraphicsMode();
                }
                return Math.max(1, (int) (segmentLength * multiplier));
        }
    }
    
    private int getCustomSegmentCount(double totalLength, double[] custom, double maxLength, boolean useMax) {
        if (custom == null || custom.length == 0) {
            return 1;
        }
        int count = 0;
        double cum = 0f;
        for (double len : custom) {
            if (cum >= totalLength) break;
            if (useMax && len > maxLength) {
                int n = (int) Math.ceil(len / maxLength);
                double subLen = len / n;
                for (int i = 0; i < n; i++) {
                    if (cum + subLen > totalLength) {
                        count++;
                        cum = totalLength;
                        break;
                    } else {
                        count++;
                        cum += subLen;
                    }
                }
            } else {
                if (cum + len > totalLength) {
                    count++;
                    cum = totalLength;
                    break;
                } else {
                    count++;
                    cum += len;
                }
            }
        }
        if (cum < totalLength) {
            double fill = totalLength - cum;
            if (useMax && fill > maxLength) {
                count += (int) Math.ceil(fill / maxLength);
            } else {
                count++;
            }
        }
        return Math.max(1, count);
    }

    // Static helper methods for arc computations

    static Vector2d circumcenter(Vector2d a, Vector2d b, Vector2d c) {
        double d = 2 * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y));
        if (Math.abs(d) < 1e-6f) return new Vector2d(0, 0);
        double ax2ay2 = a.x * a.x + a.y * a.y;
        double bx2by2 = b.x * b.x + b.y * b.y;
        double cx2cy2 = c.x * c.x + c.y * c.y;
        double ux = (ax2ay2 * (b.y - c.y) + bx2by2 * (c.y - a.y) + cx2cy2 * (a.y - b.y)) / d;
        double uy = (ax2ay2 * (c.x - b.x) + bx2by2 * (a.x - c.x) + cx2cy2 * (b.x - a.x)) / d;
        return new Vector2d(ux, uy);
    }

    static double radius(Vector2d center, Vector2d p) {
        return Math.hypot(center.x - p.x, center.y - p.y);
    }

    static double arcLength(Vector2d center, Vector2d a, Vector2d b, double radius) {
        double dot = (a.x - center.x) * (b.x - center.x) + (a.y - center.y) * (b.y - center.y);
        double angle = (float) Math.acos(dot / (radius * radius));
        return radius * angle;
    }

    static Vector2d[] equallyDistributedPointsOnArc(Vector2d center, Vector2d start, Vector2d end, double radius, int segments) {
        Vector2d[] points = new Vector2d[segments];
        double startAngle = Math.atan2(start.y - center.y, start.x - center.x);
        double endAngle = Math.atan2(end.y - center.y, end.x - center.x);
        if (endAngle < startAngle) {
            endAngle += 2 * Math.PI;
        }
        double angleStep = (endAngle - startAngle) / (segments - 1);
        for (int i = 0; i < segments; i++) {
            double angle = startAngle + i * angleStep;
            double x = (center.x + radius * Math.cos(angle));
            double y = (center.y + radius * Math.sin(angle));
            points[i] = new Vector2d(x, y);
        }
        return points;
    }

    static Vector2d[] equallyDistributedPointsOnX(Vector2d center, Vector2d start, Vector2d end, double radius, int segments) {
        Vector2d[] points = new Vector2d[segments];
        double startX = start.x;
        double endX = end.x;
        double stepX = (endX - startX) / (segments - 1);
        for (int i = 0; i < segments; i++) {
            double x = startX + i * stepX;
            double dx = x - center.x;
            double dySquared = radius * radius - dx * dx;
            double y;
            if (dySquared >= 0) {
                double dy = Math.sqrt(dySquared);
                double candidateY = center.y + dy;
                double candidateY2 = center.y - dy;
                double startAngle = Math.atan2(start.y - center.y, start.x - center.x);
                double candidateAngle = Math.atan2(candidateY - center.y, x - center.x);
                y = (candidateAngle >= startAngle && candidateAngle <= Math.atan2(end.y - center.y, end.x - center.x))
                        ? candidateY : candidateY2;
            } else {
                y = center.y;
            }
            points[i] = new Vector2d(x, y);
        }
        return points;
    }

    // Getters for main configuration values (used in WireBuilder custom path generation)
    SegmentationMode getMainMode() {
        return mainConfig.mode;
    }

    double[] getMainCustomLengths() {
        return mainConfig.customLengths;
    }

    double getMainMaxLength() {
        return mainConfig.maxLength;
    }

    boolean getMainUseArc() {
        return mainConfig.useArc;
    }
}
