package repo.interfaces;

import model.Building;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 宿舍楼的数据访问约定，主要给 {@code BuildingService} 使用。
 * <p>
 * 和业务层协作时，本接口只负责 buildings 表的读写与查询结果表达；楼号是否可新增、
 * 找不到记录后向用户显示什么提示，仍由 BuildingService 和 UI 层决定。
 */
public interface BuildingRepository {
    /**
     * 给 BuildingService 在完成业务校验后保存新宿舍楼。
     * Repository 负责生成后的主键回填，业务层继续使用返回实体处理后续流程。
     */
    Building save(Building building) throws SQLException;

    /**
     * 给 BuildingService 的宿舍楼列表页面提供全部记录。
     * 返回顺序由实现层固定，UI 只负责展示，不应依赖数据库实现细节重新排序。
     */
    List<Building> findAll() throws SQLException;

    /**
     * 给 BuildingService、RoomService 和 DormService 按内部 ID 确认宿舍楼存在。
     * 没有记录时返回 Optional.empty()，由调用方转换为对应业务错误。
     */
    Optional<Building> findById(long buildingId) throws SQLException;

    /**
     * 给 BuildingService 在新增宿舍楼前检查楼号是否重复。
     * 这里只负责按业务编号查询；是否提示用户、是否允许继续由业务层决定。
     */
    Optional<Building> findByCode(String buildingCode) throws SQLException;
}

