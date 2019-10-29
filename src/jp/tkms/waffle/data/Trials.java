package jp.tkms.waffle.data;

import jp.tkms.waffle.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public class Simulator {
    private UUID id = null;
    private String shortId;
    private String name;

    private Project project;

    public Simulator(Project project, UUID id, String name) {
        this.project = project;
        this.id = id;
        this.shortId = id.toString().replaceFirst("-.*$", "");
        this.name = name;
    }

    public Simulator(Project project, String id) {
        this.project = project;
        try {
            Database db = getWorkDB(project);
            PreparedStatement statement = db.preparedStatement("select id,name from simulator where id=?;");
            statement.setString(1, id);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                this.id = UUID.fromString(resultSet.getString("id"));
                this.shortId = this.id.toString().replaceFirst("-.*$", "");
                this.name = resultSet.getString("name");
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

    public Project getProject() {
        return project;
    }

    public Path getLocation() {
        Path path = Paths.get(project.getLocation().toAbsolutePath() + File.separator +
                "simulator" + File.separator + name + '_' + shortId
        );
        return path;
    }

    public static ArrayList<Simulator> getSimulatorList(Project project) {
        ArrayList<Simulator> simulatorList = new ArrayList<>();
        try {
            Database db = getWorkDB(project);
            ResultSet resultSet = db.executeQuery("select id,name from simulator;");
            while (resultSet.next()) {
                simulatorList.add(new Simulator(
                        project,
                        UUID.fromString(resultSet.getString("id")),
                        resultSet.getString("name"))
                );
            }

            db.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return simulatorList;
    }

    public static Simulator create(Project project, String name) {
        Simulator simulator = new Simulator(project, UUID.randomUUID(), name);
        System.out.println(simulator.getName());
        System.out.println(simulator.name);
        try {
            Database db = getWorkDB(project);
            PreparedStatement statement
                = db.preparedStatement("insert into simulator(id,name) values(?,?);");
            statement.setString(1, simulator.getId());
            statement.setString(2, simulator.getName());
            statement.execute();
            db.commit();
            db.close();

            Files.createDirectories(simulator.getLocation());
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return simulator;
    }

    private static Database getWorkDB(Project project) {
        Database db = Database.getWorkDB(project);
        updateWorkDB(db);
        return db;
    }

    private static void updateWorkDB(Database db) {
        try {
            int currentVersion = db.getVersion("simulator");
            int version = 0;

            if (currentVersion <= version++) {
                db.execute("create table simulator(" +
                        "id,name," +
                        "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                        ");");
            }

            db.setVersion("simulator", version);
            db.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
