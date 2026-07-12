package model;

public record Building(
        long buildingId,     //PF       数据库层
       String buildingCode,    //编号 业务层
        String buildingName,    //宿舍楼名
        BuildingGenderPolicy genderPolicy       //宿舍楼句句人员性别
) {
}

