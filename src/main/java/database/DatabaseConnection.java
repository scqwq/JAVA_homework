package database;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import repo.interfaces.BuildingRepository;
import repo.interfaces.DormRepository;
import repo.interfaces.RoomRepository;
import repo.interfaces.StudentRepository;
import repo.jdbc.JdbcBuildingRepository;
import repo.jdbc.JdbcDormRepository;
import repo.jdbc.JdbcRoomRepository;
import repo.jdbc.JdbcStudentRepository;

/**
 * 数据库连接管理类，实现 AutoCloseable 接口以支持 try-with-resources。
 * <p>
 * 职责：
 * <ul>
 *   <li>从环境变量或 .env 文件中读取数据库 DSN（数据源名称）</li>
 *   <li>建立并持有 JDBC 连接</li>
 *   <li>根据 DSN 类型自动识别数据库方言（PostgreSQL / MySQL）</li>
 *   <li>初始化数据库表结构（执行建表 SQL）</li>
 *   <li>创建并持有各 Repository 实例，供 Service 层使用</li>
 * </ul>
 */
public final class DatabaseConnection implements AutoCloseable {

    // 底层 JDBC 连接对象，相当于数据库访问入口。
    private final Connection connection;
    // 数据库方言，决定建表 SQL 的语法差异。
    private final DatabaseDialect dialect;
    // 宿舍楼仓储，封装对 buildings 表的 CRUD 操作。
    private final BuildingRepository buildingRepository;
    // 宿舍房间仓储，封装对 rooms 表的 CRUD 操作。
    private final RoomRepository roomRepository;
    // 学生仓储，封装对 students 表的 CRUD 操作。
    private final StudentRepository studentRepository;
    // 宿舍分配仓储，封装对 dorm_assignments 表的 CRUD 操作。
    private final DormRepository dormRepository;

    // 私有构造器，由静态工厂方法 fromEnv() 调用；完成连接、方言识别及仓储初始化。
    private DatabaseConnection(Connection connection, DatabaseDialect dialect) {
        this.connection = connection;
        this.dialect = dialect;
        // 将底层 JDBC Connection 注入各 JDBC 实现类，统一由本类管理。
        this.buildingRepository = new JdbcBuildingRepository(connection);
        this.roomRepository = new JdbcRoomRepository(connection);
        this.studentRepository = new JdbcStudentRepository(connection);
        this.dormRepository = new JdbcDormRepository(connection);
    }

    // 静态工厂方法：从环境变量或 .env 文件读取配置，建立数据库连接并返回本类实例。
    public static DatabaseConnection fromEnv() throws SQLException, IOException {
        // 先读取系统环境变量，再叠加 .env 文件中的配置（文件优先）。
        Map<String, String> values = new HashMap<>(System.getenv());
        Path envPath = Path.of(".env");
        if (Files.exists(envPath)) {
            values.putAll(loadEnvFile(envPath));
        }

        // 依次尝试三种 DSN 环境变量名，取第一个非空值。
        String dsn = firstNonBlank(values.get("DATABASE_DSN"), values.get("PGDSN"), values.get("MYSQL_DSN"));
        if (dsn == null) {
            throw new IllegalStateException("未找到 DATABASE_DSN / PGDSN / MYSQL_DSN，请先配置 .env。");
        }

        // 根据 DSN 前缀判断数据库类型并建立连接，开启自动提交。
        DatabaseDialect dialect = DatabaseDialect.fromDsn(dsn);
        Connection connection = DriverManager.getConnection(dsn);
        connection.setAutoCommit(true);
        return new DatabaseConnection(connection, dialect);
    }

    // 初始化数据库 schema：按方言执行建表 SQL（带 IF NOT EXISTS，幂等安全）。
    public void initializeSchema() throws SQLException {
        for (String statement : dialect.schemaStatements()) {
            try (Statement sql = connection.createStatement()) {
                sql.execute(statement);
            }
        }
    }

    // 返回宿舍楼仓储入口，供 BuildingService 使用。
    public BuildingRepository buildingRepository() {
        return buildingRepository;
    }

    // 返回房间仓储入口，供 RoomService 使用。
    public RoomRepository roomRepository() {
        return roomRepository;
    }

    // 返回学生仓储入口，供 StudentService 使用。
    public StudentRepository studentRepository() {
        return studentRepository;
    }

    // 返回宿舍分配仓储入口，供 DormService 使用。
    public DormRepository dormRepository() {
        return dormRepository;
    }

    // 关闭底层 JDBC 连接；由 try-with-resources 自动调用。
    @Override
    public void close() throws SQLException {
        connection.close();
    }

    // 逐行解析 .env 文件，返回 key=value 映射；跳过空行、注释行并去除值两侧引号。
    private static Map<String, String> loadEnvFile(Path path) throws IOException {
        Map<String, String> env = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                env.put(key, stripQuotes(value));
            }
        }
        return env;
    }

    // 去除字符串首尾的单引号或双引号。
    private static String stripQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    // 返回可变参数中第一个非 null 且非空白的字符串；全部为空时返回 null。
    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
