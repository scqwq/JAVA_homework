package handler;

import com.sun.net.httpserver.HttpExchange;
import model.DormAssignment;
import service.DormService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class DormAssignHandler extends BaseHandler {
    private final DormService dormService;

    public DormAssignHandler(DormService dormService) {
        this.dormService = dormService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            ensureMethod(exchange, "POST");
            Map<String, String> params = formParams(exchange);
            DormAssignment dormAssignment = dormService.assignDorm(
                    requireText(params, "studentId", "学号"),
                    requireLong(params, "buildingId", "请选择宿舍楼"),
                    requireLong(params, "roomId", "请选择房间"),
                    requireInt(params, "bedNumber", "床号")
            );
            redirect(exchange, "/?tab=dorms&message=" + encode("宿舍分配成功，床号: " + dormAssignment.bedNumber()));
        } catch (IllegalArgumentException exception) {
            redirect(exchange, "/?tab=dorms&error=" + encode(exception.getMessage()));
        } catch (SQLException exception) {
            redirect(exchange, "/?tab=dorms&error=" + encode("数据库操作失败: " + exception.getMessage()));
        }
    }
}
