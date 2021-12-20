package jp.tkms.waffle.sub.servant.processor;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import jp.tkms.waffle.sub.servant.DirectoryHash;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SubmitJobRequestProcessor extends RequestProcessor<SubmitJobMessage> {
  public static final String XSUB_TYPE = "XSUB_TYPE";
  public static final String NONE = "None";

  protected SubmitJobRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<SubmitJobMessage> messageList) throws ClassNotFoundException, IOException {
    Map environments = new HashMap(System.getenv());
    if (!environments.containsKey(XSUB_TYPE)) {
      environments.put(XSUB_TYPE, NONE);
    }

    for (SubmitJobMessage message : messageList) {
      if (message.getExecutableDirectory() != null) {
        DirectoryHash executableDirectoryHash = new DirectoryHash(baseDirectory, message.getExecutableDirectory(), false);
        if (!executableDirectoryHash.hasHashFile()) {
          executableDirectoryHash.save();
        } else {
          if (executableDirectoryHash.update()) {
            System.out.println("!!!!! EXECUTABLE FILES HAS CHANGED !!!!!");
            //TODO: notify if hash changed
          }
        }
      }

      Path workingDirectory = baseDirectory.resolve(message.getWorkingDirectory()).toAbsolutePath().normalize();

      StringWriter outputWriter = new StringWriter();
      StringWriter errorWriter = new StringWriter();
      try {
        ScriptingContainer container = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.PERSISTENT);
        container.setEnvironment(environments);
        container.setCurrentDirectory(workingDirectory.toString());
        container.setArgv(new String[]{"-p", message.getXsubParameter(), message.getCommand()});
        container.setOutput(outputWriter);
        container.setError(errorWriter);
        container.runScriptlet(PathType.ABSOLUTE, XsubFile.getXsubPath(baseDirectory).toString());
        container.clear();
        container.terminate();
        (new DirectoryHash(baseDirectory, message.getWorkingDirectory())).save();
        outputWriter.flush();

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
      outputWriter.close();
    }
  }
}
