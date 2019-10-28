package jp.tkms.waffle.data;

import jp.tkms.waffle.Environment;

import javax.xml.crypto.Data;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public class Project {
    private UUID id = null;
    private String shortId;
    private String name;
    private Path location;

    public Project(UUID id, String name) {
        this.id = id;
        this.shortId = id.toString().replaceFirst("-.*$", "");
        this.name = name;
        this.location = Paths.get(Environment.DEFAULT_WD.replaceFirst("$\\{NAME\\}", name + '_' + shortId));
    }

    public Project(String id) {
        try {
            Database db = Database.getMainDB();
            updateDatabase(db);
            PreparedStatement statement = db.preparedStatement("select id,name,location from project where id=?;");
            statement.setString(1, id);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                this.id = UUID.fromString(resultSet.getString("id"));
                this.shortId = this.id.toString().replaceFirst("-.*$", "");
                this.name = resultSet.getString("name");
                this.location = Paths.get(resultSet.getString("location"));
            }
            db.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isValid() { return id != null; }

    public String getName() {
        return name;
    }

    public UUID getIdValue() { return id; }

    public String getId() {
        return id.toString();
    }

    public String getShortId() { return shortId; }

    public Path getLocation() {
        return location;
    }

    public static ArrayList<Project> getProjectList() {
        ArrayList<Project> projectList = new ArrayList<>();
        try {
            Database db = Database.getMainDB();
            updateDatabase(db);

            ResultSet resultSet = db.executeQuery("select id,name from project;");
            while (resultSet.next()) {
                projectList.add(new Project(
                    UUID.fromString(resultSet.getString("id")),
                    resultSet.getString("name"))
                );
            }

            db.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return projectList;
    }

    private static void updateDatabase(Database database) {
        try {
            int currentVersion = database.getVersion();
            int version = 0;

            if (currentVersion <= version++) {
                database.execute("create table project(id,name,location);");
            }

            database.setVersion(version);
            database.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
