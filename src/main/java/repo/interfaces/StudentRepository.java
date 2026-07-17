package repo.interfaces;

import model.Student;
import model.StudentDormView;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 学生及学生宿舍视图的数据访问约定，主要服务于 {@code StudentService}，
 * 并由 {@code DormService} 间接复用查询结果。
 * <p>
 * Repository 负责返回学生实体或跨表视图；学号格式、入住资格和用户提示属于 Service/UI 层协作范围。
 */
public interface StudentRepository {
    /**
     * 给 StudentService 保存已通过业务校验的学生资料。
     * 学号由业务层提供且同时是主键，成功后返回同一份可供 UI 回显的学生信息。
     */
    Student save(Student student) throws SQLException;

    /**
     * 给 StudentService 的学生列表页面读取全部学生。
     * 返回普通 Student 实体，不附带宿舍信息，避免列表场景无意义地进行关联查询。
     */
    List<Student> findAll() throws SQLException;

    /**
     * 给 StudentService 和 DormService 在创建或查询宿舍关系前确认学生存在。
     * Optional.empty() 表示数据层没有该学号，具体异常由业务层统一抛出。
     */
    Optional<Student> findById(String studentId) throws SQLException;

    /**
     * 给 StudentService 删除指定学号的学生资料。
     * 返回 true 表示实际删除了记录；若返回 false，则说明数据层中不存在该学号。
     */
    boolean deleteById(String studentId) throws SQLException;

    /**
     * 给 DormService 的“按宿舍查学生”功能提供跨表展示数据。
     * 返回列表按床号排列；空列表表示当前房间无人入住，不等同于房间不存在。
     */
    List<StudentDormView> findStudentsByRoom(long roomId) throws SQLException;

    /**
     * 给 StudentService 的“按学生查宿舍”功能提供学生和入住信息的组合视图。
     * 学生存在但尚未入住时仍返回视图，宿舍字段为空；没有学生记录才返回 Optional.empty()。
     */
    Optional<StudentDormView> findDormByStudentId(String studentId) throws SQLException;
}

