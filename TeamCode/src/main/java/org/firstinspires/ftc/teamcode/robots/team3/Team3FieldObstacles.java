package org.firstinspires.ftc.teamcode.robots.team3;

import java.util.ArrayList;
import java.util.List;

/**
 * Stationary floor-level obstacle map, corner-waypoint route planner, and real-time repulsive
 * potential field for the BIOBUZZ 144x144 inch field.
 *
 * <p>Uses the exact floor-level footprints from {@code path.pp.json} and the official BIOBUZZ CAD:
 * <ul>
 *   <li>Only parts between 0" and 18" off the floor that the robot chassis can physically hit are
 *       modeled as obstacles (specifically the two thin 2.6" x 41.4" HIVE floor frame bases/legs
 *       and the four wall-mounted FLOWERs, plus the four perimeter walls).</li>
 *   <li>The overhanging HIVE cells/baskets suspended 36" to 56" in the air are above the 18"
 *       robot height limit and are NOT treated as floor obstacles.</li>
 * </ul>
 * </p>
 */
public final class Team3FieldObstacles {
    /** Official FTC field width and length in inches. */
    public static final double FIELD_SIZE_INCHES = 144.0;

    /** Half of Team 3's 16x16 inch robot footprint (from path.pp.json). */
    public static final double ROBOT_HALF_SIZE_INCHES = 8.0;
    /** Extra clearance buffer beyond the robot bumper perimeter (from path.pp.json). */
    public static final double SAFETY_MARGIN_INCHES = 2.0;
    /** Total keep-out distance from the robot center to any raw obstacle edge or field wall. */
    public static final double CLEARANCE_RADIUS_INCHES = ROBOT_HALF_SIZE_INCHES + SAFETY_MARGIN_INCHES;

    /** Extra corner padding when routing a waypoint around an inflated obstacle corner. */
    private static final double WAYPOINT_CORNER_PAD_INCHES = 1.5;

    /** Soft boundary zone outside the clearance radius where real-time repulsion gently pushes away. */
    public static final double REPULSION_BUFFER_INCHES = 3.5;
    /** Maximum repulsive power contribution when brushing against an obstacle safety boundary. */
    public static final double MAX_REPULSION_POWER = 0.28;

    /** Immutable axis-aligned floor obstacle bounding box in [0, 144] field coordinates. */
    public static final class ObstacleBox {
        private final String name;
        private final double minX;
        private final double maxX;
        private final double minY;
        private final double maxY;

        public ObstacleBox(String name, double minX, double maxX, double minY, double maxY) {
            this.name = name;
            this.minX = Math.min(minX, maxX);
            this.maxX = Math.max(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.maxY = Math.max(minY, maxY);
        }

        public String getName() { return name; }
        public double getMinX() { return minX; }
        public double getMaxX() { return maxX; }
        public double getMinY() { return minY; }
        public double getMaxY() { return maxY; }
    }

    /**
     * All 6 stationary floor-level obstacles from the official BIOBUZZ CAD (biobuzz-2026.json
     * and path.pp.json, in [0, 144] field coordinates).
     */
    private static final ObstacleBox[] FLOOR_OBSTACLES = {
            // 1. Red HIVE Floor Frame Base & A-Frame Legs (symmetric CAD floor rail & feet)
            new ObstacleBox("Red HIVE Base", 47.64, 49.23, 53.13, 90.87),
            // 2. Blue HIVE Floor Frame Base & A-Frame Legs (symmetric CAD floor rail & feet)
            new ObstacleBox("Blue HIVE Base", 94.77, 96.36, 53.13, 90.87),
            // 3. Scoring-Side FLOWER (Top Wall)
            new ObstacleBox("Scoring FLOWER", 45.48, 51.74, 136.91, 144.0),
            // 4. Blue Alliance FLOWER (Right Wall)
            new ObstacleBox("Blue FLOWER", 136.91, 144.0, 92.26, 98.52),
            // 5. Audience-Side FLOWER (Bottom Wall)
            new ObstacleBox("Audience FLOWER", 92.26, 98.52, 0.0, 7.09),
            // 6. Red Alliance FLOWER (Left Wall)
            new ObstacleBox("Red FLOWER", 0.0, 7.09, 45.48, 51.74)
    };

    private Team3FieldObstacles() {
        // Utility class
    }

    /**
     * Clamps a target field coordinate {@code (x, y)} so the robot center stays at least
     * {@link #CLEARANCE_RADIUS_INCHES} away from all 4 perimeter walls and outside all 6
     * floor obstacles.
     *
     * @param x desired field X in inches
     * @param y desired field Y in inches
     * @return 2-element array {@code [safeX, safeY]}
     */
    public static double[] clampToSafeFieldPoint(double x, double y) {
        double minWall = CLEARANCE_RADIUS_INCHES;
        double maxWall = FIELD_SIZE_INCHES - CLEARANCE_RADIUS_INCHES;
        double safeX = Math.max(minWall, Math.min(maxWall, x));
        double safeY = Math.max(minWall, Math.min(maxWall, y));

        for (ObstacleBox box : FLOOR_OBSTACLES) {
            double infMinX = box.minX - CLEARANCE_RADIUS_INCHES;
            double infMaxX = box.maxX + CLEARANCE_RADIUS_INCHES;
            double infMinY = box.minY - CLEARANCE_RADIUS_INCHES;
            double infMaxY = box.maxY + CLEARANCE_RADIUS_INCHES;

            if (safeX > infMinX && safeX < infMaxX && safeY > infMinY && safeY < infMaxY) {
                // Push out to the nearest face of the inflated obstacle box
                double dLeft = safeX - infMinX;
                double dRight = infMaxX - safeX;
                double dBottom = safeY - infMinY;
                double dTop = infMaxY - safeY;
                double minDist = Math.min(Math.min(dLeft, dRight), Math.min(dBottom, dTop));

                if (minDist == dLeft && infMinX >= minWall) {
                    safeX = infMinX;
                } else if (minDist == dRight && infMaxX <= maxWall) {
                    safeX = infMaxX;
                } else if (minDist == dBottom && infMinY >= minWall) {
                    safeY = infMinY;
                } else if (infMaxY <= maxWall) {
                    safeY = infMaxY;
                }
            }
        }

        safeX = Math.max(minWall, Math.min(maxWall, safeX));
        safeY = Math.max(minWall, Math.min(maxWall, safeY));
        return new double[]{safeX, safeY};
    }

    /**
     * Plans a collision-free sequence of 2D field waypoints from {@code (startX, startY)} to
     * {@code (targetX, targetY)} that avoids all 6 BIOBUZZ floor obstacles and the 4 walls.
     *
     * <p>If the straight-line path is clear, returns a single-element list containing only the
     * safe target point. If a stationary obstacle (such as a HIVE floor frame base/leg) lies
     * between the robot and the target spot, selects the shortest collision-free 1-corner or
     * 2-corner detour around the obstacle's safety perimeter.</p>
     *
     * @param startX current robot field X in inches
     * @param startY current robot field Y in inches
     * @param targetX desired shooting spot field X in inches
     * @param targetY desired shooting spot field Y in inches
     * @return ordered list of {@code [fieldX, fieldY]} waypoints ending at the safe target spot
     */
    public static List<double[]> planSafeFieldRoute(double startX, double startY,
                                                    double targetX, double targetY) {
        double[] safeTarget = clampToSafeFieldPoint(targetX, targetY);
        double goalX = safeTarget[0];
        double goalY = safeTarget[1];

        List<double[]> route = new ArrayList<>();
        if (isSegmentClear(startX, startY, goalX, goalY)) {
            route.add(new double[]{goalX, goalY});
            return route;
        }

        // Generate candidate corner waypoints around all inflated obstacles
        List<double[]> candidates = buildCornerCandidates();

        // 1. Search for the shortest 1-waypoint detour: Start -> WP -> Goal
        double bestOneHopCost = Double.POSITIVE_INFINITY;
        double[] bestWp1 = null;

        for (double[] wp : candidates) {
            if (isSegmentClear(startX, startY, wp[0], wp[1])
                    && isSegmentClear(wp[0], wp[1], goalX, goalY)) {
                double cost = Math.hypot(wp[0] - startX, wp[1] - startY)
                        + Math.hypot(goalX - wp[0], goalY - wp[1]);
                if (cost < bestOneHopCost) {
                    bestOneHopCost = cost;
                    bestWp1 = wp;
                }
            }
        }

        if (bestWp1 != null) {
            route.add(bestWp1);
            route.add(new double[]{goalX, goalY});
            return route;
        }

        // 2. Search for the shortest 2-waypoint detour: Start -> WP_A -> WP_B -> Goal
        double bestTwoHopCost = Double.POSITIVE_INFINITY;
        double[] bestTwoHopA = null;
        double[] bestTwoHopB = null;

        for (int i = 0; i < candidates.size(); i++) {
            double[] wpA = candidates.get(i);
            if (!isSegmentClear(startX, startY, wpA[0], wpA[1])) {
                continue;
            }
            double distStartToA = Math.hypot(wpA[0] - startX, wpA[1] - startY);
            if (distStartToA >= bestTwoHopCost) {
                continue;
            }
            for (int j = 0; j < candidates.size(); j++) {
                if (i == j) {
                    continue;
                }
                double[] wpB = candidates.get(j);
                double totalCost = distStartToA
                        + Math.hypot(wpB[0] - wpA[0], wpB[1] - wpA[1])
                        + Math.hypot(goalX - wpB[0], goalY - wpB[1]);
                if (totalCost < bestTwoHopCost
                        && isSegmentClear(wpA[0], wpA[1], wpB[0], wpB[1])
                        && isSegmentClear(wpB[0], wpB[1], goalX, goalY)) {
                    bestTwoHopCost = totalCost;
                    bestTwoHopA = wpA;
                    bestTwoHopB = wpB;
                }
            }
        }

        if (bestTwoHopA != null && bestTwoHopB != null) {
            route.add(bestTwoHopA);
            route.add(bestTwoHopB);
        }
        route.add(new double[]{goalX, goalY});
        return route;
    }

    /**
     * Computes a smooth repulsive potential-field correction vector {@code [repulseFieldX, repulseFieldY]}
     * in the [0, 144] field frame when the robot approaches within {@link #REPULSION_BUFFER_INCHES}
     * of any inflated obstacle or perimeter wall.
     */
    public static double[] computeRepulsionFieldVector(double robotX, double robotY) {
        double repulseX = 0.0;
        double repulseY = 0.0;
        double influenceRadius = CLEARANCE_RADIUS_INCHES + REPULSION_BUFFER_INCHES;

        // 1. Repulsion from each of the 6 floor obstacles
        for (ObstacleBox box : FLOOR_OBSTACLES) {
            // Nearest point on raw obstacle box to robot center
            double nearestX = Math.max(box.minX, Math.min(box.maxX, robotX));
            double nearestY = Math.max(box.minY, Math.min(box.maxY, robotY));
            double dx = robotX - nearestX;
            double dy = robotY - nearestY;
            double dist = Math.hypot(dx, dy);

            if (dist < influenceRadius) {
                double penetration = (influenceRadius - dist) / REPULSION_BUFFER_INCHES;
                double mag = Math.min(MAX_REPULSION_POWER, MAX_REPULSION_POWER * penetration);
                if (dist > 0.001) {
                    repulseX += mag * (dx / dist);
                    repulseY += mag * (dy / dist);
                } else {
                    // Fallback if exactly on raw edge: push away from box center
                    double cx = 0.5 * (box.minX + box.maxX);
                    double cy = 0.5 * (box.minY + box.maxY);
                    double cdx = robotX - cx;
                    double cdy = robotY - cy;
                    double cdist = Math.max(0.001, Math.hypot(cdx, cdy));
                    repulseX += mag * (cdx / cdist);
                    repulseY += mag * (cdy / cdist);
                }
            }
        }

        // 2. Repulsion from the 4 perimeter walls
        if (robotX < influenceRadius) {
            double pen = (influenceRadius - robotX) / REPULSION_BUFFER_INCHES;
            repulseX += Math.min(MAX_REPULSION_POWER, MAX_REPULSION_POWER * pen);
        } else if (robotX > FIELD_SIZE_INCHES - influenceRadius) {
            double pen = (robotX - (FIELD_SIZE_INCHES - influenceRadius)) / REPULSION_BUFFER_INCHES;
            repulseX -= Math.min(MAX_REPULSION_POWER, MAX_REPULSION_POWER * pen);
        }

        if (robotY < influenceRadius) {
            double pen = (influenceRadius - robotY) / REPULSION_BUFFER_INCHES;
            repulseY += Math.min(MAX_REPULSION_POWER, MAX_REPULSION_POWER * pen);
        } else if (robotY > FIELD_SIZE_INCHES - influenceRadius) {
            double pen = (robotY - (FIELD_SIZE_INCHES - influenceRadius)) / REPULSION_BUFFER_INCHES;
            repulseY -= Math.min(MAX_REPULSION_POWER, MAX_REPULSION_POWER * pen);
        }

        // Clamp combined repulsion magnitude
        double totalMag = Math.hypot(repulseX, repulseY);
        if (totalMag > MAX_REPULSION_POWER) {
            repulseX = (repulseX / totalMag) * MAX_REPULSION_POWER;
            repulseY = (repulseY / totalMag) * MAX_REPULSION_POWER;
        }
        return new double[]{repulseX, repulseY};
    }

    private static List<double[]> buildCornerCandidates() {
        List<double[]> list = new ArrayList<>();
        double pad = CLEARANCE_RADIUS_INCHES + WAYPOINT_CORNER_PAD_INCHES;
        double minWall = CLEARANCE_RADIUS_INCHES;
        double maxWall = FIELD_SIZE_INCHES - CLEARANCE_RADIUS_INCHES;

        for (ObstacleBox box : FLOOR_OBSTACLES) {
            double[] xs = {box.minX - pad, box.maxX + pad};
            double[] ys = {box.minY - pad, box.maxY + pad};
            for (double cx : xs) {
                for (double cy : ys) {
                    if (cx >= minWall && cx <= maxWall && cy >= minWall && cy <= maxWall
                            && !isPointInsideAnyInflatedObstacle(cx, cy)) {
                        list.add(new double[]{cx, cy});
                    }
                }
            }
        }
        return list;
    }

    private static boolean isPointInsideAnyInflatedObstacle(double x, double y) {
        for (ObstacleBox box : FLOOR_OBSTACLES) {
            if (x > box.minX - CLEARANCE_RADIUS_INCHES
                    && x < box.maxX + CLEARANCE_RADIUS_INCHES
                    && y > box.minY - CLEARANCE_RADIUS_INCHES
                    && y < box.maxY + CLEARANCE_RADIUS_INCHES) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the line segment {@code (x1, y1) -> (x2, y2)} avoids all inflated obstacles.
     */
    private static boolean isSegmentClear(double x1, double y1, double x2, double y2) {
        for (ObstacleBox box : FLOOR_OBSTACLES) {
            double minX = box.minX - CLEARANCE_RADIUS_INCHES;
            double maxX = box.maxX + CLEARANCE_RADIUS_INCHES;
            double minY = box.minY - CLEARANCE_RADIUS_INCHES;
            double maxY = box.maxY + CLEARANCE_RADIUS_INCHES;

            // If start point is already slightly inside the inflated margin, only block if the
            // segment intersects the raw obstacle + half-robot core
            boolean startInsideInflated = (x1 > minX && x1 < maxX && y1 > minY && y1 < maxY);
            if (startInsideInflated) {
                double coreMargin = ROBOT_HALF_SIZE_INCHES;
                if (segmentIntersectsBox(x1, y1, x2, y2,
                        box.minX - coreMargin, box.maxX + coreMargin,
                        box.minY - coreMargin, box.maxY + coreMargin)) {
                    return false;
                }
            } else if (segmentIntersectsBox(x1, y1, x2, y2, minX, maxX, minY, maxY)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Exact Liang-Barsky slab intersection test between segment {@code (x1,y1)-(x2,y2)} and AABB.
     */
    private static boolean segmentIntersectsBox(double x1, double y1, double x2, double y2,
                                                double minX, double maxX, double minY, double maxY) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double tMin = 0.0;
        double tMax = 1.0;

        if (Math.abs(dx) < 1e-9) {
            if (x1 <= minX || x1 >= maxX) {
                return false;
            }
        } else {
            double invDx = 1.0 / dx;
            double t1 = (minX - x1) * invDx;
            double t2 = (maxX - x1) * invDx;
            if (t1 > t2) {
                double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return false;
            }
        }

        if (Math.abs(dy) < 1e-9) {
            if (y1 <= minY || y1 >= maxY) {
                return false;
            }
        } else {
            double invDy = 1.0 / dy;
            double t1 = (minY - y1) * invDy;
            double t2 = (maxY - y1) * invDy;
            if (t1 > t2) {
                double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return false;
            }
        }

        return tMin <= tMax;
    }
}
