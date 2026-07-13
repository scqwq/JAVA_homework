package model;

import java.util.List;

/**
 * 系统整体入住率概览。
 */
public record OccupancyOverview(
        long totalRooms,
        long totalBeds,
        long occupiedBeds,
        double occupancyRate,
        List<BuildingOccupancy> buildingStats
) {
}
