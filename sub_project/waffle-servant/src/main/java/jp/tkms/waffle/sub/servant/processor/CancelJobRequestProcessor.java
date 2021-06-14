package jp.tkms.waffle.sub.servant.processor;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import jp.tkms.waffle.sub.servant.Constants;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.XsubFile;
import jp.tkms.waffle.sub.servant.message.request.CancelJobMessage;
import jp.tkms.waffle.sub.servant.message.request.SubmitJobMessage;
import jp.tkms.waffle.sub.servant.message.response.JobCanceledMessage;
import jp.tkms.waffle.sub.servant.message.response.JobExceptionMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateJobIdMessage;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CancelJobRequestProcessor extends RequestProcessor<CancelJobMessage> {
  public static final String XSUB_TYPE = "XSUB_TYPE";
  public static final String NONE = "None";

  protected CancelJobRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<CancelJobMessage> messageList) throws ClassNotFoundException, IOException {
    Map environments = new HashMap(System.getenv());
    if (!environments.containsKey(XSUB_TYPE)) {
      environments.put(XSUB_TYPE, NONE);
    }

    for (CancelJobMessage message : messageList) {
      ScriptingContainer container = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.PERSISTENT);
      container.setEnvironment(environments);
      container.setArgv(new String[]{message.getJobId()});
      try {
        container.runScriptlet(PathType.ABSOLUTE, XsubFile.getXdelPath(baseDirectory).toString());
      } catch (RuntimeException e) {
        e.printStackTrace();
      }
      container.clear();
      container.terminate();
      response.add(new JobCanceledMessage(message));
      response.add(message.getWorkingDirectory().resolve(Constants.STDOUT_FILE));
      response.add(message.getWorkingDirectory().resolve(Constants.STDERR_FILE));
    }
  }
}