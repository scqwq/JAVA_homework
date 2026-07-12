package repo.interfaces;

import model.Room;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {
    Room save(Room room) throws SQLException;

    List<Room> findAll() throws SQLException;

    List<Room> findByBuildingId(long buildingId) throws SQLException;

    Optional<Room> findById(long roomId) throws SQLException;
}

