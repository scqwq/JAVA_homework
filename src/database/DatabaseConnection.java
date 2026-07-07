package database;

import repo.interfaces.BuildingRepository;
import repo.interfaces.DormRepository;
import repo.interfaces.RoomRepository;
import repo.interfaces.StudentRepository;
import repo.jdbc.JdbcBuildingRepository;
import repo.jdbc.JdbcDormRepository;
import repo.jdbc.JdbcRoomRepository;
import repo.jdbc.JdbcStudentRepository;

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

public final class DatabaseConnection implements AutoCloseable {
    private final Connection connection;
    private final DatabaseDialect dialect;
    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final DormRepository dormRepository;

    private DatabaseConnection(Connection connection, DatabaseDialect dialect) {
        this.connection = connection;
        this.dialect = dialect;
        this.buildingRepository = new JdbcBuildingRepository(connection);
        this.roomRepository = new JdbcRoomRepository(connection);
        this.studentRepository = new JdbcStudentRepository(connection);
        this.dormRepository = new JdbcDormRepository(connection);
    }

    public static DatabaseConnection fromEnv() throws SQLException, IOException {
        Map<String, String> values = new HashMap<>(System.getenv());
        Path envPath = Path.of(".env");
        if (Files.exists(envPath)) {
            values.putAll(loadEnvFile(envPath));
        }

        String dsn = firstNonBlank(values.get("DATABASE_DSN"), values.get("PGDSN"), values.get("MYSQL_DSN"));
        if (dsn == null) {
            throw new IllegalStateException("未找到 DATABASE_DSN / PGDSN / MYSQL_DSN，请先配置 .env。");
        }

        DatabaseDialect dialect = DatabaseDialect.fromDsn(dsn);
        Connection connection = DriverManager.getConnection(dsn);
        connection.setAutoCommit(true);
        return new DatabaseConnection(connection, dialect);
    }

    public void initializeSchema() throws SQLException {
        for (String statement : dialect.schemaStatements()) {
            try (Statement sql = connection.createStatement()) {
                sql.execute(statement);
            }
        }
    }

    public BuildingRepository buildingRepository() {
        return buildingRepository;
    }

    public RoomRepository roomRepository() {
        return roomRepository;
    }

    public StudentRepository studentRepository() {
        return studentRepository;
    }

    public DormRepository dormRepository() {
        return dormRepository;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

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

    private static String stripQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

