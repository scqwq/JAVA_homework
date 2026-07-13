package repo.interfaces;

import model.Room;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 宿舍房间的数据访问约定，供 {@code RoomService} 管理房间信息，也为
 * {@code DormService} 的入住校验提供房间基础数据。
 * <p>
 * 本接口不判断楼层是否合法、房间是否属于用户输入的宿舍楼，这些业务规则由 Service 层组合查询结果后处理。
 */
public interface RoomRepository {
    /**
     * 给 RoomService 在确认所属宿舍楼存在后写入房间记录。
     * 返回带数据库主键的 Room，便于业务层向 UI 回显新房间编号。
     */
    Room save(Room room) throws SQLException;

    /**
     * 给 RoomService 的全部房间列表使用。
     * 实现层负责稳定排序，调用方得到的是可直接展示的领域对象列表。
     */
    List<Room> findAll() throws SQLException;

    /**
     * 给 RoomService 按宿舍楼展示房间，也可供 DormService 相关流程复用。
     * 若该楼没有房间返回空列表；宿舍楼 ID 是否有效应先由业务层确认。
     */
    List<Room> findByBuildingId(long buildingId) throws SQLException;

    /**
     * 给 RoomService 和 DormService 根据房间 ID 取回归属与楼层信息。
     * 未找到时返回 Optional.empty()，由调用 Service 决定错误提示。
     */
    Optional<Room> findById(long roomId) throws SQLException;

    /**
     * 给 RoomService 按楼栋、楼层和房间号定位具体房间。
     * 适合“用户知道物理位置但不知道房间 ID”的查询场景。
     */
    Optional<Room> findByBuildingFloorAndRoomNumber(long buildingId, int floorNumber, String roomNumber) throws SQLException;
}

