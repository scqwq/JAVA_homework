package model;

/**
 * 学生宿舍视图对象，用于跨表查询后一次性展示学生及其宿舍信息。
 * <p>
 * 该 record 不对应单张表，而是 students、buildings、rooms、dorm_assignments 的关联结果。
 */
public record StudentDormView(
        String studentId,   // 学生学号。
        String studentName, // 学生姓名。
        String className,   // 学生班级。
        String grade,       // 学生年级。
        Gender gender,      // 学生性别。
        String buildingCode, // 宿舍楼业务编号。
        String buildingName, // 宿舍楼显示名称。
        String roomNumber,   // 房间号。
        Integer bedNumber    // 床号；若学生暂未分配宿舍则为 null。
) {
}
