package jp.tkms.waffle.sub.servant.processor;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.XsubFile;
import jp.tkms.waffle.sub.servant.message.request.CollectStatusMessage;
import jp.tkms.waffle.sub.servant.message.request.SubmitJobMessage;
import jp.tkms.waffle.sub.servant.message.response.JobExceptionMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateJobIdMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateStatusMessage;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.IOException;
import java.io.StringWriter;
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
        response.add(new UpdateStatusMessage(message, jsonObject.get(message.getJobId()).asObject().getString("status", null).toString().equals("finished")));
      }
    } catch (Exception e) {
      for (CollectStatusMessage message : messageList) {
        response.add(new JobExceptionMessage(message, e.getMessage() + "\n" + outputWriter.toString()));
      }
    }
    outputWriter.close();
    container.terminate();
  }
}
