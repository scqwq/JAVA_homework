package model;

/**
 * 宿舍分配领域对象，对应 dorm_assignments 表的一行记录。
 * <p>
 * 记录学生与宿舍楼、房间、床号的对应关系。
 */
public record DormAssignment(
        long assignmentId,  // 分配记录自增主键，数据库生成。
        String studentId,   // 学生学号，外键关联 students 表。
        long buildingId,    // 宿舍楼 ID，外键关联 buildings 表。
        long roomId,        // 房间 ID，外键关联 rooms 表。
        int bedNumber       // 床号，取值范围为 1 到 4。
) {
}
