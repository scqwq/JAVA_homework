package model;
//表查询后的完整展示数据 便于业务查询展示使用

public record StudentDormView(
        String studentId, //学号
        String studentName, 
        String className,  //班级
        String grade,
        Gender gender,
        String buildingCode, //宿舍楼编号
        String buildingName,  //宿舍楼名称
        String roomNumber,      //宿舍房间号
        Integer bedNumber       //床号
) {
}

