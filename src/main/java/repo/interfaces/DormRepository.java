package repo.interfaces;

import model.DormAssignment;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 宿舍分配的数据访问约定，专门供 {@code DormService} 编排入住和调宿流程。
 * <p>
 * 本接口只保存、更新和查询 dorm_assignments 表；学生性别、房间归属和床号范围等规则必须由 DormService 在调用前校验。
 */
public interface DormRepository {
    /**
     * 给 DormService 写入一条已完成校验的入住分配。
     * 返回值带有数据库生成的 assignmentId，业务层可据此继续处理但无需了解 JDBC 细节。
     */
    DormAssignment save(DormAssignment dormAssignment) throws SQLException;

    /**
     * 给 DormService 调整既有学生的目标楼、房间和床位。
     * 本方法按学生更新既有记录，不负责确认学生是否已有分配或目标床位能否使用。
     */
    DormAssignment update(DormAssignment dormAssignment) throws SQLException;

    /**
     * 给 DormService 判断学生当前是否已经有入住分配。
     * Optional.empty() 是“未分配”的正常业务状态，不是数据库异常。
     */
    Optional<DormAssignment> findByStudentId(String studentId) throws SQLException;

    /**
     * 给宿舍查询与调试场景按房间读取分配记录。
     * 返回列表应按床号排列，方便上层直接理解一个房间的床位占用情况。
     */
    List<DormAssignment> findByRoomId(long roomId) throws SQLException;

    /**
     * 给 DormService 在分配或调宿前检查目标床位是否被占用。
     * 调宿时传入当前学生学号即可排除其原记录，避免把自己已有的床位误判为冲突。
     */
    boolean existsByRoomIdAndBedNumber(long roomId, int bedNumber, String excludeStudentId) throws SQLException;

    /**
     * 统计系统内所有已分配床位的数量，用于首页入住率概览。
     */
    long countAllAssignments() throws SQLException;

    /**
     * 按宿舍楼分组统计已分配床位的数量，用于首页各楼栋入住率明细。
     */
    Map<Long, Long> countAssignmentsByBuilding() throws SQLException;
}

