package service;

import model.Student;
import model.StudentDormView;
import model.Gender;
import database.DatabaseConnection;

import java.sql.SQLException;
import java.util.List;

/**
 * 学生服务类，负责处理学生资料及学生宿舍视图相关的业务逻辑。
 * <p>
 * 负责学号非空、学号重复等业务校验，并将性别字符串转换为领域枚举。
 */
public class StudentService {
    // 数据库连接入口，用于访问学生仓储。
    private final DatabaseConnection databaseConnection;

    // 通过构造器注入数据库连接。
    public StudentService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    // 新增学生：校验学号非空和唯一性后保存学生资料。
    public Student createStudent(String studentId, String name, String className, String grade, String gender) throws SQLException {
        if (studentId.isBlank()) {
            throw new IllegalArgumentException("学号不能为空");
        }
        databaseConnection.studentRepository().findById(studentId).ifPresent(existing -> {
            throw new IllegalArgumentException("学号已存在: " + studentId);
        });
        return databaseConnection.studentRepository().save(
                new Student(studentId, name, className, grade, Gender.fromLabel(gender))
        );
    }

    // 查询全部学生列表，供界面层展示。
    public List<Student> listStudents() throws SQLException {
        return databaseConnection.studentRepository().findAll();
    }

    // 根据学号查询单个学生；若不存在，则抛出业务异常。
    public Student getStudent(String studentId) throws SQLException {
        return databaseConnection.studentRepository()
                .findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("未找到学生: " + studentId));
    }

    // 根据学号查询学生的宿舍信息；会先确认学生存在。
    public StudentDormView getDormByStudentId(String studentId) throws SQLException {
        getStudent(studentId);
        return databaseConnection.studentRepository()
                .findDormByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("未找到学生: " + studentId));
    }

    // 根据房间 ID 查询入住该房间的学生宿舍视图列表。
    public List<StudentDormView> getStudentsByRoomId(long roomId) throws SQLException {
        return databaseConnection.studentRepository().findStudentsByRoom(roomId);
    }
}
