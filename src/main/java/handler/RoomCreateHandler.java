package handler;

import com.sun.net.httpserver.HttpExchange;
import model.Room;
import service.RoomService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class RoomCreateHandler extends BaseHandler {
    private final RoomService roomService;

    public RoomCreateHandler(RoomService roomService) {
        this.roomService = roomService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            ensureMethod(exchange, "POST");
            Map<String, String> params = formParams(exchange);
            Room room = roomService.createRoom(
                    requireText(params, "roomNumber", "房间号"),
                    requireLong(params, "buildingId", "请选择宿舍楼"),
                    requireInt(params, "floorNumber", "楼层")
            );
            redirect(exchange, "/?tab=rooms&message=" + encode("房间创建成功: " + room.roomNumber()));
        } catch (IllegalArgumentException exception) {
            redirect(exchange, "/?tab=rooms&error=" + encode(exception.getMessage()));
        } catch (SQLException exception) {
            redirect(exchange, "/?tab=rooms&error=" + encode("数据库操作失败: " + exception.getMessage()));
        }
    }
}
