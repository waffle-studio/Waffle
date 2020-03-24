package jp.tkms.waffle.data;

import org.json.JSONArray;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

abstract public class AbstractRun extends ProjectData {
  protected static final String KEY_FINALIZER = "finalizer";

  private JSONArray finalizers;

  public AbstractRun(Project project) {
    super(project);
  }

  public AbstractRun(Project project, UUID id, String name) {
    super(project, id, name);
  }

  public ArrayList<String> getFinalizers() {
    if (finalizers == null) {
      finalizers = new JSONArray(getFromDB(KEY_FINALIZER));
    }
    ArrayList<String> stringList = new ArrayList<>();
    for (Object o : finalizers.toList()) {
      stringList.add(o.toString());
    }
    return stringList;
  }

  public void setFinalizers(ArrayList<String> finalizers) {
    this.finalizers = new JSONArray(finalizers);
    String finalizersJson = this.finalizers.toString();

    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("update " + getTableName() + " set " + KEY_FINALIZER + "=? where " + KEY_ID + "=?;");
        statement.setString(1, finalizersJson);
        statement.setString(2, getId());
        statement.execute();
      }
    });
  }

  public void addRawFinalizerScript(String script) {
    ArrayList<String> finalizers = getFinalizers();
    finalizers.add(script);
    setFinalizers(finalizers);
  }

  public void addFinalizer(ConductorRun conductorRun, String name) {
    String fileName = conductorRun.getConductor().getListenerScriptFileName(name);
    addRawFinalizerScript(conductorRun.getConductor().getFileContents(fileName));
  }
}
