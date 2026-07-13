package model;

/**
 * 学生领域对象，对应 students 表的一行记录。
 * <p>
 * 学号同时作为业务标识和数据库主键。
 */
public record Student(
        String studentId,   // 学生学号，主键，业务上唯一标识一名学生。
        String studentName, // 学生姓名。
        String className,   // 学生班级。
        String grade,       // 学生年级。
        Gender gender       // 学生性别。
) {
}
