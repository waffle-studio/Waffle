package jp.tkms.waffle.sub.servant.processor;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import jp.tkms.waffle.sub.servant.*;
import jp.tkms.waffle.sub.servant.message.request.CollectStatusMessage;
import jp.tkms.waffle.sub.servant.message.request.SubmitJobMessage;
import jp.tkms.waffle.sub.servant.message.response.*;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CollectStatusRequestProcessor extends RequestProcessor<CollectStatusMessage> {
  public static final String XSUB_TYPE = "XSUB_TYPE";
  public static final String NONE = "None";

  protected CollectStatusRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<CollectStatusMessage> messageList) throws ClassNotFoundException, IOException {
    Map environments = new HashMap(System.getenv());
    if (!environments.containsKey(XSUB_TYPE)) {
      environments.put(XSUB_TYPE, NONE);
    }

    ScriptingContainer container = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.PERSISTENT);
    ArrayList<String> argumentList = new ArrayList<>();
    argumentList.add("-m");
    HashSet<String> jobIdSet = new HashSet<>();
    for (CollectStatusMessage message : messageList) {
      jobIdSet.add(message.getJobId());
    }
    argumentList.addAll(jobIdSet);

    StringWriter outputWriter = new StringWriter();
    container.setEnvironment(environments);
    container.setArgv(argumentList.toArray(new String[argumentList.size()]));
    container.setOutput(outputWriter);
    container.runScriptlet(PathType.ABSOLUTE, XsubFile.getXstatPath(baseDirectory).toString());
    outputWriter.flush();

    try {
      JsonObject jsonObject = Json.parse(outputWriter.toString()).asObject();
      for (CollectStatusMessage message : messageList) {
        try {
          new EventReader(baseDirectory, message.getWorkingDirectory().resolve(Constants.EVENT_FILE)).process((name, value) -> {
            response.add(new UpdateResultMessage(message, name, value));
          });
          if (jsonObject.get(message.getJobId()).asObject().getString("status", null).toString().equals("finished")) {
            int exitStatus = -2;
            try {
              exitStatus = Integer.parseInt(new String(Files.readAllBytes(baseDirectory.resolve(message.getWorkingDirectory()).resolve(Constants.EXIT_STATUS_FILE))));
            } catch (Exception | Error e) {
              exitStatus = -1;
            }
            if ((exitStatus >= 0 && new DirectoryHash(baseDirectory, message.getWorkingDirectory()).isMatchToHashFile()) || exitStatus < 0) {
              //(new DirectoryHash(baseDirectory, message.getWorkingDirectory())).waitToMatch(Constants.DIRECTORY_SYNCHRONIZATION_TIMEOUT);
              response.add(new UpdateStatusMessage(message, exitStatus));
              response.add(message.getWorkingDirectory().resolve(Constants.STDOUT_FILE));
              response.add(message.getWorkingDirectory().resolve(Constants.STDERR_FILE));
            } else {
              response.add(new UpdateStatusMessage(message));
            }
          } else {
            response.add(new UpdateStatusMessage(message));
          }
        } catch (Exception e) {
          response.add(new JobExceptionMessage(message, e.getMessage() + "\n" + outputWriter.toString()));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    outputWriter.close();
    container.terminate();
  }
}