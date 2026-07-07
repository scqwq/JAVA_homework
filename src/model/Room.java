package model;

public record Room(
        long roomId,  //PF 房间id
        String roomNumber,        //房间号
        long buildingId,  //所属宿舍楼 FK
        int floorNumber                 //楼层
) {
}

