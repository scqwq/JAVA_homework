package repo.interfaces;

import model.DormAssignment;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

//宿舍分配
public interface DormRepository {
    DormAssignment save(DormAssignment dormAssignment) throws SQLException;

    DormAssignment update(DormAssignment dormAssignment) throws SQLException;

    Optional<DormAssignment> findByStudentId(String studentId) throws SQLException;

    List<DormAssignment> findByRoomId(long roomId) throws SQLException;

    boolean existsByRoomIdAndBedNumber(long roomId, int bedNumber, String excludeStudentId) throws SQLException;
}

