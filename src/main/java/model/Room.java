package model;

/**
 * 宿舍房间领域对象，对应 rooms 表的一行记录。
 * <p>
 * 每个房间属于某一栋宿舍楼，并在同一楼内具有唯一的房间号。
 */
public record Room(
        long roomId,        // 房间自增主键，数据库生成。
        String roomNumber,  // 房间业务编号，同一栋楼内唯一。
        long buildingId,    // 所属宿舍楼 ID，外键关联 buildings 表。
        int floorNumber     // 楼层号，必须大于等于 1。
) {
}
