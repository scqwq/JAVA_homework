package model;

/**
 * 宿舍楼领域对象，对应 buildings 表的一行记录。
 * <p>
 * 使用 Java record 不可变地保存宿舍楼标识、编号、名称及性别策略。
 */
public record Building(
        long buildingId,        // 宿舍楼自增主键，数据库生成。
        String buildingCode,    // 宿舍楼业务编号，全局唯一。
        String buildingName,    // 宿舍楼显示名称。
        BuildingGenderPolicy genderPolicy   // 该楼允许的入住性别规则。
) {
}
