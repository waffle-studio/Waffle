package jp.tkms.waffle.sub.servant.processor;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.XsubFile;
import jp.tkms.waffle.sub.servant.message.request.SubmitJobMessage;
import jp.tkms.waffle.sub.servant.message.response.JobExceptionMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateJobIdMessage;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SubmitJobRequestProcessor extends RequestProcessor<SubmitJobMessage> {
  public static final String XSUB_TYPE = "XSUB_TYPE";
  public static final String NONE = "None";

  protected SubmitJobRequestProcessor(Envelope request, Envelope response) {
    super(request, response);
  }

  @Override
  protected void process(Path baseDirectory) throws ClassNotFoundException, IOException {
    Map environments = new HashMap(System.getenv());
    if (!environments.containsKey(XSUB_TYPE)) {
      environments.put(XSUB_TYPE, NONE);
    }

    ScriptingContainer container = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.PERSISTENT);
    for (SubmitJobMessage message : getMessageList()) {
      StringWriter outputWriter = new StringWriter();
      container.setEnvironment(environments);
      container.setCurrentDirectory(baseDirectory.resolve(message.getWorkingDirectory()).toString());
      container.setArgv(new String[]{"-p", message.getXsubParameter(), message.getCommand()});
      container.setOutput(outputWriter);
      container.runScriptlet(PathType.ABSOLUTE, XsubFile.getXsubPath(baseDirectory).toString());
      container.clear();
      outputWriter.flush();
      try {
        JsonObject jsonObject = Json.parse(outputWriter.toString()).asObject();
        //System.out.println(jsonObject.toString());
        response.add(new UpdateJobIdMessage(message, jsonObject.getString("job_id", null).toString()));
      } catch (Exception e) {
        //e.printStackTrace();
        response.add(new JobExceptionMessage(message, e.getMessage() + "\n" + outputWriter.toString()));
      }
    }
    container.terminate();
  }
}
