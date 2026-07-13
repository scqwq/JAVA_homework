package model;

/**
 * 单个宿舍楼的入住率统计视图。
 */
public record BuildingOccupancy(
        long buildingId,
        String buildingName,
        long totalRooms,
        long totalBeds,
        long occupiedBeds,
        double occupancyRate
) {
}
