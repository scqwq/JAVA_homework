package model;

//学生和床号关系表
public record DormAssignment(
        long assignmentId, //分配记录 PK
        String studentId,       //FK
        long buildingId,        //FK
        long roomId,            //FK
        int bedNumber  //床号
) {
}

