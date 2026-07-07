package service;

import model.Student;
import model.StudentDormView;
import model.Gender;
import database.DatabaseConnection;

import java.sql.SQLException;
import java.util.List;

public class StudentService {
    private final DatabaseConnection databaseConnection;

    public StudentService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public Student createStudent(String studentId, String name, String className, String grade, String gender) throws SQLException {
        if (studentId.isBlank()) {
            throw new IllegalArgumentException("学号不能为空");
        }
        return databaseConnection.studentRepository().save(
                new Student(studentId, name, className, grade, Gender.fromLabel(gender))
        );
    }

    public List<Student> listStudents() throws SQLException {
        return databaseConnection.studentRepository().findAll();
    }

    public Student getStudent(String studentId) throws SQLException {
        return databaseConnection.studentRepository()
                .findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("未找到学生: " + studentId));
    }

    public StudentDormView getDormByStudentId(String studentId) throws SQLException {
        getStudent(studentId);
        return databaseConnection.studentRepository()
                .findDormByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("未找到学生: " + studentId));
    }

    public List<StudentDormView> getStudentsByRoomId(long roomId) throws SQLException {
        return databaseConnection.studentRepository().findStudentsByRoom(roomId);
    }
}

