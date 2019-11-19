package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RubyConductor extends CycleConductor {
  @Override
  protected void preProcess(ConductorRun run) {
    ScriptingContainer container = new ScriptingContainer();
    try {
      container.runScriptlet(getInitScript());
      container.runScriptlet(PathType.ABSOLUTE, run.getConductor().getScriptPath().toString());
      container.runScriptlet("$default_project = Java::jp.tkms.waffle.data.Project.getInstance(\"" + run.getProject().getId() + "\")");
      container.runScriptlet("$default_conductor = Java::jp.tkms.waffle.data.Conductor.getInstance($default_project, \"" + run.getConductor().getId() + "\")");
      container.runScriptlet("$registry = Registry.new()");
      container.runScriptlet("preProcess(Trial.new($default_project.id,\"" + run.getTrial().getId() + "\"))");
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('" + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
  }

  @Override
  protected void cycleProcess(ConductorRun run) {
    ScriptingContainer container = new ScriptingContainer();
    try {
      container.runScriptlet(PathType.ABSOLUTE, run.getConductor().getScriptPath().toString());
      container.runScriptlet("cycleProcess(Trial.new(\"" + run.getProject().getId() + "\",\"" + run.getTrial().getId() + "\"))");
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('" + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
  }

  @Override
  protected void postProcess(ConductorRun run) {
    ScriptingContainer container = new ScriptingContainer();
    try {
      container.runScriptlet(PathType.ABSOLUTE, run.getConductor().getScriptPath().toString());
      container.runScriptlet("postProcess(Trial.new(\"" + run.getProject().getId() + "\",\"" + run.getTrial().getId() + "\"))");
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('" + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
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
        "def preProcess(trial)\n" +
          "end\n" +
          "\n" +
          "def cycleProcess(trial)\n" +
          "end\n" +
          "\n" +
          "def postProcess(trial)\n" +
          "end\n");
      filewriter.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String getInitScript() {
    String result = "";
    InputStream in = getClass().getResourceAsStream("/ruby_conductor_init.rb");
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    String data;
    try {
      while (true) {
        if (!((data = reader.readLine()) != null)) break;
        result += data + '\n';
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }
}
