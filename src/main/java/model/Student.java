package model;

public record Student(
        String studentId,       //学号
        String studentName,     //学生姓名
        String className,       //学生班级
        String grade,   //学生年级
        Gender gender   //学生性别
) {
}

