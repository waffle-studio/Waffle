package jp.tkms.waffle.data.computer;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.PollingThread;
import jp.tkms.waffle.data.util.InstanceCache;
import jp.tkms.waffle.data.web.Data;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.exception.NotFoundXsubException;
import jp.tkms.waffle.exception.WaffleException;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.data.util.ComputerState;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.submitter.*;
import org.json.JSONObject;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Computer implements DataDirectory, PropertyFile {
  private static final String KEY_LOCAL = "LOCAL";
  private static final String KEY_WORKBASE = "work_base_dir";
  private static final String KEY_XSUB = "xsub_dir";
  private static final String KEY_XSUB_TEMPLATE = "xsub_template";
  private static final String KEY_POLLING = "polling_interval";
  private static final String KEY_MAX_THREADS = "maximum_threads";
  private static final String KEY_ALLOCABLE_MEMORY = "allocable_memory";
  private static final String KEY_MAX_JOBS = "maximum_jobs";
  private static final String KEY_SUBMITTER = "submitter";
  private static final String KEY_ENCRYPT_KEY = "encrypt_key";
  private static final String KEY_PARAMETERS = "parameters";
  private static final String KEY_STATE = "state";
  private static final String KEY_ENVIRONMENTS = "environments";
  private static final String KEY_MESSAGE = "message";
  private static final String ENCRYPT_ALGORITHM = "AES/CBC/PKCS5Padding";
  private static final IvParameterSpec IV_PARAMETER_SPEC = new IvParameterSpec("0123456789ABCDEF".getBytes());

  private static final InstanceCache<String, Computer> instanceCache = new InstanceCache<>();

  public static final ArrayList<Class<AbstractSubmitter>> submitterTypeList = new ArrayList(Arrays.asList(
    JobNumberLimitedSshSubmitter.class,
    ThreadAndMemoryLimitedSshSubmitter.class,
    JobNumberLimitedLocalSubmitter.class,
    MultiComputerSubmitter.class,
    LoadBalancingSubmitter.class,
    DeadlineWrapper.class,
    WrappedSshSubmitter.class
  ));

  private String name;
  private String submitterType = null;
  private String workBaseDirectory = null;
  private String xsubDirectory = null;
  private SecretKeySpec encryptKey = null;
  private Integer pollingInterval = null;
  private Double maximumNumberOfThreads = null;
  private Double allocableMemorySize = null;
  private Integer maximumNumberOfJobs = null;
  private JSONObject parameters = null;
  private JSONObject xsubTemplate = null;

  public Computer(String name) {
    this.name = name;
    instanceCache.put(name, this);

    initialize();

    Main.registerFileChangeEventListener(getBaseDirectoryPath().resolve(name), () -> {
      synchronized (this) {
        submitterType = null;
        workBaseDirectory = null;
        xsubDirectory = null;
        pollingInterval = null;
        maximumNumberOfThreads = null;
        allocableMemorySize = null;
        maximumNumberOfJobs = null;
        parameters = null;
        xsubTemplate = null;
        reloadPropertyStore();
      }
    });
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Computer) {
      return getName().equals(((Computer) o).getName());
    }
    return false;
  }

  public String getName() {
    return name;
  }

  public static Computer getInstance(String name) {
    if (name != null && !name.equals("") && Files.exists(getBaseDirectoryPath().resolve(name))) {
      Computer computer = instanceCache.get(name);
      if (computer == null) {
        computer = new Computer(name);
      }
      return computer;
    }
    return null;
  }

  public static Computer find(String key) {
    return getInstance(key);
  }

  public static ArrayList<Computer> getList() {
    initializeWorkDirectory();

    ArrayList<Computer> list = new ArrayList<>();

    for (File file : getBaseDirectoryPath().toFile().listFiles()) {
      if (file.isDirectory()) {
        list.add(getInstance(file.getName()));
      }
    }

    return list;
  }

  public void initialize() {
    if (! Files.exists(getDirectoryPath())) {
      try {
        Files.createDirectories(getDirectoryPath());
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    initializeWorkDirectory();

    if (getState() == null) { setState(ComputerState.Unviable); }
    if (getXsubDirectory() == null) { setXsubDirectory(""); }
    if (getWorkBaseDirectory() == null) { setWorkBaseDirectory("/tmp/waffle"); }
    if (getMaximumNumberOfThreads() == null) { setMaximumNumberOfThreads(1.0); }
    if (getAllocableMemorySize() == null) { setAllocableMemorySize(1.0); }
    if (getPollingInterval() == null) { setPollingInterval(10); }
    if (getMaximumNumberOfJobs() == null) { setMaximumNumberOfJobs(1); }
  }

  public static ArrayList<Computer> getViableList() {
    ArrayList<Computer> list = new ArrayList<>();

    for (Computer computer : getList()) {
      if (computer.getState().equals(ComputerState.Viable)) {
        list.add(computer);
      }
    }

    return list;
  }

  public static Computer create(String name, String submitterClassName) {
    Data.initializeWorkDirectory();

    name = FileName.removeRestrictedCharacters(name);

    Computer computer = getInstance(name);
    if (computer == null) {
      computer = new Computer(name);
    }
    computer.setSubmitterType(submitterClassName);

    return computer;
  }

  public static ArrayList<String> getSubmitterTypeNameList() {
    ArrayList<String> list = new ArrayList<>();
    for (Class c : submitterTypeList) {
      list.add(c.getCanonicalName());
    }
    return list;
  }

  public void update() {
    try {
      AbstractSubmitter.checkWaffleServant(this, false);
      JSONObject jsonObject = AbstractSubmitter.getXsubTemplate(this, false);
      if (jsonObject != null) {
        setXsubTemplate(jsonObject);
        setParameters(getParameters());
        setState(ComputerState.Viable);
        setMessage("");
      }
    } catch (NotFoundXsubException e) {
      setMessage("bin/xsub is not found in " + ("".equals(getXsubDirectory()) ? "$PATH" : getXsubDirectory()));
      setState(ComputerState.XsubNotFound);
    } catch (RuntimeException | WaffleException e) {
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("java.io.FileNotFoundException: ")) {
          message = message.replaceFirst("java\\.io\\.FileNotFoundException: ", "");
          setState(ComputerState.KeyNotFound);
        } else if (message.startsWith("invalid privatekey: ")) {
          if (getParameters().keySet().contains(JobNumberLimitedSshSubmitter.KEY_IDENTITY_FILE)) {
            String keyPath = getParameters().getString(JobNumberLimitedSshSubmitter.KEY_IDENTITY_FILE);
            if (keyPath.indexOf('~') == 0) {
              keyPath = keyPath.replaceFirst("^~", System.getProperty("user.home"));
            }
            try {
              if (!"".equals(keyPath) && (new String(Files.readAllBytes(Paths.get(keyPath)))).indexOf("OPENSSH PRIVATE KEY") > 0) {
                message = keyPath + " is a OpenSSH private key type and WAFFLE does not supported the key type";
                setState(ComputerState.UnsupportedKey);
              }
            } catch (IOException ioException) {
              ErrorLogMessage.issue(ioException);
            }
          }
        } else {
          message = message.replaceFirst("Auth fail", "probably, invalid user or key");
          message = message.replaceFirst("USERAUTH fail", "probably, invalid key passphrase (identity_pass)");
          message = message.replaceFirst("java\\.net\\.UnknownHostException: (.*)", "$1 is unknown host");
          message = message.replaceFirst("java\\.net\\.ConnectException: Connection refused \\(Connection refused\\)", "Connection refused (could not connect to the SSH server)");
          setState(ComputerState.Unviable);
        }
        setMessage(message);
      }
    }
  }

  private void setMessage(String message) {
    setToProperty(KEY_MESSAGE, message);
  }

  public String getMessage() {
    return getStringFromProperty(KEY_MESSAGE, "");
  }

  private void setState(ComputerState state) {
    setToProperty(KEY_STATE, state.ordinal());
  }

  public ComputerState getState() {
    Integer state = getIntFromProperty(KEY_STATE, ComputerState.Unviable.ordinal());
    if (state == null) {
      return null;
    }
    return ComputerState.valueOf(state);
  }

  public boolean isLocal() {
    //return LOCAL_UUID.equals(id);
    return getName().equals(KEY_LOCAL);
  }

  public String getSubmitterType() {
    synchronized (this) {
      if (submitterType == null) {
        submitterType = getStringFromProperty(KEY_SUBMITTER, submitterTypeList.get(0).getCanonicalName());
      }
      return submitterType;
    }
  }

  public void setSubmitterType(String submitterClassName) {
    synchronized (this) {
      setToProperty(KEY_SUBMITTER, submitterClassName);
      submitterType = submitterClassName;
    }
  }

  public String getWorkBaseDirectory() {
    synchronized (this) {
      if (workBaseDirectory == null) {
        workBaseDirectory = getStringFromProperty(KEY_WORKBASE);
      }
      return workBaseDirectory;
    }
  }

  public void setWorkBaseDirectory(String workBaseDirectory) {
    synchronized (this) {
      setToProperty(KEY_WORKBASE, workBaseDirectory);
      this.workBaseDirectory = workBaseDirectory;
    }
  }

  public String getXsubDirectory() {
    synchronized (this) {
      if (xsubDirectory == null) {
        xsubDirectory = getStringFromProperty(KEY_XSUB);
      }
      return xsubDirectory;
    }
  }

  public void setXsubDirectory(String xsubDirectory) {
    synchronized (this) {
      setToProperty(KEY_XSUB, xsubDirectory);
      this.xsubDirectory = xsubDirectory;
    }
  }

  private SecretKeySpec getEncryptKey() {
    synchronized (this) {
      if (encryptKey == null) {
        String encryptKeyText = getStringFromProperty(KEY_ENCRYPT_KEY);
        if (encryptKeyText == null || encryptKeyText.length() != 16) {
          encryptKeyText = UUID.randomUUID().toString().replace("-", "").substring(16, 32);
          setToProperty(KEY_ENCRYPT_KEY, encryptKeyText);
        }
        encryptKey = new SecretKeySpec(encryptKeyText.getBytes(), "AES");
      }
      return encryptKey;
    }
  }

  public String encryptText(String text) {
    if (text != null) {
      try {
        Cipher encrypter = Cipher.getInstance(ENCRYPT_ALGORITHM);
        encrypter.init(Cipher.ENCRYPT_MODE, getEncryptKey(), IV_PARAMETER_SPEC);
        byte[] byteToken = encrypter.doFinal(text.getBytes());
        return new String(Base64.getEncoder().encode(byteToken));
      } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
      }
    }
    return "";
  }

  public String decryptText(String text) {
    if (text != null) {
      try {
        Cipher decrypter = Cipher.getInstance(ENCRYPT_ALGORITHM);
        decrypter.init(Cipher.DECRYPT_MODE, getEncryptKey(), IV_PARAMETER_SPEC);
        byte[] byteToken = Base64.getDecoder().decode(text);
        return new String(decrypter.doFinal(byteToken));
      } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
        e.printStackTrace();
      }
    }
    return "";
  }

  /*
  public String getOs() {
    if (os == null) {
      os = getStringFromDB(KEY_OS);
    }
    return os;
  }

  public String getDirectorySeparetor() {
    String directorySeparetor = "/";
    if (getOs().equals("U")) {
      directorySeparetor = "/";
    }
    return directorySeparetor;
  }
   */

  public Integer getPollingInterval() {
    synchronized (this) {
      if (pollingInterval == null) {
        pollingInterval = getIntFromProperty(KEY_POLLING);
      }
      return pollingInterval;
    }
  }

  public void setPollingInterval(Integer pollingInterval) {
    synchronized (this) {
      setToProperty(KEY_POLLING, pollingInterval);
      this.pollingInterval = pollingInterval;
    }
  }

  public Double getMaximumNumberOfThreads() {
    synchronized (this) {
      if (maximumNumberOfThreads == null) {
        maximumNumberOfThreads = getDoubleFromProperty(KEY_MAX_THREADS);
      }
      return maximumNumberOfThreads;
    }
  }

  public void setMaximumNumberOfThreads(Double maximumNumberOfThreads) {
    synchronized (this) {
      setToProperty(KEY_MAX_THREADS, maximumNumberOfThreads);
      this.maximumNumberOfThreads = maximumNumberOfThreads;
    }
  }

  public Double getAllocableMemorySize() {
    synchronized (this) {
      if (allocableMemorySize == null) {
        allocableMemorySize = getDoubleFromProperty(KEY_ALLOCABLE_MEMORY);
      }
      return allocableMemorySize;
    }
  }

  public void setAllocableMemorySize(Double allocableMemorySize) {
    synchronized (this) {
      setToProperty(KEY_ALLOCABLE_MEMORY, allocableMemorySize);
      this.allocableMemorySize = allocableMemorySize;
    }
  }

  public Integer getMaximumNumberOfJobs() {
    synchronized (this) {
      if (maximumNumberOfJobs == null) {
        maximumNumberOfJobs = getIntFromProperty(KEY_MAX_JOBS);
      }
      return maximumNumberOfJobs;
    }
  }

  public void setMaximumNumberOfJobs(Integer maximumNumberOfJobs) {
    synchronized (this) {
      setToProperty(KEY_MAX_JOBS, maximumNumberOfJobs);
      this.maximumNumberOfJobs = maximumNumberOfJobs;
    }
  }

  public JSONObject getXsubParameters() {
    JSONObject jsonObject = new JSONObject();
    for (String key : getXsubParametersTemplate().keySet()) {
      jsonObject.put(key, getParameter(key));
    }
    return jsonObject;
  }

  public JSONObject getParametersWithDefaultParameters() {
    JSONObject parameters = AbstractSubmitter.getParameters(this);
    JSONObject jsonObject = getParameters();
    for (String key : jsonObject.keySet()) {
      parameters.put(key, jsonObject.get(key));
    }
    return parameters;
  }

  public JSONObject getParametersWithDefaultParametersFiltered() {
    JSONObject parameters = AbstractSubmitter.getParameters(this);
    JSONObject jsonObject = getParameters();
    for (String key : jsonObject.keySet()) {
      if (! key.startsWith(".")) {
        parameters.put(key, jsonObject.get(key));
      }
    }
    return parameters;
  }

  public JSONObject getFilteredParameters() {
    JSONObject jsonObject = new JSONObject();
    JSONObject parameters = getParameters();
    for (String key : parameters.keySet()) {
      if (key.startsWith(".")) {
        jsonObject.put(key, parameters.get(key));
      }
    }
    return jsonObject;
  }

  public JSONObject getParameters() {
    synchronized (this) {
      if (parameters == null) {
        String json = getFileContents(KEY_PARAMETERS + Constants.EXT_JSON);
        if (json.equals("")) {
          json = "{}";
          createNewFile(KEY_PARAMETERS + Constants.EXT_JSON);
          updateFileContents(KEY_PARAMETERS + Constants.EXT_JSON, json);
        }
        parameters = getXsubParametersTemplate();
        try {
          JSONObject jsonObject = AbstractSubmitter.getInstance(PollingThread.Mode.Normal, this).getDefaultParameters(this);
          for (String key : jsonObject.toMap().keySet()) {
            parameters.put(key, jsonObject.getJSONObject(key));
          }
        } catch (Exception e) {
        }
        JSONObject jsonObject = new JSONObject(json);
        for (String key : jsonObject.keySet()) {
          parameters.put(key, jsonObject.get(key));
        }
      }
      return new JSONObject(parameters.toString());
    }
  }

  public Object getParameter(String key) {
    return getParameters().get(key);
  }

  public void setParameters(JSONObject jsonObject) {
    synchronized (this) {
      JSONObject parameters = getFilteredParameters();
      for (String key : jsonObject.keySet()) {
        parameters.put(key, jsonObject.get(key));
      }

      updateFileContents(KEY_PARAMETERS + Constants.EXT_JSON, parameters.toString(2));
      this.parameters = null;
    }
  }

  public void setParameters(String json) {
    try {
      setParameters(new JSONObject(json));
    } catch (Exception e) {
      WarnLogMessage.issue(e);
    }
  }

  public Object setParameter(String key, Object value) {
    synchronized (this) {
      JSONObject jsonObject = getParameters();
      jsonObject.put(key, value);
      setParameters(jsonObject);
      return value;
    }
  }

  public JSONObject getEnvironments() {
    synchronized (this) {
      return getJSONObjectFromProperty(KEY_ENVIRONMENTS, new JSONObject());
    }
  }

  public void setEnvironments(JSONObject jsonObject) {
    synchronized (this) {
      setToProperty(KEY_ENVIRONMENTS, jsonObject);
    }
  }

  public JSONObject getXsubTemplate() {
    synchronized (this) {
      if (xsubTemplate == null) {
        xsubTemplate = new JSONObject(getStringFromProperty(KEY_XSUB_TEMPLATE, "{}"));
      }
      return xsubTemplate;
    }
  }

  public void setXsubTemplate(JSONObject jsonObject) {
    synchronized (this) {
      this.xsubTemplate = jsonObject;
      setToProperty(KEY_XSUB_TEMPLATE, jsonObject.toString());
    }
  }

  public JSONObject getXsubParametersTemplate() {
    JSONObject jsonObject = new JSONObject();

    try {
      JSONObject object = getXsubTemplate().getJSONObject("parameters");
      for (String key : object.toMap().keySet()) {
        jsonObject.put(key, object.getJSONObject(key).get("default"));
      }
    } catch (Exception e) {
    }

    return jsonObject;
  }

  public static Path getBaseDirectoryPath() {
    return Data.getWaffleDirectoryPath().resolve(Constants.COMPUTER);
  }

  @Override
  public Path getDirectoryPath() {
    return getBaseDirectoryPath().resolve(name);
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

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(Constants.COMPUTER + Constants.EXT_JSON);
  }

  public static void initializeWorkDirectory() {
    Data.initializeWorkDirectory();
    if (! Files.exists(getBaseDirectoryPath().resolve(KEY_LOCAL))) {
      Computer computer = create(KEY_LOCAL, JobNumberLimitedLocalSubmitter.class.getCanonicalName());
      Path xsubPath = Constants.WORK_DIR.resolve("xsub");
      /*
      if (! Files.exists(xsubPath)) {
        ResourceFile.unzip("/xsub.zip", Constants.WORK_DIR);
        try {
          Process process = new ProcessBuilder("sh", xsubPath.resolve("bin").resolve("set_perm.sh").toString()).start();
          process.waitFor();
        } catch (Exception e) {
          ErrorLogMessage.issue(e);
        }
      }
       */
      computer.setXsubDirectory(xsubPath.toString());
      Path localWorkBaseDirectoryPath = Constants.WORK_DIR.resolve("local");
      try {
        Files.createDirectories(localWorkBaseDirectoryPath);
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
      computer.setWorkBaseDirectory(localWorkBaseDirectoryPath.toString());
      computer.update();
      InfoLogMessage.issue(computer, "was added automatically");
    }
  }
}
