package jp.tkms.waffle.sub.servant.processor;

import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.XsubFile;
import jp.tkms.waffle.sub.servant.message.request.CancelJobMessage;
import jp.tkms.waffle.sub.servant.message.request.SendXsubTemplateMessage;
import jp.tkms.waffle.sub.servant.message.response.JobCanceledMessage;
import jp.tkms.waffle.sub.servant.message.response.XsubTemplateMessage;
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

public class SendXsubTemplateRequestProcessor extends RequestProcessor<SendXsubTemplateMessage> {
  public static final String XSUB_TYPE = "XSUB_TYPE";
  public static final String NONE = "None";

  protected SendXsubTemplateRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<SendXsubTemplateMessage> messageList) throws ClassNotFoundException, IOException {
    Map environments = new HashMap(System.getenv());
    if (!environments.containsKey(XSUB_TYPE)) {
      environments.put(XSUB_TYPE, NONE);
    }

    StringWriter outputWriter = new StringWriter();
    ScriptingContainer container = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.PERSISTENT);
    container.setEnvironment(environments);
    container.setArgv(new String[]{"-t"});
    container.setOutput(outputWriter);
    container.runScriptlet("require 'jruby'");
    container.runScriptlet(PathType.ABSOLUTE, XsubFile.getXsubPath(baseDirectory).toString());
    outputWriter.flush();
    response.add(new XsubTemplateMessage(outputWriter.toString()));
    outputWriter.close();
    container.terminate();
  }
}
