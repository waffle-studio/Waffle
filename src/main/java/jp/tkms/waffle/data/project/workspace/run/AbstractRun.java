package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.WorkspaceData;
import jp.tkms.waffle.data.util.FileName;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

abstract public class AbstractRun extends WorkspaceData implements DataDirectory, PropertyFile {
  public static final String RUN = "RUN";
  public static final String KEY_NOTE_TXT = "note.txt";
  public static final String KEY_ERROR_NOTE_TXT = "error_note.txt";
  protected static final String KEY_FINALIZER = "finalizer";

  private Path path;
  private JSONArray finalizers = null;
  private JSONArray callstack = null;
  private ConductorRun finalizerReferenceOwner = null;

  public AbstractRun(Workspace workspace, Path path) {
    super(workspace);
    this.path = path;
  }

  protected String generateUniqueFileName(String name) {
    name = FileName.removeRestrictedCharacters(name);
    String uniqueName = name;
    int count = 0;
    while (uniqueName.length() <= 0 || Files.exists(getDirectoryPath().resolve(uniqueName))) {
      uniqueName = name + '_' + count++;
    }
    return uniqueName;
  }

  public void setNote(String text) {
    createNewFile(KEY_NOTE_TXT);
    updateFileContents(KEY_NOTE_TXT, text);
  }

  public String getNote() {
    return getFileContents(KEY_NOTE_TXT);
  }

  public void appendErrorNote(String note) {
    createNewFile(KEY_ERROR_NOTE_TXT);
    updateFileContents(KEY_ERROR_NOTE_TXT, getErrorNote().concat(note).concat("\n"));
  }

  public String getErrorNote() {
    return getFileContents(KEY_ERROR_NOTE_TXT);
  }


  public ArrayList<String> getFinalizers() {
    if (finalizers == null) {
      finalizers = new JSONArray(getStringFromProperty(KEY_FINALIZER, "[]"));
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
    setToProperty(KEY_FINALIZER, finalizersJson);
  }

  public void addFinalizer(String key) {
    ConductorRun referenceOwner = getOwner();
    if (this instanceof ConductorRun) {
      if (((ConductorRun)this).isActorGroupRun()) {
        referenceOwner = (finalizerReferenceOwner == null ? getOwner() : finalizerReferenceOwner);
      }
    }
    ConductorRun actorRun = ConductorRun.create(getRunNode(),
      (ConductorRun)(this instanceof ExecutableRun || (this instanceof ConductorRun && ((ConductorRun)this).isActorGroupRun())
        ? getParentActor() : this), referenceOwner, key);
    ArrayList<String> finalizers = getFinalizers();
    finalizers.add(actorRun.getDirectoryPath().toString());
    setFinalizers(finalizers);
  }

  protected void setFinalizerReference(ConductorRun actorRun) {
    this.finalizerReferenceOwner = actorRun;
  }


  @Override
  public Path getDirectoryPath() {
    return path;
  }

  JSONObject propertyStoreCache = null;
  @Override
  public JSONObject getPropertyStoreCache() {
    return propertyStoreCache;
  }
  @Override
  public void setPropertyStoreCache(JSONObject cache) {
    propertyStoreCache = cache;
  }
}
