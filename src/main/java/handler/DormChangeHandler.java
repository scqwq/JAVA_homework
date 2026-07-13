package handler;

import com.sun.net.httpserver.HttpExchange;
import model.DormAssignment;
import service.DormService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class DormChangeHandler extends BaseHandler {
    private final DormService dormService;

    public DormChangeHandler(DormService dormService) {
        this.dormService = dormService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            ensureMethod(exchange, "POST");
            Map<String, String> params = formParams(exchange);
            DormAssignment dormAssignment = dormService.changeDorm(
                    requireText(params, "studentId", "学号"),
                    requireLong(params, "buildingId", "宿舍楼 ID"),
                    requireLong(params, "roomId", "房间 ID"),
                    requireInt(params, "bedNumber", "床号")
            );
            redirect(exchange, "/?tab=dorms&message=" + encode("调宿成功，新的床号: " + dormAssignment.bedNumber()));
        } catch (IllegalArgumentException exception) {
            redirect(exchange, "/?tab=dorms&error=" + encode(exception.getMessage()));
        } catch (SQLException exception) {
            redirect(exchange, "/?tab=dorms&error=" + encode("数据库操作失败: " + exception.getMessage()));
        }
    }
}
