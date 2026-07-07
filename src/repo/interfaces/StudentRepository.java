package repo.interfaces;

import model.Student;
import model.StudentDormView;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface StudentRepository {
    Student save(Student student) throws SQLException;

    List<Student> findAll() throws SQLException;

    Optional<Student> findById(String studentId) throws SQLException;

    List<StudentDormView> findStudentsByRoom(long roomId) throws SQLException;

    Optional<StudentDormView> findDormByStudentId(String studentId) throws SQLException;
}

