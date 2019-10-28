package jp.tkms.waffle.data;

import jp.tkms.waffle.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.crypto.Data;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

public class Database {
    static Logger logger = LoggerFactory.getLogger("Database");

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

    public static Database getDB(Path path) throws SQLException {
        initialize();
        String url = "jdbc:sqlite:" + path.toAbsolutePath().toString();
        return new Database(DriverManager.getConnection(url));
    }

    public static Database getMainDB() throws SQLException {
        return getDB(Paths.get(Environment.MAIN_DB_NAME));
    }

    private Connection connection;

    public Database(Connection connection) throws SQLException {
        this.connection = connection;
        connection.setAutoCommit(false);
    }

    public void commit() throws SQLException {
        connection.commit();
    }

    public void close() throws SQLException {
        connection.close();
    }

    public int getVersion() {
        int version = -1;
        try (ResultSet resultSet = executeQuery("select value from system where name='version';")) {
            while (resultSet.next()){
                version = resultSet.getInt(1);
            }
        } catch (SQLException e) {
            logger.info("Create new main database");
        }
        if (version == -1) {
            try {
                execute("create table system(name,value);");
                execute("insert into system(name,value) values('version',0);");
                version = 0;
                commit();
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }
        return version;
    }

    public void setVersion(int version) {
        try {
            execute("update system set value=" + version + " where name='version';");
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }

    public void execute(String sql) throws SQLException {
        Statement statement = connection.createStatement();
        statement.execute(sql);
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        Statement statement = connection.createStatement();
        return statement.executeQuery(sql);
    }

    public PreparedStatement preparedStatement(String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }
}
