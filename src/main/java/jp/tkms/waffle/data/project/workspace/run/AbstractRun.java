package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.WorkspaceData;
import jp.tkms.waffle.data.project.workspace.executable.StagedExecutable;
import jp.tkms.waffle.data.util.ChildElementsArrayList;
import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.exception.RunNotFoundException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;

abstract public class AbstractRun extends WorkspaceData implements DataDirectory, PropertyFile {
  public static final String RUN = "RUN";
  public static final String KEY_NOTE_TXT = "note.txt";
  public static final String KEY_ERROR_NOTE_TXT = "error_note.txt";
  protected static final String KEY_FINALIZER = "finalizer";
  protected static final String KEY_PARENT_RUN = "parent_run";

  private Path path;
  private JSONArray finalizers = null;
  private JSONArray callstack = null;
  private ConductorRun owner = null;
  private AbstractRun parent = null;

  public AbstractRun(Workspace workspace, AbstractRun parent, Path path) {
    super(workspace);
    this.path = path;
    setToProperty("class", getClass().getConstructors()[0].getDeclaringClass().getSimpleName());
    setParent(parent);
    if (parent == null) {
      this.owner = (ConductorRun) this;
    } else if (parent instanceof ConductorRun) {
      this.owner = (ConductorRun) parent;
    } else {
      this.owner = parent.getOwner();
    }
  }

  public static AbstractRun getInstance(Workspace workspace, String localPathString) throws RunNotFoundException {
    if (workspace == null || localPathString == null) {
      return null;
    }

    Path basePath = Constants.WORK_DIR.resolve(localPathString);

    if (Files.exists(basePath.resolve(ConductorRun.JSON_FILE))) {
      return ConductorRun.getInstance(workspace, localPathString);
    } else if (Files.exists(basePath.resolve(ProcedureRun.JSON_FILE))) {
      return ProcedureRun.getInstance(workspace, localPathString);
    } else if (Files.exists(basePath.resolve(ExecutableRun.JSON_FILE))) {
      return ExecutableRun.getInstance(localPathString);
    }

    return null;
  }

  public static ArrayList<AbstractRun> getList(Workspace workspace) {
    Path basePath = getBaseDirectoryPath(workspace);
    /*
    return new ChildElementsArrayList().getList(basePath, name -> {
      try {
        return getInstance(workspace, DataDirectory.toLocalDirectoryPath(basePath.resolve(name.toString())).toString());
      } catch (RunNotFoundException e) {
        ErrorLogMessage.issue(e);
      }
      return null;
    });
     */

    ArrayList<AbstractRun> abstractRunList = new ArrayList<>();
    try {
      Files.list(basePath).sorted(
        Comparator.comparingLong(path -> {
          try {
            return Files.readAttributes(path, BasicFileAttributes.class).creationTime().toInstant().toEpochMilli() * -1;
          } catch (IOException e) {
            return 0;
          }
        })
      ).forEach(path -> {
        if (Files.isDirectory(path)) {
          try {
            abstractRunList.add(getInstance(workspace, DataDirectory.toLocalDirectoryPath(path).toString()));
          } catch (RunNotFoundException e) {
            ErrorLogMessage.issue(e);
          }
        }
      });
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
    return abstractRunList;
  }

  public ArrayList<AbstractRun> getList() {
    /*
    return new ChildElementsArrayList().getList(getDirectoryPath(), name -> {
      try {
        return getInstance(getWorkspace(), DataDirectory.toLocalDirectoryPath(getDirectoryPath().resolve(name.toString())).toString());
      } catch (RunNotFoundException e) {
        ErrorLogMessage.issue(e);
      }
      return null;
    });
     */

    ArrayList<AbstractRun> abstractRunList = new ArrayList<>();
    try {
      Files.list(getDirectoryPath()).sorted(
        Comparator.comparingLong(path -> {
          try {
            return Files.readAttributes(path, BasicFileAttributes.class).creationTime().toInstant().toEpochMilli() * -1;
          } catch (IOException e) {
            return 0;
          }
        })
      ).forEach(path -> {
        if (Files.isDirectory(path)) {
          try {
            abstractRunList.add(getInstance(getWorkspace(), DataDirectory.toLocalDirectoryPath(path).toString()));
          } catch (RunNotFoundException e) {
            ErrorLogMessage.issue(e);
          }
        }
      });
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
    return abstractRunList;
  }

  public ConductorRun getOwner() {
    return owner;
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

  public String getName() {
    return getDirectoryPath().getFileName().toString();
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

  public void setParent(AbstractRun parent) {
    this.parent = parent;
    if (parent != null) {
      setToProperty(KEY_PARENT_RUN, parent.getLocalDirectoryPath().toString());
    }
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
    /*
    ConductorRun referenceOwner = getOwner();
    if (this instanceof ConductorRun) {
      if (((ConductorRun)this).isActorGroupRun()) {
        referenceOwner = (finalizerReferenceOwner == null ? getOwner() : finalizerReferenceOwner);
      }
    }
    ConductorRun actorRun = ConductorRun.create(getRunNode(),
      (ConductorRun)(this instanceof ExecutableRun || (this instanceof ConductorRun && ((ConductorRun)this).isActorGroupRun())
        ? getParentActor() : this), referenceOwner, key);
    ProcedureRun procedureRun = ProcedureRun.create(this, )
    ArrayList<String> finalizers = getFinalizers();
    finalizers.add(actorRun.getDirectoryPath().toString());
    setFinalizers(finalizers);
     */
  }

  public static Path getBaseDirectoryPath(Workspace workspace) {
    return workspace.getDirectoryPath().resolve(RUN);
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
