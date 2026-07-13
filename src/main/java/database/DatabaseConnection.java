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
 * 数据库连接管理类。
 * <p>
 * 职责：
 * <ul>
 *   <li>从环境变量或 .env 文件中读取数据库 DSN（数据源名称）</li>
 *   <li>建立并持有 JDBC 连接</li>
 *   <li>根据 DSN 类型自动识别数据库方言（PostgreSQL / MySQL）</li>
 *   <li>初始化数据库表结构（执行建表 SQL）</li>
 *   <li>创建并持有各 Repository 实例，供 Service 层使用</li>
 * </ul>
 * 实现了 {@link AutoCloseable}，支持 try-with-resources 自动关闭连接。
 */

//Autocloseable是java提供的接口  imple意思是保证实现接口中的方法(即保证实现某个接口)
// dbc 类似 gorm.DB ，都是"封装后的数据库入口"
public final class DatabaseConnection implements AutoCloseable {

    // JDBC 连接对象，类似sql.DB，属于底层连接，Con类已经封装了一层
    private final Connection connection;

    /** 数据库方言，决定建表 SQL 的语法。 */
    private final DatabaseDialect dialect;

    /** 宿舍楼仓储，封装对 buildings 表的 CRUD 操作。 */
    private final BuildingRepository buildingRepository;

    /** 宿舍房间仓储，封装对 rooms 表的 CRUD 操作。 */
    private final RoomRepository roomRepository;

    /** 学生仓储，封装对 students 表的 CRUD 操作。 */
    private final StudentRepository studentRepository;

    /** 宿舍分配仓储，封装对 dorm_assignments 表的 CRUD 操作。 */
    private final DormRepository dormRepository;

    /**
     * 私有构造器——由静态工厂方法 {@link #fromEnv()} 调用。
     *
     * @param connection JDBC 连接（已由 DriverManager 建立）
     * @param dialect    数据库方言（由 DSN 自动识别）
     */
    private DatabaseConnection(Connection connection, DatabaseDialect dialect) {
        this.connection = connection;
        this.dialect = dialect;
        // 将底层 JDBC Connection 注入各 Repository 实现，实现数据访问的封装
        this.buildingRepository = new JdbcBuildingRepository(connection);
        this.roomRepository = new JdbcRoomRepository(connection);
        this.studentRepository = new JdbcStudentRepository(connection);
        this.dormRepository = new JdbcDormRepository(connection);
    }

    /**
     * 静态工厂方法：从环境变量或 .env 文件读取配置，创建数据库连接。
     * <p>
     * 配置读取优先级：系统环境变量 → .env 文件。
     * DSN 查找顺序：DATABASE_DSN → PGDSN → MYSQL_DSN（取第一个非空的）。
     *
     * @return 已初始化的 DatabaseConnection 实例
     * @throws SQLException     JDBC 连接失败
     * @throws IOException      读取 .env 文件失败
     * @throws IllegalStateException 未找到任何 DSN 配置
     */
    public static DatabaseConnection fromEnv() throws SQLException, IOException {
        // 先读取系统环境变量，再叠加 .env 文件中的配置（文件优先）
        Map<String, String> values = new HashMap<>(System.getenv());
        Path envPath = Path.of(".env");
        if (Files.exists(envPath)) {
            values.putAll(loadEnvFile(envPath));
        }

        // 依次尝试三种 DSN 环境变量名，取第一个非空值
        String dsn = firstNonBlank(values.get("DATABASE_DSN"), values.get("PGDSN"), values.get("MYSQL_DSN"));
        if (dsn == null) {
            throw new IllegalStateException("未找到 DATABASE_DSN / PGDSN / MYSQL_DSN，请先配置 .env。");
        }

        // 根据 DSN 的 jdbc: 前缀判断数据库类型，获取对应的方言
        DatabaseDialect dialect = DatabaseDialect.fromDsn(dsn);
        // 建立 JDBC 连接，并开启自动提交
        Connection connection = DriverManager.getConnection(dsn);
        connection.setAutoCommit(true);
        return new DatabaseConnection(connection, dialect);
    }

    /**
     * 初始化数据库 schema：执行建表 SQL（带 IF NOT EXISTS，幂等安全）。
     * <p>
     * 建表顺序：buildings → rooms → students → dorm_assignments
     * 保证外键依赖的表先被创建。
     */
    public void initializeSchema() throws SQLException {
        for (String statement : dialect.schemaStatements()) {
            try (Statement sql = connection.createStatement()) {
                sql.execute(statement);
            }
        }
    }

    /**
     * 供 BuildingService 获取宿舍楼数据访问入口。
     * 连接和 JDBC 实现由本类统一持有，业务层只依赖 BuildingRepository 约定。
     *
     * @return 与当前数据库连接绑定的宿舍楼仓储
     */
    public BuildingRepository buildingRepository() {
        return buildingRepository;
    }

    /**
     * 供 RoomService 获取房间数据访问入口。
     * 如果 DormService 需要房间信息，应通过 RoomService 保持业务校验边界，不直接处理 JDBC 连接。
     *
     * @return 与当前数据库连接绑定的房间仓储
     */
    public RoomRepository roomRepository() {
        return roomRepository;
    }

    /**
     * 供 StudentService 获取学生及学生宿舍视图的数据访问入口。
     * DormService 复用学生信息时同样通过 StudentService 组织业务语义。
     *
     * @return 与当前数据库连接绑定的学生仓储
     */
    public StudentRepository studentRepository() {
        return studentRepository;
    }

    /**
     * 供 DormService 获取入住和调宿的数据访问入口。
     * 仓储只保存分配结果，分配规则和面向用户的错误信息仍由 DormService 负责。
     *
     * @return 与当前数据库连接绑定的宿舍分配仓储
     */
    public DormRepository dormRepository() {
        return dormRepository;
    }

    /**
     * 关闭底层 JDBC 连接。
     * <p>
     * 由 try-with-resources 自动调用，无需手动管理。
     */
    @Override
    public void close() throws SQLException {
        connection.close();
    }

    /**
     * 逐行解析 .env 文件，返回 key=value 的映射。
     * <p>
     * 支持：
     * <ul>
     *   <li>以 # 开头的注释行会被跳过</li>
     *   <li>空行会被跳过</li>
     *   <li>值两侧的引号（单引号或双引号）会被自动去除</li>
     * </ul>
     *
     * @param path .env 文件的路径
     * @return 解析后的键值对映射
     * @throws IOException 读取文件时出错
     */
    private static Map<String, String> loadEnvFile(Path path) throws IOException {
        Map<String, String> env = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // 跳过空行、注释行和无效行
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

    /**
     * 去除字符串首尾的引号（支持单引号 `'` 和双引号 `"`）。
     * <p>
     * 例如：`"jdbc:postgresql://..."` → `jdbc:postgresql://...`
     *
     * @param value 原始值
     * @return 去除引号后的值
     */
    private static String stripQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * 返回可变参数列表中第一个非 null、非空白的字符串。
     *
     * @param values 候选值列表
     * @return 第一个非空值，若全部为空则返回 null
     */
    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
