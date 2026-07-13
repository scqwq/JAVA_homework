package model;

import java.util.List;

/**
 * 系统整体入住率概览。
 *
 * @param totalRooms    系统房间总数
 * @param totalBeds     系统总床位数（每间房固定 4 床）
 * @param occupiedBeds  已入住床位数
 * @param occupancyRate 整体入住率百分比（0.0 ~ 100.0）
 * @param buildingStats 各楼栋入住率明细
 */
public record OccupancyOverview(
        long totalRooms,
        long totalBeds,
        long occupiedBeds,
        double occupancyRate,
        List<BuildingOccupancy> buildingStats
) {
}
