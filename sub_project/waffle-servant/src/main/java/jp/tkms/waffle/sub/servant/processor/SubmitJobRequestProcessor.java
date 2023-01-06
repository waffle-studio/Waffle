package jp.tkms.waffle.sub.servant.processor;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import jp.tkms.waffle.sub.servant.Constants;
import jp.tkms.waffle.sub.servant.DirectoryHash;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.XsubFile;
import jp.tkms.waffle.sub.servant.message.request.SubmitJobMessage;
import jp.tkms.waffle.sub.servant.message.response.JobExceptionMessage;
import jp.tkms.waffle.sub.servant.message.response.RequestRepreparingMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateJobIdMessage;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SubmitJobRequestProcessor extends RequestProcessor<SubmitJobMessage> {
  public static final String XSUB_TYPE = "XSUB_TYPE";
  public static final String NONE = "None";
  public static final int MAX_REMOVING_RETRYING = 100;

  protected SubmitJobRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<SubmitJobMessage> messageList) throws ClassNotFoundException, IOException {
    ArrayList<Path> removingList = new ArrayList<>();
    Map environments = new HashMap(System.getenv());
    if (!environments.containsKey(XSUB_TYPE)) {
      environments.put(XSUB_TYPE, NONE);
    }

    messageList.stream().parallel().forEach(message -> {
      Path workingDirectory = baseDirectory.resolve(message.getWorkingDirectory()).toAbsolutePath().normalize();

      if (Files.exists(workingDirectory.resolve(Constants.XSUB_LOG_FILE))) { // If already executed, remove and request resubmit
        try {
          Files.deleteIfExists(workingDirectory.resolve(Constants.ALIVE));
        } catch (IOException e) {
          //NOP
        }
        removingList.add(workingDirectory);
        response.add(new RequestRepreparingMessage(message));
        return;
      }

      if (message.getExecutableDirectory() != null) {
        DirectoryHash executableDirectoryHash = new DirectoryHash(baseDirectory, message.getExecutableDirectory(), false);
        synchronized (this) {
          if (!executableDirectoryHash.hasHashFile()) {
            executableDirectoryHash.save();
          } else {
            if (executableDirectoryHash.update()) {
              System.out.println("!!!!! EXECUTABLE FILES HAS CHANGED !!!!!");
              //TODO: notify if hash changed
            }
          }
        }
      }

      StringWriter outputWriter = new StringWriter();
      StringWriter errorWriter = new StringWriter();
      try {
        DirectoryHash directoryHash = new DirectoryHash(baseDirectory, message.getWorkingDirectory());
        directoryHash.createEmptyHashFile();

        synchronized (this) {
          ScriptingContainer container = new ScriptingContainer(LocalContextScope.CONCURRENT, LocalVariableBehavior.TRANSIENT);
          container.setEnvironment(environments);
          container.setCurrentDirectory(workingDirectory.toString());
          container.setArgv(new String[]{"-p", message.getXsubParameter(), message.getCommand()});
          container.setOutput(outputWriter);
          container.setError(errorWriter);
          container.runScriptlet(PathType.ABSOLUTE, XsubFile.getXsubPath(baseDirectory).toString());
          container.clear();
          container.terminate();
          directoryHash.save();
          outputWriter.flush();
        }

        JsonObject jsonObject = Json.parse(outputWriter.toString()).asObject();
        //System.out.println(jsonObject.toString());
        response.add(new UpdateJobIdMessage(message, jsonObject.getString("job_id", null).toString(), workingDirectory));
      } catch (Exception e) {
        //e.printStackTrace();
        response.add(new UpdateJobIdMessage(message, "FAILED", workingDirectory));

        errorWriter.flush();
        outputWriter.flush();
        String errorMessage = errorWriter.toString();
        if ("".equals(errorMessage)) {
          response.add(new JobExceptionMessage(message, e.getMessage() + "\n" + outputWriter.toString()));
        } else {
          response.add(new JobExceptionMessage(message, errorWriter.toString()));
        }
      }
      try {
        outputWriter.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    if (!removingList.isEmpty()) {
      try {
        TimeUnit.SECONDS.sleep(Constants.WATCHDOG_INTERVAL);
      } catch (InterruptedException e) {
        //NOP
      }
      removingList.stream().parallel().forEach(path -> {
        for (int count = 0; count < MAX_REMOVING_RETRYING; count += 1) {
          try {
            Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
          } catch (IOException e) {
            continue;
          }
          break;
        }
      });
    }
  }
}
