import com.sun.net.httpserver.HttpServer;
import database.DatabaseConnection;
import service.BuildingService;
import service.DormService;
import service.RoomService;
import service.StudentService;
import ui.ConsoleUtils;

import java.io.Console;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

/**
 * 系统入口类，负责组装 Service、初始化数据库并启动控制台主菜单。
 * <p>
 * 采用 try-with-resources 管理 {@link DatabaseConnection}，确保程序退出时自动关闭 JDBC 连接。
 */
public class Main {

    // 类加载时根据环境变量或控制台字符集对齐标准输出编码，减少中文乱码。
    static {
        alignConsoleEncoding();
    }

    // 程序入口：从环境变量创建连接、建表，然后进入主循环。
    public static void main(String[] args) {
        try {
            DatabaseConnection databaseConnection = DatabaseConnection.fromEnv();
            databaseConnection.initializeSchema();

            // 按依赖关系实例化各业务服务；DormService 需要引用其他服务以完成入住校验。
            BuildingService buildingService = new BuildingService(databaseConnection);
            RoomService roomService = new RoomService(databaseConnection, buildingService);
            StudentService studentService = new StudentService(databaseConnection);
            DormService dormService = new DormService(databaseConnection, studentService, buildingService, roomService);

            HttpServer server = Router.start(buildingService, roomService, studentService, dormService);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.stop(0);
                try {
                    databaseConnection.close();
                } catch (Exception ignored) {
                }
            }));

            System.out.println("Web 服务已启动: http://localhost:" + server.getAddress().getPort());
            Thread.currentThread().join();
        } catch (Exception exception) {
            ConsoleUtils.printError("系统启动失败: " + exception.getMessage());
        }
    }

    // 根据 CONSOLE_ENCODING 环境变量或交互式控制台的字符集，调整 System.out / System.err 编码。
    // 当程序被重定向时（System.console() 为 null）且未设置环境变量，则保持 JVM 默认行为。
    private static void alignConsoleEncoding() {
        try {
            Charset targetCharset = null;
            String envEncoding = System.getenv("CONSOLE_ENCODING");
            if (envEncoding != null && !envEncoding.isBlank()) {
                targetCharset = Charset.forName(envEncoding);
            } else {
                Console console = System.console();
                if (console != null) {
                    targetCharset = console.charset();
                }
            }
            if (targetCharset != null) {
                System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, targetCharset));
                System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, targetCharset));
            }
        } catch (Exception ignored) {
            // 编码对齐失败时继续使用 JVM 默认输出，避免影响程序启动。
        }
    }
}
