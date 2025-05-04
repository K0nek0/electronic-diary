package planner.models;

import java.sql.*;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:planner.db";
    
    public static void initialize() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
            Statement stmt = conn.createStatement()) {
            
            // Создаем таблицу категорий
            String createCategoriesTable = "CREATE TABLE IF NOT EXISTS categories (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL UNIQUE)";
            stmt.execute(createCategoriesTable);
            
            // Создаем таблицу задач
            String createTasksTable = "CREATE TABLE IF NOT EXISTS tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL," +
                    "description TEXT," +
                    "due_date TEXT," +
                    "completed BOOLEAN DEFAULT FALSE," +
                    "category_id INTEGER," +
                    "FOREIGN KEY (category_id) REFERENCES categories(id))";
            stmt.execute(createTasksTable);
            
            // Добавляем тестовые данные, если таблицы пустые
            if (isTableEmpty(conn, "categories")) {
                stmt.execute("INSERT INTO categories (name) VALUES " +
                        "('Работа'), " +
                        "('Учеба'), " +
                        "('Личное')");
            }
            
        } catch (SQLException e) {
            System.err.println("Ошибка при инициализации базы данных: " + e.getMessage());
        }
    }
    
    private static boolean isTableEmpty(Connection conn, String tableName) throws SQLException {
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            return rs.getInt(1) == 0;
        }
    }
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
