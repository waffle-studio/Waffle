package jp.tkms.waffle.data;

import jp.tkms.waffle.Environment;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static boolean initialized = false;

    private static void initialize() {
        if (!initialized) {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static Connection getConnection(Path path) throws SQLException {
        initialize();
        String url = "jdbc:sqlite:" + path.toAbsolutePath().toString();
        return DriverManager.getConnection(url);
    }

    public static Connection getMainDBConnection() throws SQLException {
        return getConnection(Paths.get(Environment.MAIN_DB_NAME));
    }
}
