package jp.tkms.waffle.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public class Project {
    private UUID id;
    private String name;

    public Project(UUID id, String name) {
        this.id = id;
        this.name = name;
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
                database.execute("create table project(id,name);");
            }

            database.setVersion(version);
            database.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
