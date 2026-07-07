package model;

public record DormAssignment(
        long assignmentId, //分配记录 PK
        String studentId,       //FK
        long buildingId,        //FK
        long roomId,            //FK
        int bedNumber  //床号
) {
}

