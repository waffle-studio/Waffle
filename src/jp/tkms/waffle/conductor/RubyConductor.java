package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RubyConductor extends CycleConductor {
  @Override
  protected void preProcess(ConductorRun run) {
    BrowserMessage.addMessage("toastr.warning('OKOK');");
    BrowserMessage.addMessage("alert('OKOK');");
    Ruby ruby = Ruby.newInstance();
    ScriptingContainer container = new ScriptingContainer();
    try {
      container.runScriptlet(PathType.ABSOLUTE, run.getConductor().getScriptPath().toString());
      container.runScriptlet("preProces()");
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.warning('" + e.getMessage().replaceAll("'","\"") + "');");
    }
  }

  @Override
  protected void cycleProcess(ConductorRun run) {

  }

  @Override
  protected void postProcess(ConductorRun run) {

  }

  @Override
  public String defaultScriptName() {
    return "main.rb";
  }

  @Override
  public void prepareConductor(Conductor conductor) {
    try {
      FileWriter filewriter = new FileWriter(conductor.getScriptPath().toFile());

      filewriter.write(
        "def preProcess()\n" +
        "end\n" +
        "\n" +
        "def cycleProcess()\n" +
        "end\n" +
        "\n" +
        "def postProcess()\n" +
        "end\n");
      filewriter.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
