package jp.tkms.waffle.data;

import jp.tkms.waffle.Environment;

import java.io.IOException;
import java.nio.file.Files;
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
        this.location = Paths.get(Environment.DEFAULT_WD.replaceFirst(
                "\\$\\{NAME\\}", name + '_' + shortId));
    }

    public Project(String id) {
        try {
            Database db = getMainDB();
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

    private String getFromDB(String key) {
        String result = null;
        try {
            Database db = getWorkDB();
            PreparedStatement statement = db.preparedStatement("select " + key + " from project where id=?;");
            statement.setString(1, getId());
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                result = resultSet.getString(key);
            }
            db.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean isValid() { return id != null; }

    public String getName() {
        return name;
    }

    public String getId() {
        return id.toString();
    }

    public String getShortId() { return shortId; }

    public Path getLocation() {
        return location;
    }

    public ArrayList<Simulator> getSimulatorList() {
        return Simulator.getSimulatorList(this);
    }

    public Simulator getSimulator(String id) {
        return new Simulator(this, id);
    }

    public static ArrayList<Project> getProjectList() {
        ArrayList<Project> projectList = new ArrayList<>();
        try {
            Database db = getMainDB();
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

    public static Project create(String name) {
        Project project = new Project(UUID.randomUUID(), name);
        try {
            Database db = getMainDB();
            PreparedStatement statement
                = db.preparedStatement("insert into project(id,name,location) values(?,?,?);");
            statement.setString(1, project.getId());
            statement.setString(2, project.getName());
            statement.setString(3, project.getLocation().toString());
            statement.execute();
            db.commit();
            db.close();

            Files.createDirectories(project.getLocation());

            project.getWorkDB().close(); // initialize database
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return project;
    }

    Database getWorkDB() {
        Database db = Database.getWorkDB(this);
        updateWorkDB(db);
        return db;
    }

    private void updateWorkDB(Database db) {
        try {
            int currentVersion = db.getVersion();
            int version = 0;

            if (currentVersion <= version++) {
                PreparedStatement statement
                    = db.preparedStatement("insert into system(name,value) values('id',?);");
                statement.setString(1, getId());
                statement.execute();

                statement = db.preparedStatement("insert into system(name,value) values('name',?);");
                statement.setString(1, getName());
                statement.execute();

                db.execute("insert into system(name,value)" +
                        " values('timestamp_create',(DATETIME('now','localtime')));");
            }

            db.setVersion(version);
            db.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Database getMainDB() {
        Database db = Database.getMainDB();
        updateMainDB(db);
        return db;
    }

    private static void updateMainDB(Database db) {
        try {
            int currentVersion = db.getVersion();
            int version = 0;

            if (currentVersion <= version++) {
                db.execute("create table project(id,name,location);");
            }

            db.setVersion(version);
            db.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
