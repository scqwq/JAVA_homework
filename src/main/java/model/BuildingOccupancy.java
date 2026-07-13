package model;

/**
 * 单个宿舍楼的入住率统计视图。
 *
 * @param buildingId   宿舍楼 ID
 * @param buildingName 宿舍楼名称
 * @param totalRooms   该楼栋下的房间总数
 * @param totalBeds    该楼栋下的总床位数（每间房固定 4 床）
 * @param occupiedBeds 已入住床位数
 * @param occupancyRate 入住率百分比（0.0 ~ 100.0）
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
